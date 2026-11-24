package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import entity.Action;
import entity.ActionType;
import entity.BotProfile;
import entity.Card;
import entity.DecisionContext;
import entity.Game;
import entity.Match;
import entity.Rank;
import entity.Suit;
import use_case.opponent_action.OpponentUnavailableException;

/**
 * Tests the LLM opponent with a scripted transport in place of the network:
 * no test here sends anything anywhere.
 */
class LlmOpponentDataAccessObjectTest {
    private static final String KEY = "sk-ant-test-0123456789-ABCDEFG";
    private static final BotProfile PROFILE = new BotProfile("Balanced", 0.5);
    private static final int OK = 200;
    private static final long SECOND_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final long MINUTE_NANOS = TimeUnit.MINUTES.toNanos(1);

    // ---- the answer becomes an action ----------------------------------

    @Test
    void foldBecomesFold() throws OpponentUnavailableException {
        assertEquals(Action.fold(), decide(facingBet(), answer("FOLD", 0)));
    }

    @Test
    void checkBecomesCheck() throws OpponentUnavailableException {
        assertEquals(Action.check(), decide(nobodyHasBet(), answer("CHECK", 0)));
    }

    @Test
    void callBecomesCall() throws OpponentUnavailableException {
        assertEquals(Action.call(), decide(facingBet(), answer("CALL", 0)));
    }

    @Test
    void betKeepsItsAmount() throws OpponentUnavailableException {
        assertEquals(Action.bet(60), decide(nobodyHasBet(), answer("BET", 60)));
    }

    @Test
    void raiseKeepsItsAmount() throws OpponentUnavailableException {
        assertEquals(Action.raise(200), decide(facingBet(), answer("RAISE", 200)));
    }

    @Test
    void theSmallestAndTheLargestTotalAreKeptAsTheyAre() throws OpponentUnavailableException {
        assertEquals(Action.raise(80), decide(facingBet(), answer("RAISE", 80)));
        assertEquals(Action.raise(990), decide(facingBet(), answer("RAISE", 990)));
    }

    @Test
    void anAmountGivenWithFoldCheckOrCallIsIgnored() throws OpponentUnavailableException {
        assertEquals(Action.call(), decide(facingBet(), answer("CALL", 40)));
        assertEquals(Action.fold(), decide(facingBet(), answer("FOLD", -3)));
        assertEquals(Action.check(), decide(nobodyHasBet(), "{\"action\":\"CHECK\"}"));
        assertEquals(Action.call(), decide(facingBet(), "{\"action\":\"CALL\",\"amount\":\"none\"}"));
    }

    @Test
    void entriesTheFormDoesNotHaveAreIgnored() throws OpponentUnavailableException {
        String text = "{\"reason\":\"strong hand\",\"confidence\":0.9,\"amount\":150,\"action\":\"RAISE\"}";
        assertEquals(Action.raise(150), decide(facingBet(), text));
    }

    // ---- an amount outside the legal range ------------------------------

    @ParameterizedTest
    @CsvSource({
        "79, 80", "1, 80", "0, 80", "-250, 80", "-99999999999999999999, 80",
        "991, 990", "5000, 990", "2147483648, 990", "99999999999999999999, 990", "1e30, 990", "1e999999999, 990"
    })
    void aRaiseOutsideTheLegalRangeIsMovedIntoIt(String asked, int expected) throws OpponentUnavailableException {
        String text = "{\"action\":\"RAISE\",\"amount\":" + asked + ",\"reason\":\"test\"}";
        assertEquals(Action.raise(expected), decide(facingBet(), text));
    }

    @Test
    void aBetOutsideTheLegalRangeIsMovedIntoIt() throws OpponentUnavailableException {
        assertEquals(Action.bet(10), decide(nobodyHasBet(), answer("BET", 3)));
        assertEquals(Action.bet(995), decide(nobodyHasBet(), answer("BET", 4000)));
    }

    @Test
    void aWholeNumberWrittenWithAFractionOrAnExponentIsAccepted() throws OpponentUnavailableException {
        assertEquals(Action.raise(120), decide(facingBet(), "{\"action\":\"RAISE\",\"amount\":120.0}"));
        assertEquals(Action.raise(300), decide(facingBet(), "{\"action\":\"RAISE\",\"amount\":3e2}"));
    }

    @Test
    void anAmountIsNotMovedWhenTheActionTypeIsNotLegal() {
        // A bet of 60 would fit the range, but only a raise is legal: nothing is repaired.
        assertUnavailable(facingBet(), answer("BET", 60));
    }

    @Test
    void aRangeThatHoldsNoPositiveTotalGivesNoAction() {
        DecisionContext broken = new DecisionContext(cards("A♠", "K♦"), List.of(), 20, 0,
                List.of(ActionType.FOLD, ActionType.CHECK, ActionType.BET), 0, 0, 0.5);
        assertUnavailable(broken, answer("BET", 50));
    }

