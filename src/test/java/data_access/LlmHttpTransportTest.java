package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Action;
import entity.ActionType;
import entity.BotProfile;
import entity.Card;
import entity.DecisionContext;
import entity.Rank;
import entity.Suit;
import use_case.opponent_action.OpponentUnavailableException;

/**
 * Tests the transport that really uses HTTP. The other end is a small server
 * started by the test on this machine, so nothing leaves it and the LLM
 * service is never called.
 */
class LlmHttpTransportTest {
    private static final String KEY = "sk-ant-test-0123456789-ABCDEFG";
    private static final Duration PATIENT = Duration.ofSeconds(20);
    private static final Duration IMPATIENT = Duration.ofMillis(400);
    private static final long STALL_SECONDS = 30;
    private static final long MUCH_LESS_THAN_THE_STALL_MILLIS = 10_000;

    private final CountDownLatch release = new CountDownLatch(1);
    private HttpServer server;
    private ExecutorService handlers;

    @BeforeEach
    void startServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.handlers = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "llm-test-server");
            thread.setDaemon(true);
            return thread;
        });
        this.server.setExecutor(this.handlers);
        this.server.start();
    }

    @AfterEach
    void stopServer() {
        this.release.countDown();
        this.server.stop(0);
        this.handlers.shutdownNow();
    }

    @Test
    void theBodyAndTheHeadersArriveAndTheReplyComesBack() throws IOException {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();
        AtomicReference<String> receivedKey = new AtomicReference<>();
        AtomicReference<String> receivedType = new AtomicReference<>();
        this.server.createContext("/v1/messages", exchange -> {
            method.set(exchange.getRequestMethod());
            receivedKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            receivedType.set(exchange.getRequestHeaders().getFirst("content-type"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"cards\":\"K♥ 10♦\"}");
        });

        String sent = "{\"hole\":\"A♠ Q♣\",\"note\":\"caf\u00e9\"}";
        LlmHttpReply reply = new LlmHttpTransport(PATIENT).post(url("/v1/messages"),
                Map.of("x-api-key", KEY, "content-type", "application/json"), sent);

        assertEquals(200, reply.getStatusCode());
        assertEquals("{\"cards\":\"K♥ 10♦\"}", reply.getBody());
        assertEquals("POST", method.get());
        assertEquals(sent, receivedBody.get(), "the body must travel as UTF-8");
        assertEquals(KEY, receivedKey.get());
        assertEquals("application/json", receivedType.get());
    }

    @Test
    void oneTransportCanSendManyRequests() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        this.server.createContext("/count", exchange -> {
            exchange.getRequestBody().readAllBytes();
            respond(exchange, 200, String.valueOf(requests.incrementAndGet()));
        });
        LlmHttpTransport transport = new LlmHttpTransport(PATIENT);
        assertEquals("1", transport.post(url("/count"), Map.of(), "{}").getBody());
        assertEquals("2", transport.post(url("/count"), Map.of(), "{}").getBody());
        assertEquals("3", transport.post(url("/count"), Map.of(), "{}").getBody());
    }

    @Test
    void anErrorStatusIsAReplyNotAFailure() throws IOException {
        this.server.createContext("/v1/messages", exchange -> {
            exchange.getRequestBody().readAllBytes();
            respond(exchange, 429, "{\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\"}}");
        });
        LlmHttpReply reply = new LlmHttpTransport(PATIENT).post(url("/v1/messages"), Map.of(), "{}");
        assertEquals(429, reply.getStatusCode());
        assertTrue(reply.getBody().contains("rate_limit_error"), reply.getBody());
    }

    @Test
    void anEmptyReplyHasAnEmptyBody() throws IOException {
        this.server.createContext("/empty", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        LlmHttpReply reply = new LlmHttpTransport(PATIENT).post(url("/empty"), Map.of(), "{}");
        assertEquals(204, reply.getStatusCode());
        assertEquals("", reply.getBody());
    }

    @Test
    void aRedirectIsNotFollowedSoTheKeyGoesNowhereElse() throws IOException {
        AtomicInteger elsewhere = new AtomicInteger();
        this.server.createContext("/elsewhere", exchange -> {
            elsewhere.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            respond(exchange, 200, "{}");
        });
        this.server.createContext("/v1/messages", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Location", url("/elsewhere"));
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
        });
        LlmHttpReply reply = new LlmHttpTransport(PATIENT).post(url("/v1/messages"),
                Map.of("x-api-key", KEY), "{}");
        assertEquals(307, reply.getStatusCode());
        assertEquals(0, elsewhere.get());
    }

    @Test
    void aReplyThatDoesNotComeTimesOut() {
        this.server.createContext("/v1/messages", exchange -> {
            exchange.getRequestBody().readAllBytes();
            awaitRelease();
            exchange.close();
        });
        long start = System.nanoTime();
        assertThrows(HttpTimeoutException.class,
                () -> new LlmHttpTransport(IMPATIENT).post(url("/v1/messages"), Map.of(), "{}"));
        assertTrue(millisSince(start) < MUCH_LESS_THAN_THE_STALL_MILLIS, "waited " + millisSince(start) + " ms");
    }

    @Test
    void aReplyThatStopsHalfwayTimesOutToo() {
        // The headers and the start of the body arrive at once; the rest never does. A limit on the wait for
        // the headers alone would not notice, so this is what shows that the whole request is limited.
        this.server.createContext("/v1/messages", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, 0);
            OutputStream body = exchange.getResponseBody();
            body.write("{\"content\":[".getBytes(StandardCharsets.UTF_8));
            body.flush();
            awaitRelease();
            exchange.close();
        });
        long start = System.nanoTime();
        assertThrows(HttpTimeoutException.class,
                () -> new LlmHttpTransport(IMPATIENT).post(url("/v1/messages"), Map.of(), "{}"));
        assertTrue(millisSince(start) < MUCH_LESS_THAN_THE_STALL_MILLIS, "waited " + millisSince(start) + " ms");
    }

    @Test
    void aPortNobodyListensOnIsAFailureButNotATimeout() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        String closed = "http://127.0.0.1:" + port + "/v1/messages";
        IOException failure = assertThrows(IOException.class,
                () -> new LlmHttpTransport(PATIENT).post(closed, Map.of(), "{}"));
        assertFalse(failure instanceof HttpTimeoutException, failure.toString());
    }

    @Test
    void anAddressOrHeaderThatCannotBeUsedFailsWithoutRepeatingTheKey() {
        LlmHttpTransport transport = new LlmHttpTransport(PATIENT);
        Map<String, String> headers = Map.of("x-api-key", KEY);
        List<IOException> failures = List.of(
                assertThrows(IOException.class, () -> transport.post("not an address", headers, "{}")),
                assertThrows(IOException.class, () -> transport.post("ftp://127.0.0.1/v1/messages", headers, "{}")),
                assertThrows(IOException.class, () -> transport.post("/v1/messages", headers, "{}")),
                assertThrows(IOException.class, () -> transport.post(url("/v1/messages"),
                        Map.of("x-api-key", KEY + "\nx-other: injected"), "{}")),
                assertThrows(IOException.class, () -> transport.post(url("/v1/messages"),
                        Map.of("bad header name", KEY), "{}")));
        for (IOException failure : failures) {
            assertFalse(String.valueOf(failure.getMessage()).contains(KEY), failure.toString());
            assertNull(failure.getCause(), "the failure underneath may quote the header, so it is not kept");
        }
    }

    @Test
    void anInterruptedWaitEndsAtOnceAndStaysInterrupted() {
        this.server.createContext("/v1/messages", exchange -> {
            exchange.getRequestBody().readAllBytes();
            awaitRelease();
            exchange.close();
        });
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedIOException.class,
                    () -> new LlmHttpTransport(PATIENT).post(url("/v1/messages"), Map.of(), "{}"));
            assertTrue(Thread.currentThread().isInterrupted(), "the interruption must not be swallowed");
        }
        finally {
            // Clears the flag so that it does not leak into the next test.
            Thread.interrupted();
        }
    }

    @Test
    void theTimeAllowedMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new LlmHttpTransport(null));
        assertThrows(IllegalArgumentException.class, () -> new LlmHttpTransport(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new LlmHttpTransport(Duration.ofSeconds(-1)));
    }

    @Test
    void theOpponentDecidesOverRealHttp() throws IOException, OpponentUnavailableException {
        AtomicReference<String> receivedKey = new AtomicReference<>();
        AtomicReference<String> receivedVersion = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();
        this.server.createContext("/v1/messages", exchange -> {
            receivedKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            receivedVersion.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            JSONObject text = new JSONObject().put("type", "text")
                    .put("text", "{\"action\":\"RAISE\",\"amount\":200,\"reason\":\"top pair, good kicker\"}");
            JSONObject thinking = new JSONObject().put("type", "thinking").put("thinking", "").put("signature", "s");
            respond(exchange, 200, new JSONObject()
                    .put("type", "message")
                    .put("role", "assistant")
                    .put("content", new JSONArray().put(thinking).put(text))
                    .put("stop_reason", "end_turn")
                    .toString());
        });
        LlmSettings settings = new LlmSettings(KEY, null, url(""), 20);
        DecisionContext context = new DecisionContext(
                List.of(new Card(Rank.ACE, Suit.SPADE), new Card(Rank.KING, Suit.DIAMOND)),
                List.of(new Card(Rank.TEN, Suit.HEART), new Card(Rank.JACK, Suit.CLUB), new Card(Rank.ACE, Suit.CLUB)),
                120, 40, List.of(ActionType.FOLD, ActionType.CALL, ActionType.RAISE), 80, 990, 0.81);

        LlmOpponentDataAccessObject opponent = new LlmOpponentDataAccessObject(settings,
                new LlmHttpTransport(settings.getTimeout()));
        Action action = opponent.decide(context, new BotProfile("Aggressive", 0.8));

        assertEquals(Action.raise(200), action);
        assertEquals(KEY, receivedKey.get());
        assertEquals("2026-10-21", receivedVersion.get());
        JSONObject sent = new JSONObject(receivedBody.get());
        assertEquals("claude-haiku-5-5", sent.getString("model"));
        String turn = sent.getJSONArray("messages").getJSONObject(0).getString("content");
        assertTrue(turn.contains("Your hole cards: A♠ K♦"), turn);
        assertTrue(turn.contains("Community cards: 10♥ J♣ A♣ (flop)"), turn);
    }

    @Test
    void theOpponentReportsAServerThatIsTooSlow() throws IOException {
        this.server.createContext("/v1/messages", exchange -> {
            exchange.getRequestBody().readAllBytes();
            awaitRelease();
            exchange.close();
        });
        LlmSettings settings = new LlmSettings(KEY, null, url(""), 1);
        DecisionContext context = new DecisionContext(
                List.of(new Card(Rank.ACE, Suit.SPADE), new Card(Rank.KING, Suit.DIAMOND)), List.of(),
                30, 10, List.of(ActionType.FOLD, ActionType.CALL, ActionType.RAISE), 40, 1000, 0.65);

        LlmOpponentDataAccessObject opponent = new LlmOpponentDataAccessObject(settings,
                new LlmHttpTransport(settings.getTimeout()));
        long start = System.nanoTime();
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent.decide(context, new BotProfile("Tight", 0.2)));
        assertTrue(failure.getMessage().contains("within 1 s"), failure.getMessage());
        assertTrue(millisSince(start) < MUCH_LESS_THAN_THE_STALL_MILLIS, "waited " + millisSince(start) + " ms");
    }

    private String url(String path) {
        return "http://127.0.0.1:" + this.server.getAddress().getPort() + path;
    }

    private void awaitRelease() {
        try {
            this.release.await(STALL_SECONDS, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static long millisSince(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
