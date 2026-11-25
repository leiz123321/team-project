package data_access;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import entity.Action;
import entity.BotProfile;
import entity.DecisionContext;
import use_case.opponent_action.OpponentActionDecisionDataAccessInterface;
import use_case.opponent_action.OpponentUnavailableException;

/**
 * Asks an LLM for the computer opponent's next action. The service is the
 * Anthropic Messages API, called over HTTP.
 *
 * <p>The API key comes from the environment variable {@code ANTHROPIC_API_KEY}
 * or, if that is not set, from the entry {@code api_key} of the file
 * {@code data/llm.properties}. The same file may also set {@code model}
 * and {@code timeout_seconds}. Without a key this object
 * {@linkplain #isConfigured() is not configured} and never uses the network.
 * The key is sent to the service and nowhere else: it is not logged, not
 * written to a file, and not part of any exception.
 *
 * <p>The model is told how to play by the {@link BotProfile} and what the
 * turn looks like by the {@link DecisionContext}. Nothing else about the game
 * is sent, so the model cannot see the human's cards.
 *
 * <p>Every way in which a decision can fail to arrive ends in an
 * {@link OpponentUnavailableException}: no key, no connection, no reply in
 * time, an error from the service, a refusal, or an answer that cannot be
 * read or names an action that is not legal. The use case then lets the
 * built-in strategy decide. A request is tried once, because a second try
 * would keep the person at the table waiting.
 *
 * <p>A service that did not answer in time is left alone for a minute. Until
 * the minute is over, {@link #decide} fails at once without using the
 * network, so that the person at the table does not wait the whole time
 * again on every turn of the opponent. After the minute the service is tried
 * again, and another request that gets no answer in time starts another
 * minute.
 */
public class LlmOpponentDataAccessObject implements OpponentActionDecisionDataAccessInterface {
    private static final Path DEFAULT_SETTINGS_FILE = Paths.get("data", "llm.properties");

    /** How long the service is left alone after a request that timed out. The message below calls it a minute. */
    private static final long PAUSE_SECONDS = 60;
    private static final long PAUSE_NANOS = TimeUnit.SECONDS.toNanos(PAUSE_SECONDS);
    private static final String PAUSED = "The LLM service did not answer in time and is left alone for a minute.";

    private final LlmSettings settings;
    private final LlmTransport transport;
    private final LongSupplier nanoClock;

    /** Whether a request has ever timed out. Until then {@link #pausedUntil} means nothing. */
    private volatile boolean timedOut;

    /** The reading of the nanosecond clock at which the pause after the latest timeout ends. */
    private volatile long pausedUntil;

    /**
     * Creates the object with the settings found in the environment and in
     * the file {@code data/llm.properties}, which does not have to exist.
     */
    public LlmOpponentDataAccessObject() {
        this(DEFAULT_SETTINGS_FILE);
    }

    /**
     * Creates the object with the settings found in the environment and in
     * the given properties file.
     *
     * @param settingsFile the properties file; it does not have to exist
     */
    public LlmOpponentDataAccessObject(Path settingsFile) {
        this(LlmSettings.load(System.getenv(), settingsFile));
    }

    private LlmOpponentDataAccessObject(LlmSettings settings) {
        this(settings, new LlmHttpTransport(settings.getTimeout()));
    }

    /**
     * Creates the object with settings and a transport that are already
     * made. Tests use this to put a scripted transport in place of the
     * network.
     *
     * @param settings how to reach the service
     * @param transport what sends the requests
     * @throws IllegalArgumentException if an argument is null
     */
    LlmOpponentDataAccessObject(LlmSettings settings, LlmTransport transport) {
        this(settings, transport, System::nanoTime);
    }

    /**
     * Creates the object with settings, a transport, and a clock that are
     * already made. Tests use this to let a minute pass without waiting for
     * it.
     *
     * @param settings how to reach the service
     * @param transport what sends the requests
     * @param nanoClock gives the time in nanoseconds, counted from any fixed
     *        moment, the way {@link System#nanoTime()} does
     * @throws IllegalArgumentException if an argument is null
     */
    LlmOpponentDataAccessObject(LlmSettings settings, LlmTransport transport, LongSupplier nanoClock) {
        if (settings == null || transport == null || nanoClock == null) {
            throw new IllegalArgumentException("settings, transport, and clock must not be null");
        }
        this.settings = settings;
        this.transport = transport;
        this.nanoClock = nanoClock;
    }