    // ---- white space and code fences ------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
        "```json\n{\"action\":\"RAISE\",\"amount\":200,\"reason\":\"value\"}\n```",
        "```\n{\"action\":\"RAISE\",\"amount\":200,\"reason\":\"value\"}\n```",
        "```JSON\r\n{\"action\":\"RAISE\",\"amount\":200,\"reason\":\"value\"}\r\n```\n",
        "```json{\"action\":\"RAISE\",\"amount\":200,\"reason\":\"value\"}```",
        "\n\n   {\"action\":\"RAISE\",\"amount\":200,\"reason\":\"value\"}  \n",
        "  ```json\n\n  { \"action\" : \"RAISE\" ,\n \"amount\" : 200 , \"reason\" : \"value\" }\n\n```  ",
        "{\"action\":\"RAISE\",\"amount\":200,\"reason\":\"a ``` inside the reason is fine\"}"
    })
    void whiteSpaceAndACodeFenceAroundTheAnswerAreAllowed(String text) throws OpponentUnavailableException {
        assertEquals(Action.raise(200), decide(facingBet(), text));
    }

    // ---- answers that cannot be used ------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
        "I think I will call here.",
        "CALL",
        "   ",
        "[]",
        "[{\"action\":\"CALL\",\"amount\":0}]",
        "\"CALL\"",
        "null",
        "{}",
        "{\"amount\":0,\"reason\":\"no action\"}",
        "{\"action\":null,\"amount\":0}",
        "{\"action\":3,\"amount\":0}",
        "{\"action\":[\"CALL\"],\"amount\":0}",
        "{\"action\":\"ALL_IN\",\"amount\":0}",
        "{\"action\":\"call\",\"amount\":0}",
        "{\"action\":\" CALL\",\"amount\":0}",
        "{\"action\":\"FOLD|CHECK|CALL|BET|RAISE\",\"amount\":0}",
        "Sure! {\"action\":\"CALL\",\"amount\":0,\"reason\":\"odds\"}",
        "{\"action\":\"CALL\",\"amount\":0,\"reason\":\"odds\"} Hope that helps.",
        "{\"action\":\"CALL\",\"amount\":0}{\"action\":\"FOLD\",\"amount\":0}",
        "{\"action\":\"CALL\",\"amount\":0",
        "{'action':'CALL','amount':0}",
        "{action:CALL,amount:0}",
        "{\"action\":\"CALL\",\"amount\":0,}",
        "{\"action\":\"CALL\",\"action\":\"FOLD\",\"amount\":0}",
        "```json\n{\"action\":\"CALL\",\"amount\":0}",
        "{\"action\":\"CALL\",\"amount\":0}\n```",
        "```json\nI will call.\n```",
        "```",
        "``````",
        "{\"action\":\"RAISE\",\"reason\":\"no amount\"}",
        "{\"action\":\"RAISE\",\"amount\":null}",
        "{\"action\":\"RAISE\",\"amount\":\"200\"}",
        "{\"action\":\"RAISE\",\"amount\":true}",
        "{\"action\":\"RAISE\",\"amount\":[200]}",
        "{\"action\":\"RAISE\",\"amount\":200.5}",
        "{\"action\":\"RAISE\",\"amount\":1e-3}"
    })
    void anAnswerThatIsNotTheAgreedJsonObjectIsRefused(String text) {
        assertUnavailable(facingBet(), text);
    }

    @Test
    void anEmptyAnswerIsRefused() {
        assertUnavailable(facingBet(), "");
    }

    @Test
    void anActionTypeThatIsNotLegalIsRefused() {
        OpponentUnavailableException checkFacingBet = assertUnavailable(facingBet(), answer("CHECK", 0));
        assertTrue(checkFacingBet.getMessage().contains("CHECK"), checkFacingBet.getMessage());
        assertUnavailable(facingBet(), answer("BET", 200));
        assertUnavailable(nobodyHasBet(), answer("CALL", 0));
        assertUnavailable(nobodyHasBet(), answer("RAISE", 60));
        assertUnavailable(callOrFold(), answer("RAISE", 500));
        assertUnavailable(callOrFold(), answer("CHECK", 0));
    }

    // ---- replies of the service that carry no usable answer -------------

    @ParameterizedTest
    @CsvSource({
        "400, not accepted", "401, API key", "403, API key", "404, not found",
        "429, too many requests", "500, problem", "503, problem", "529, problem", "302, not accepted",
        "199, not accepted"
    })
    void anErrorStatusIsReportedWithItsNumber(int status, String hint) {
        String errorBody = "{\"type\":\"error\",\"error\":{\"type\":\"some_error\",\"message\":\"details\"}}";
        ScriptedTransport transport = ScriptedTransport.replying(status, errorBody);
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent(transport).decide(facingBet(), PROFILE));
        assertTrue(failure.getMessage().contains("HTTP " + status), failure.getMessage());
        assertTrue(failure.getMessage().contains(hint), failure.getMessage());
        assertEquals(1, transport.calls(), "a failed request is not tried again");
    }

    @Test
    void anErrorStatusWinsOverABodyThatLooksLikeAnAnswer() {
        ScriptedTransport transport = ScriptedTransport.replying(500, messageWith(answer("CALL", 0)));
        assertThrows(OpponentUnavailableException.class, () -> opponent(transport).decide(facingBet(), PROFILE));
    }

    @Test
    void aReplyThatIsNotJsonIsRefused() {
        assertReplyRefused("<html><body>Bad gateway</body></html>");
        assertReplyRefused("");
        assertReplyRefused(null);
        assertReplyRefused("[1,2,3]");
    }

    @Test
    void aReplyWithoutAnAnswerInItIsRefused() {
        assertReplyRefused("{}");
        assertReplyRefused("{\"type\":\"message\",\"stop_reason\":\"end_turn\"}");
        assertReplyRefused("{\"type\":\"message\",\"content\":\"CALL\",\"stop_reason\":\"end_turn\"}");
        assertReplyRefused("{\"type\":\"message\",\"content\":[],\"stop_reason\":\"end_turn\"}");
        assertReplyRefused("{\"type\":\"message\",\"content\":[\"CALL\", 7, null],\"stop_reason\":\"end_turn\"}");
        assertReplyRefused("{\"type\":\"message\",\"content\":[{\"type\":\"text\",\"text\":\"  \"}]}");
        assertReplyRefused("{\"type\":\"message\",\"content\":[{\"type\":\"text\"}]}");
        assertReplyRefused("{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"oops\"}}");
    }

    @Test
    void aReplyThatOnlyHoldsThinkingIsRefused() {
        JSONObject thinking = new JSONObject().put("type", "thinking").put("thinking", "").put("signature", "abc");
        assertReplyRefused(message("end_turn", thinking).toString());
    }

    @Test
    void aRefusalIsReportedEvenWithText() {
        OpponentUnavailableException empty = assertReplyRefused(message("refusal").toString());
        assertTrue(empty.getMessage().contains("declined"), empty.getMessage());
        assertReplyRefused(message("refusal", textBlock(answer("CALL", 0))).toString());
    }

    @Test
    void anAnswerCutOffByTheTokenLimitIsRefused() {
        OpponentUnavailableException failure =
                assertReplyRefused(message("max_tokens", textBlock("{\"action\":\"CALL\",\"amou")).toString());
        assertTrue(failure.getMessage().contains("cut off"), failure.getMessage());
        assertReplyRefused(message("max_tokens", textBlock(answer("CALL", 0))).toString());
    }

    @Test
    void theAnswerIsFoundAfterThinkingBlocks() throws OpponentUnavailableException {
        JSONObject thinking = new JSONObject().put("type", "thinking").put("thinking", "").put("signature", "abc");
        String reply = message("end_turn", thinking, thinking, textBlock(answer("RAISE", 300))).toString();
        ScriptedTransport transport = ScriptedTransport.replying(OK, reply);
        assertEquals(Action.raise(300), opponent(transport).decide(facingBet(), PROFILE));
    }

    @Test
    void anAnswerSplitOverTextBlocksIsPutTogether() throws OpponentUnavailableException {
        String reply = message("end_turn",
                textBlock("{\"action\":\"RAISE\","), textBlock("\"amount\":300}")).toString();
        ScriptedTransport transport = ScriptedTransport.replying(OK, reply);
        assertEquals(Action.raise(300), opponent(transport).decide(facingBet(), PROFILE));
    }

    @Test
    void aReplyWithoutAStopReasonIsStillRead() throws OpponentUnavailableException {
        String reply = new JSONObject().put("content", new JSONArray().put(textBlock(answer("FOLD", 0)))).toString();
        assertEquals(Action.fold(), opponent(ScriptedTransport.replying(OK, reply)).decide(facingBet(), PROFILE));
        String nullReason = "{\"stop_reason\":null,\"content\":[{\"type\":\"text\",\"text\":\"{\\\"action\\\":"
                + "\\\"FOLD\\\",\\\"amount\\\":0}\"}]}";
        assertEquals(Action.fold(), opponent(ScriptedTransport.replying(OK, nullReason)).decide(facingBet(), PROFILE));
    }

    // ---- requests that get no reply -------------------------------------

    @Test
    void aTimeoutIsReportedWithTheTimeAllowed() {
        ScriptedTransport transport = ScriptedTransport.failing(new HttpTimeoutException("request timed out"));
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent(transport).decide(facingBet(), PROFILE));
        assertTrue(failure.getMessage().contains("did not answer within 12 s"), failure.getMessage());
        assertEquals(1, transport.calls(), "a request that timed out is not tried again");
    }

    @Test
    void aTimeoutNamesTheConfiguredTime() {
        ScriptedTransport transport = ScriptedTransport.failing(new HttpTimeoutException("request timed out"));
        LlmOpponentDataAccessObject opponent =
                new LlmOpponentDataAccessObject(new LlmSettings(KEY, null, null, 3), transport);
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent.decide(facingBet(), PROFILE));
        assertTrue(failure.getMessage().contains("within 3 s"), failure.getMessage());
    }

    @Test
    void aConnectionFailureIsReportedByItsKind() {
        ScriptedTransport transport = ScriptedTransport.failing(new ConnectException("Connection refused"));
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent(transport).decide(facingBet(), PROFILE));
        assertTrue(failure.getMessage().contains("could not be reached"), failure.getMessage());
        assertTrue(failure.getMessage().contains("ConnectException"), failure.getMessage());
    }

    @Test
    void aFailureOfAnUnnamedKindIsStillReported() {
        ScriptedTransport transport = ScriptedTransport.failing(new IOException("broken pipe") { });
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent(transport).decide(facingBet(), PROFILE));
        assertTrue(failure.getMessage().contains("could not be reached"), failure.getMessage());
        assertFalse(failure.getMessage().contains("()"), failure.getMessage());
    }

    @Test
    void aTransportThatBreaksDownDoesNotBreakTheGame() {
        ScriptedTransport crashing = ScriptedTransport.crashing(new IllegalStateException("selector closed"));
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent(crashing).decide(facingBet(), PROFILE));
        assertTrue(failure.getMessage().contains("IllegalStateException"), failure.getMessage());

        ScriptedTransport silent = ScriptedTransport.replying(null);
        assertThrows(OpponentUnavailableException.class, () -> opponent(silent).decide(facingBet(), PROFILE));
    }

    // ---- a service that did not answer in time is left alone for a minute --

    // The clock of a real machine may start anywhere. It starts in several places here, one of them below
    // zero and two of them so late that the minute ends after the clock has wrapped around.
    @ParameterizedTest
    @ValueSource(longs = {0L, 987_654_321L, -7_000_000_000_000L, Long.MIN_VALUE, Long.MAX_VALUE - 1_000_000_000L,
        Long.MAX_VALUE})
    void afterATimeoutTheServiceIsLeftAloneForAMinuteAndThenAskedAgain(long start)
            throws OpponentUnavailableException {
        AtomicLong clock = new AtomicLong(start);
        ScriptedTransport transport = ScriptedTransport.failing(
                new HttpTimeoutException("timed out sending x-api-key: " + KEY));
        LlmOpponentDataAccessObject opponent = opponent(transport, clock);

        OpponentUnavailableException timeout = assertThrows(OpponentUnavailableException.class,
                () -> opponent.decide(facingBet(), PROFILE));
        assertTrue(timeout.getMessage().contains("did not answer within 12 s"), timeout.getMessage());
        assertEquals(1, transport.calls());

        // The service would answer by now, but nobody asks it while the minute lasts.
        transport.answersFromNowOn(answer("CALL", 0));
        for (long later : new long[] {0, 1, SECOND_NANOS, MINUTE_NANOS / 2, MINUTE_NANOS - 1}) {
            clock.set(start + later);
            OpponentUnavailableException paused = assertThrows(OpponentUnavailableException.class,
                    () -> opponent.decide(facingBet(), PROFILE), () -> "asked " + later + " ns after the timeout");
            assertTrue(paused.getMessage().contains("did not answer in time"), paused.getMessage());
            assertTrue(paused.getMessage().contains("left alone for a minute"), paused.getMessage());
            assertTrue(paused.getMessage().length() <= 100, "the reason should be short: " + paused.getMessage());
            assertFalse(paused.toString().contains(KEY), paused.toString());
            assertNull(paused.getCause(), "the timeout is not attached: its text is not ours");
        }
        assertEquals(1, transport.calls(), "the service must not be asked while it is left alone");

        clock.set(start + MINUTE_NANOS);
        assertEquals(Action.call(), opponent.decide(facingBet(), PROFILE));
        assertEquals(2, transport.calls(), "the service is asked again when the minute is over");
        // An answer that came in time leaves no pause behind.
        assertEquals(Action.call(), opponent.decide(facingBet(), PROFILE));
        assertEquals(3, transport.calls());
    }

    @Test
    void everyTimeoutStartsAMinuteOfItsOwn() {
        AtomicLong clock = new AtomicLong();
        // A connection that could not be made in time is a timeout too.
        ScriptedTransport transport = ScriptedTransport.failing(new HttpConnectTimeoutException("connect timed out"));
        LlmOpponentDataAccessObject opponent = opponent(transport, clock);

        assertThrows(OpponentUnavailableException.class, () -> opponent.decide(facingBet(), PROFILE));
        clock.set(MINUTE_NANOS + SECOND_NANOS);
        assertThrows(OpponentUnavailableException.class, () -> opponent.decide(facingBet(), PROFILE));
        assertEquals(2, transport.calls(), "the first minute is over, so the service is asked again");

        // The second minute is counted from the second timeout, not from the first.
        clock.set(2 * MINUTE_NANOS);
        assertThrows(OpponentUnavailableException.class, () -> opponent.decide(facingBet(), PROFILE));
        assertEquals(2, transport.calls(), "the second minute is not over yet");
        clock.set(2 * MINUTE_NANOS + SECOND_NANOS);
        assertThrows(OpponentUnavailableException.class, () -> opponent.decide(facingBet(), PROFILE));
        assertEquals(3, transport.calls());
    }

    @Test
    void aFailureThatCostsNoWaitingStartsNoPause() {
        List<ScriptedTransport> transports = List.of(
                ScriptedTransport.failing(new ConnectException("Connection refused")),
                ScriptedTransport.replying(500, "{}"),
                ScriptedTransport.answering("I think I will call here."),
                ScriptedTransport.replying(null),
                ScriptedTransport.crashing(new IllegalStateException("selector closed")));
        for (ScriptedTransport transport : transports) {
            LlmOpponentDataAccessObject opponent = opponent(transport, new AtomicLong());
            assertThrows(OpponentUnavailableException.class, () -> opponent.decide(facingBet(), PROFILE));
            assertThrows(OpponentUnavailableException.class, () -> opponent.decide(facingBet(), PROFILE));
            assertEquals(2, transport.calls(), "only a request that timed out keeps the next one from being sent");
        }
    }

    // ---- no key ----------------------------------------------------------

    @Test
    void withoutAKeyNothingIsSent() {
        for (String missing : new String[] {null, "", "   ", "\n"}) {
            ScriptedTransport transport = ScriptedTransport.answering(answer("CALL", 0));
            LlmOpponentDataAccessObject opponent =
                    new LlmOpponentDataAccessObject(new LlmSettings(missing, null, null, 0), transport);
            assertFalse(opponent.isConfigured());
            OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                    () -> opponent.decide(facingBet(), PROFILE));
            assertTrue(failure.getMessage().contains("No API key"), failure.getMessage());
            assertEquals(0, transport.calls(), "the transport must not be used without a key");
        }
    }

    @Test
    void withAKeyTheOpponentIsConfigured() {
        assertTrue(opponent(ScriptedTransport.answering(answer("CALL", 0))).isConfigured());
    }

    // ---- arguments -------------------------------------------------------

    @Test
    void missingArgumentsAreRejected() {
        ScriptedTransport transport = ScriptedTransport.answering(answer("CALL", 0));
        LlmOpponentDataAccessObject opponent = opponent(transport);
        assertThrows(IllegalArgumentException.class, () -> opponent.decide(null, PROFILE));
        assertThrows(IllegalArgumentException.class, () -> opponent.decide(facingBet(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new LlmOpponentDataAccessObject(null, transport));
        assertThrows(IllegalArgumentException.class,
                () -> new LlmOpponentDataAccessObject(new LlmSettings(KEY, null, null, 0), null));
        assertThrows(IllegalArgumentException.class,
                () -> new LlmOpponentDataAccessObject(new LlmSettings(KEY, null, null, 0), transport, null));
        assertEquals(0, transport.calls());
    }

    @Test
    void aTurnWithoutLegalActionsIsNotSent() {
        DecisionContext nothingLegal = new DecisionContext(cards("A♠", "K♦"), List.of(), 20, 0,
                List.of(), 0, 0, 0.5);
        ScriptedTransport transport = ScriptedTransport.answering(answer("FOLD", 0));
        assertThrows(OpponentUnavailableException.class, () -> opponent(transport).decide(nothingLegal, PROFILE));
        assertEquals(0, transport.calls());
    }

    // ---- the request -----------------------------------------------------

    @Test
    void theRequestGoesToTheMessagesApiWithTheKeyInAHeader() throws OpponentUnavailableException {
        ScriptedTransport transport = ScriptedTransport.answering(answer("CALL", 0));
        opponent(transport).decide(facingBet(), PROFILE);

        assertEquals(1, transport.calls());
        assertEquals("https://api.anthropic.com/v1/messages", transport.lastUrl());
        Map<String, String> headers = transport.lastHeaders();
        assertEquals(Set.of("content-type", "anthropic-version", "x-api-key"), headers.keySet());
        assertEquals(KEY, headers.get("x-api-key"));
        assertEquals("application/json", headers.get("content-type"));
        assertEquals("2026-10-21", headers.get("anthropic-version"));
        assertFalse(transport.lastBody().contains(KEY), "the key belongs in the header only");
        assertFalse(transport.lastUrl().contains(KEY), "the key belongs in the header only");
    }

    @Test
    void theRequestUsesTheConfiguredModelAndAddress() throws OpponentUnavailableException {
        ScriptedTransport transport = ScriptedTransport.answering(answer("CALL", 0));
        LlmSettings settings = new LlmSettings(KEY, "claude-sonnet-5-5", "http://localhost:8080/proxy/", 30);
        new LlmOpponentDataAccessObject(settings, transport).decide(facingBet(), PROFILE);

        assertEquals("http://localhost:8080/proxy/v1/messages", transport.lastUrl());
        assertEquals("claude-sonnet-5-5", new JSONObject(transport.lastBody()).getString("model"));
    }

    @Test
    void theRequestBodyHasTheShapeTheServiceExpects() throws OpponentUnavailableException {
        ScriptedTransport transport = ScriptedTransport.answering(answer("CALL", 0));
        opponent(transport).decide(facingBet(), PROFILE);
        JSONObject body = new JSONObject(transport.lastBody());

        assertEquals(Set.of("model", "max_tokens", "system", "messages", "output_config"), body.keySet());
        assertEquals("claude-haiku-5-5", body.getString("model"));
        int maxTokens = body.getInt("max_tokens");
        assertTrue(maxTokens >= 256 && maxTokens <= 2048, "max_tokens should stay small: " + maxTokens);
        assertEquals(LlmPrompt.systemPrompt(PROFILE), body.getString("system"));

        JSONArray messages = body.getJSONArray("messages");
        assertEquals(1, messages.length());
        JSONObject message = messages.getJSONObject(0);
        assertEquals(Set.of("role", "content"), message.keySet());
        assertEquals("user", message.getString("role"));
        assertEquals(LlmPrompt.userMessage(facingBet()), message.getString("content"));

        JSONObject outputConfig = body.getJSONObject("output_config");
        assertEquals(Set.of("effort", "format"), outputConfig.keySet());
        assertEquals("low", outputConfig.getString("effort"));
        JSONObject format = outputConfig.getJSONObject("format");
        assertEquals(Set.of("type", "schema"), format.keySet());
        assertEquals("json_schema", format.getString("type"));

        JSONObject schema = format.getJSONObject("schema");
        assertEquals("object", schema.getString("type"));
        assertFalse(schema.getBoolean("additionalProperties"));
        assertEquals(List.of("action", "amount", "reason"), schema.getJSONArray("required").toList());
        JSONObject properties = schema.getJSONObject("properties");
        assertEquals(Set.of("action", "amount", "reason"), properties.keySet());
        assertEquals("string", properties.getJSONObject("action").getString("type"));
        assertEquals("integer", properties.getJSONObject("amount").getString("type"));
        assertEquals("string", properties.getJSONObject("reason").getString("type"));
    }

    @Test
    void theSchemaOnlyOffersTheLegalActionTypes() throws OpponentUnavailableException {
        assertEquals(List.of("FOLD", "CALL", "RAISE"), offeredActions(facingBet()));
        assertEquals(List.of("FOLD", "CHECK", "BET"), offeredActions(nobodyHasBet()));
        assertEquals(List.of("FOLD", "CALL"), offeredActions(callOrFold()));
        // The order is the declared one, whatever order the context lists them in.
        DecisionContext shuffled = new DecisionContext(cards("A♠", "K♦"), List.of(), 30, 10,
                List.of(ActionType.RAISE, ActionType.CALL, ActionType.FOLD, ActionType.CALL), 40, 990, 0.6);
        assertEquals(List.of("FOLD", "CALL", "RAISE"), offeredActions(shuffled));
    }

    @Test
    void theSameTurnAlwaysGivesTheSameRequest() throws OpponentUnavailableException {
        ScriptedTransport first = ScriptedTransport.answering(answer("CALL", 0));
        ScriptedTransport second = ScriptedTransport.answering(answer("FOLD", 0));
        opponent(first).decide(facingBet(), PROFILE);
        opponent(second).decide(facingBet(), PROFILE);
        assertEquals(first.lastBody(), second.lastBody());
    }

    @Test
    void theProfileOnlyChangesTheSystemPrompt() throws OpponentUnavailableException {
        ScriptedTransport balanced = ScriptedTransport.answering(answer("CALL", 0));
        ScriptedTransport tight = ScriptedTransport.answering(answer("CALL", 0));
        opponent(balanced).decide(facingBet(), PROFILE);
        opponent(tight).decide(facingBet(), new BotProfile("Tight", 0.1));

        JSONObject balancedBody = new JSONObject(balanced.lastBody());
        JSONObject tightBody = new JSONObject(tight.lastBody());
        assertNotEquals(balancedBody.getString("system"), tightBody.getString("system"));
        balancedBody.remove("system");
        tightBody.remove("system");
        assertTrue(balancedBody.similar(tightBody));
    }

    // ---- the model sees only what the computer player sees ----------------

    @Test
    void theRequestHoldsNothingThatIsNotInTheDecisionContext() throws OpponentUnavailableException {
        int turnsChecked = 0;
        Set<Integer> boardSizesSeen = new HashSet<>();
        for (long seed = 1; seed <= 30; seed++) {
            Match match = new Match("Hikaru", "Deep Blue", 1237, 5, 10);
            Random random = new Random(seed);
            // Two hands, so that the computer is the small blind in one and the big blind in the other.
            for (int hand = 0; hand < 2 && !match.isOver(); hand++) {
                Game game = match.startNextHand(random);
                while (!game.isHandOver()) {
                    if (game.getPlayerToAct() == match.getComputer()) {
                        // Any chance will do here: the test is about what is sent, not about the odds.
                        DecisionContext context = game.getDecisionContext(random.nextDouble());
                        assertRequestHoldsOnlyTheContext(context, match);
                        boardSizesSeen.add(context.getBoardCards().size());
                        turnsChecked++;
                    }
                    game.apply(someLegalAction(game, random));
                }
            }
        }
        assertTrue(turnsChecked >= 100, "turns checked: " + turnsChecked);
        assertEquals(Set.of(0, 3, 4, 5), boardSizesSeen, "every betting round should have been checked");
    }

    @Test
    void theSystemPromptDoesNotDependOnTheTurn() throws OpponentUnavailableException {
        ScriptedTransport first = ScriptedTransport.answering(answer("CALL", 0));
        ScriptedTransport second = ScriptedTransport.answering(answer("CHECK", 0));
        opponent(first).decide(facingBet(), PROFILE);
        opponent(second).decide(nobodyHasBet(), PROFILE);
        String firstSystem = new JSONObject(first.lastBody()).getString("system");
        String secondSystem = new JSONObject(second.lastBody()).getString("system");
        assertEquals(firstSystem, secondSystem);
        for (Suit suit : Suit.values()) {
            assertFalse(firstSystem.contains(suit.getSymbol()), "the system prompt must not name a card");
        }
    }

    // ---- the key stays secret ---------------------------------------------

    @Test
    void theKeyIsNeverPartOfAnException() {
        String echo = "{\"type\":\"error\",\"error\":{\"type\":\"" + KEY + "\",\"message\":\"bad key " + KEY + "\"}}";
        List<ScriptedTransport> transports = List.of(
                ScriptedTransport.replying(401, echo),
                ScriptedTransport.replying(500, KEY),
                ScriptedTransport.replying(OK, KEY),
                ScriptedTransport.replying(OK, messageWith("my key is " + KEY)),
                ScriptedTransport.replying(OK, messageWith("{\"action\":\"" + KEY + "\",\"amount\":0}")),
                ScriptedTransport.replying(OK, messageWith("{\"action\":\"RAISE\",\"amount\":\"" + KEY + "\"}")),
                ScriptedTransport.replying(OK, message("refusal", textBlock(KEY)).toString()),
                ScriptedTransport.failing(new IOException("invalid header value: \"" + KEY + "\"")),
                ScriptedTransport.failing(new HttpTimeoutException("timed out sending x-api-key: " + KEY)),
                ScriptedTransport.crashing(new IllegalArgumentException("invalid header value: \"" + KEY + "\"")));
        for (ScriptedTransport transport : transports) {
            OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                    () -> opponent(transport).decide(facingBet(), PROFILE));
            for (Throwable link = failure; link != null; link = link.getCause()) {
                assertFalse(String.valueOf(link.getMessage()).contains(KEY), link.toString());
                assertFalse(link.toString().contains(KEY), link.toString());
                assertEquals(0, link.getSuppressed().length);
            }
            assertNull(failure.getCause(), "a failure from elsewhere is not attached: its text is not ours");
        }
    }

    // ---- helpers ----------------------------------------------------------

    // FOLD, CALL or RAISE: 40 to call into a pot of 120, raise to between 80 and 990.
    private static DecisionContext facingBet() {
        return new DecisionContext(cards("A♠", "K♦"), cards("10♥", "J♣", "2♦"), 120, 40,
                List.of(ActionType.FOLD, ActionType.CALL, ActionType.RAISE), 80, 990, 0.634);
    }

    // FOLD, CHECK or BET: nothing to call, bet between 10 and 995.
    private static DecisionContext nobodyHasBet() {
        return new DecisionContext(cards("7♣", "7♦"), List.of(), 20, 0,
                List.of(ActionType.FOLD, ActionType.CHECK, ActionType.BET), 10, 995, 0.66);
    }

    // FOLD or CALL only: the human is all-in, so there is nothing to raise.
    private static DecisionContext callOrFold() {
        return new DecisionContext(cards("Q♥", "9♥"), cards("2♣", "8♠", "K♥", "3♦"), 700, 300,
                List.of(ActionType.FOLD, ActionType.CALL), 300, 300, 0.31);
    }

    private static List<Card> cards(String... texts) {
        List<Card> cards = new ArrayList<>();
        for (String text : texts) {
            cards.add(card(text));
        }
        return cards;
    }

    private static Card card(String text) {
        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                Card card = new Card(rank, suit);
                if (card.toString().equals(text)) {
                    return card;
                }
            }
        }
        throw new IllegalArgumentException("not a card: " + text);
    }

    private static List<Card> everyCard() {
        List<Card> cards = new ArrayList<>();
        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        return cards;
    }

    private static String answer(String action, int amount) {
        return "{\"action\":\"" + action + "\",\"amount\":" + amount + ",\"reason\":\"because\"}";
    }

    private static JSONObject textBlock(String text) {
        return new JSONObject().put("type", "text").put("text", text);
    }

    // A reply in the form of the Messages API.
    private static JSONObject message(String stopReason, JSONObject... blocks) {
        JSONArray content = new JSONArray();
        for (JSONObject block : blocks) {
            content.put(block);
        }
        return new JSONObject()
                .put("id", "msg_test")
                .put("type", "message")
                .put("role", "assistant")
                .put("model", "claude-haiku-5-5")
                .put("content", content)
                .put("stop_reason", stopReason)
                .put("usage", new JSONObject().put("input_tokens", 400).put("output_tokens", 30));
    }

    private static String messageWith(String answerText) {
        return message("end_turn", textBlock(answerText)).toString();
    }

    private static LlmOpponentDataAccessObject opponent(LlmTransport transport) {
        return new LlmOpponentDataAccessObject(new LlmSettings(KEY, null, null, 0), transport);
    }

    // An opponent whose time is whatever the test puts into the clock, in nanoseconds.
    private static LlmOpponentDataAccessObject opponent(LlmTransport transport, AtomicLong clock) {
        return new LlmOpponentDataAccessObject(new LlmSettings(KEY, null, null, 0), transport, clock::get);
    }

    private static Action decide(DecisionContext context, String answerText) throws OpponentUnavailableException {
        return opponent(ScriptedTransport.answering(answerText)).decide(context, PROFILE);
    }

    private static OpponentUnavailableException assertUnavailable(DecisionContext context, String answerText) {
        ScriptedTransport transport = ScriptedTransport.answering(answerText);
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent(transport).decide(context, PROFILE), () -> "accepted: " + answerText);
        assertFalse(failure.getMessage().isBlank());
        assertTrue(failure.getMessage().length() <= 100, "the reason should be short: " + failure.getMessage());
        return failure;
    }

    private static OpponentUnavailableException assertReplyRefused(String replyBody) {
        ScriptedTransport transport = ScriptedTransport.replying(OK, replyBody);
        OpponentUnavailableException failure = assertThrows(OpponentUnavailableException.class,
                () -> opponent(transport).decide(facingBet(), PROFILE), () -> "accepted: " + replyBody);
        assertTrue(failure.getMessage().length() <= 100, "the reason should be short: " + failure.getMessage());
        return failure;
    }

    private static List<Object> offeredActions(DecisionContext context) throws OpponentUnavailableException {
        ScriptedTransport transport = ScriptedTransport.answering(answer("FOLD", 0));
        opponent(transport).decide(context, PROFILE);
        return new JSONObject(transport.lastBody()).getJSONObject("output_config").getJSONObject("format")
                .getJSONObject("schema").getJSONObject("properties").getJSONObject("action")
                .getJSONArray("enum").toList();
    }

    // Picks an action for whoever is to act, so that hands reach every betting round.
    private static Action someLegalAction(Game game, Random random) {
        List<ActionType> legal = game.getLegalActionTypes();
        int roll = random.nextInt(10);
        Action action;
        if (roll < 2 && (legal.contains(ActionType.BET) || legal.contains(ActionType.RAISE))) {
            int total = Math.min(game.getMinWager() + random.nextInt(40), game.getMaxWager());
            if (legal.contains(ActionType.BET)) {
                action = Action.bet(total);
            }
            else {
                action = Action.raise(total);
            }
        }
        else if (legal.contains(ActionType.CHECK)) {
            action = Action.check();
        }
        else if (roll == 9) {
            action = Action.fold();
        }
        else {
            action = Action.call();
        }
        return action;
    }

    // Sends the request for a turn of a real game and checks everything in it: the cards it names, the
    // numbers it gives, and that it says nothing about the human player.
    private static void assertRequestHoldsOnlyTheContext(DecisionContext context, Match match)
            throws OpponentUnavailableException {
        ScriptedTransport transport = ScriptedTransport.answering(answer("FOLD", 0));
        opponent(transport).decide(context, PROFILE);
        JSONObject body = new JSONObject(transport.lastBody());

        // 1. The parts of the request: nothing but the model, the limit, the two texts, and the answer form.
        assertEquals(Set.of("model", "max_tokens", "system", "messages", "output_config"), body.keySet());
        assertEquals(LlmPrompt.systemPrompt(PROFILE), body.getString("system"));
        assertEquals(1, body.getJSONArray("messages").length());
        String userMessage = body.getJSONArray("messages").getJSONObject(0).getString("content");

        // 2. Cards: every card named anywhere in the request is one the computer player can see.
        List<String> texts = new ArrayList<>();
        collectTexts(body, texts);
        Set<Card> visible = new HashSet<>(context.getHoleCards());
        visible.addAll(context.getBoardCards());
        for (Card card : everyCard()) {
            for (String text : texts) {
                assertTrue(visible.contains(card) || !text.contains(card.toString()),
                        () -> "the request names " + card + ", which the computer cannot see: " + text);
            }
        }
        for (Card card : match.getHuman().getHoleCards().getCards()) {
            assertFalse(transport.lastBody().contains(card.toString()), "the human's " + card + " was sent");
        }
        for (Card card : visible) {
            assertTrue(userMessage.contains(card.toString()), "the computer's own " + card + " is missing");
        }

        // 3. Numbers: every number in the description of the turn is in the context, or follows from it.
        Set<String> allowed = new HashSet<>();
        allowed.add(String.valueOf(context.getPot()));
        allowed.add(String.valueOf(context.getAmountToCall()));
        allowed.add(String.valueOf(context.getMinWager()));
        allowed.add(String.valueOf(context.getMaxWager()));
        addNumbers(allowed, percent(context.getWinChance()));
        if (context.getAmountToCall() > 0) {
            addNumbers(allowed, percent(
                    (double) context.getAmountToCall() / (context.getPot() + context.getAmountToCall())));
        }
        for (Card card : visible) {
            addNumbers(allowed, card.toString());
        }
        Set<String> used = new HashSet<>();
        addNumbers(used, userMessage);
        used.removeAll(allowed);
        assertTrue(used.isEmpty(), () -> "numbers that are not in the context: " + used + " in\n" + userMessage);

        // 4. The players: neither name is sent.
        for (String text : texts) {
            assertFalse(text.contains(match.getHuman().getName()), text);
            assertFalse(text.contains(match.getComputer().getName()), text);
        }

        // 5. The answer form offers exactly the legal action types.
        List<Object> offered = body.getJSONObject("output_config").getJSONObject("format").getJSONObject("schema")
                .getJSONObject("properties").getJSONObject("action").getJSONArray("enum").toList();
        List<String> legal = new ArrayList<>();
        for (ActionType type : ActionType.values()) {
            if (context.isLegal(type)) {
                legal.add(type.name());
            }
        }
        assertEquals(legal, offered);
    }

    private static String percent(double chance) {
        return String.format(Locale.ROOT, "%.1f", chance * 100);
    }

    private static void addNumbers(Set<String> numbers, String text) {
        Matcher matcher = Pattern.compile("\\d+").matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
    }

    // Collects every text in a JSON value, keys included.
    private static void collectTexts(Object value, List<String> texts) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            for (String key : object.keySet()) {
                texts.add(key);
                collectTexts(object.get(key), texts);
            }
        }
        else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                collectTexts(array.get(i), texts);
            }
        }
        else {
            texts.add(String.valueOf(value));
        }
    }

    /**
     * A transport that remembers what it was asked to send and gives back a
     * prepared reply or failure.
     */
    static final class ScriptedTransport implements LlmTransport {
        private final List<String> urls = new ArrayList<>();
        private final List<Map<String, String>> headers = new ArrayList<>();
        private final List<String> bodies = new ArrayList<>();
        private LlmHttpReply reply;
        private IOException failure;
        private RuntimeException crash;

        static ScriptedTransport replying(LlmHttpReply reply) {
            ScriptedTransport transport = new ScriptedTransport();
            transport.reply = reply;
            return transport;
        }

        static ScriptedTransport replying(int status, String body) {
            return replying(new LlmHttpReply(status, body));
        }

        static ScriptedTransport answering(String answerText) {
            return replying(OK, messageWith(answerText));
        }

        static ScriptedTransport failing(IOException failure) {
            ScriptedTransport transport = new ScriptedTransport();
            transport.failure = failure;
            return transport;
        }

        static ScriptedTransport crashing(RuntimeException crash) {
            ScriptedTransport transport = new ScriptedTransport();
            transport.crash = crash;
            return transport;
        }

        // Changes the script: whatever the transport did before, from now on it gives this answer.
        void answersFromNowOn(String answerText) {
            this.failure = null;
            this.crash = null;
            this.reply = new LlmHttpReply(OK, messageWith(answerText));
        }

        @Override
        public LlmHttpReply post(String url, Map<String, String> requestHeaders, String body) throws IOException {
            this.urls.add(url);
            this.headers.add(requestHeaders);
            this.bodies.add(body);
            if (this.failure != null) {
                throw this.failure;
            }
            if (this.crash != null) {
                throw this.crash;
            }
            return this.reply;
        }

        int calls() {
            return this.bodies.size();
        }

        String lastUrl() {
            return this.urls.get(this.urls.size() - 1);
        }

        Map<String, String> lastHeaders() {
            return this.headers.get(this.headers.size() - 1);
        }

        String lastBody() {
            return this.bodies.get(this.bodies.size() - 1);
        }
    }
}
