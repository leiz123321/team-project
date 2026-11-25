package data_access;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The transport that really uses the network, through the HTTP client of the
 * Java library.
 *
 * <p>Two limits keep a slow service from holding up the game. Connecting may
 * take at most {@value #MAX_CONNECT_SECONDS} seconds, or the whole time
 * allowed if that is shorter. The request as a whole, from sending it to
 * reading the last byte of the reply, may take at most the time given to the
 * constructor; after that the request is given up and reported as timed out.
 *
 * <p>Redirects are not followed, so the request headers, which carry the API
 * key, only ever go to the address that was asked for.
 */
final class LlmHttpTransport implements LlmTransport {
    private static final long MAX_CONNECT_SECONDS = 5;

    private final Duration timeout;
    private HttpClient client;

    /**
     * Creates the transport. Nothing touches the network until the first
     * request.
     *
     * @param timeout the longest a whole request may take
     * @throws IllegalArgumentException if timeout is null, zero, or negative
     */
    LlmHttpTransport(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
    }

    @Override
    public LlmHttpReply post(String url, Map<String, String> headers, String body) throws IOException {
        HttpRequest request = buildRequest(url, headers, body);
        CompletableFuture<HttpResponse<String>> pending = client().sendAsync(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            HttpResponse<String> response = pending.get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
            return new LlmHttpReply(response.statusCode(), response.body());
        }
        catch (TimeoutException exception) {
            pending.cancel(true);
            throw new HttpTimeoutException("no complete reply within " + this.timeout.toMillis() + " ms");
        }
        catch (InterruptedException exception) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("interrupted while waiting for the reply");
        }
        catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("the request failed", cause);
        }
    }

    private HttpRequest buildRequest(String url, Map<String, String> headers, String body) throws IOException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(this.timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
            return builder.build();
        }
        catch (IllegalArgumentException exception) {
            // The message of e can quote a header value, and so the API key; it is not passed on.
            throw new IOException("the request could not be built");
        }
    }

    private synchronized HttpClient client() {
        if (this.client == null) {
            Duration connectTimeout = Duration.ofSeconds(MAX_CONNECT_SECONDS);
            if (this.timeout.compareTo(connectTimeout) < 0) {
                connectTimeout = this.timeout;
            }
            this.client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(connectTimeout)
                    .build();
        }
        return this.client;
    }
}