    /**
     * Returns whether an API key was found. If not, {@link #decide} fails at
     * once, without using the network.
     *
     * @return true if the LLM service can be asked
     */
    public boolean isConfigured() {
        return this.settings.isConfigured();
    }

    /**
     * Asks the model for the computer opponent's next action. In the minute
     * after a request that got no answer in time, nothing is sent and the
     * call fails at once.
     *
     * @param context what the computer player can see, and nothing else
     * @param profile the playing style the opponent should follow
     * @return an action whose type is legal for the context; the amount of a
     *         bet or a raise is within the legal range
     * @throws OpponentUnavailableException if no decision could be obtained;
     *         the message says why in a few words
     * @throws IllegalArgumentException if context or profile is null
     */
    @Override
    public Action decide(DecisionContext context, BotProfile profile) throws OpponentUnavailableException {
        if (context == null || profile == null) {
            throw new IllegalArgumentException("context and profile must not be null");
        }
        if (!isConfigured()) {
            throw new OpponentUnavailableException("No API key is set for the LLM opponent.");
        }
        if (isPaused()) {
            throw new OpponentUnavailableException(PAUSED);
        }
        if (context.getLegalActionTypes().isEmpty()) {
            throw new OpponentUnavailableException("There is no legal action to choose from.");
        }
        try {
            String body = LlmMessagesApi.requestBody(this.settings.getModel(), LlmPrompt.systemPrompt(profile),
                    LlmPrompt.userMessage(context), context.getLegalActionTypes());
            String answer = LlmMessagesApi.answerText(send(body));
            return LlmDecisionParser.parse(answer, context);
        }
        catch (RuntimeException exception) {
            throw unavailable("The LLM opponent failed", exception);
        }
    }

    private LlmHttpReply send(String body) throws OpponentUnavailableException {
        LlmHttpReply reply;
        try {
            reply = this.transport.post(LlmMessagesApi.endpoint(this.settings.getBaseUrl()),
                    LlmMessagesApi.headers(this.settings.getApiKey()), body);
        }
        catch (HttpTimeoutException exception) {
            // A connection that could not be made in time ends here too. Either way the next turns do not wait.
            startPause();
            long seconds = this.settings.getTimeout().getSeconds();
            throw new OpponentUnavailableException("The LLM service did not answer within " + seconds + " s.");
        }
        catch (IOException exception) {
            throw unavailable("The LLM service could not be reached", exception);
        }
        if (reply == null) {
            throw new OpponentUnavailableException("The LLM service gave no reply.");
        }
        return reply;
    }

    /**
     * Returns whether the service is being left alone after a request that
     * timed out. The two times are compared by their difference, because a
     * nanosecond clock may start anywhere, below zero too.
     */
    private boolean isPaused() {
        return this.timedOut && this.nanoClock.getAsLong() - this.pausedUntil < 0;
    }

    /**
     * Leaves the service alone from now on for the length of the pause.
     */
    private void startPause() {
        this.pausedUntil = this.nanoClock.getAsLong() + PAUSE_NANOS;
        this.timedOut = true;
    }

    /**
     * Builds the exception for a failure of some other kind. Only the kind of
     * failure is named. Its own message is left out and it is not attached as
     * the cause, because that text comes from elsewhere and the API key must
     * never be readable from an exception.
     *
     * @param reason what went wrong, without a full stop
     * @param failure the failure underneath
     * @return the exception to throw
     */
    private static OpponentUnavailableException unavailable(String reason, Exception failure) {
        String kind = failure.getClass().getSimpleName();
        if (kind.isEmpty()) {
            kind = failure.getClass().getName();
        }
        return new OpponentUnavailableException(reason + " (" + kind + ").");
    }
}
