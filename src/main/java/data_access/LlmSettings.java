package data_access;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * How {@link LlmOpponentDataAccessObject} reaches the LLM service: the API
 * key, the model, the address of the service, and how long to wait for it.
 *
 * <p>The API key is taken from the environment variable
 * {@value #API_KEY_VARIABLE}, or else from the entry {@value #API_KEY_PROPERTY}
 * of a properties file. The same file may also set {@value #MODEL_PROPERTY}
 * and {@value #TIMEOUT_PROPERTY} (a whole number of seconds). A missing or
 * unreadable file, a missing entry, or a value that cannot be used leaves the
 * default in place.
 *
 * <p>A properties file with every entry looks like this; each line is
 * optional:
 * <pre>
 * api_key=sk-ant-...
 * model=claude-haiku-5-5
 * timeout_seconds=12
 * </pre>
 *
 * <p>The default model is the fastest and cheapest current Claude model. It
 * was chosen because the opponent's turn is a small decision that a person is
 * waiting for. Another model can be named, but it has to be a current one:
 * the request asks for a structured output and sets the effort, which older
 * models reject. The service then answers with an error and the built-in
 * strategy plays instead.
 *
 * <p>The key is only kept in memory: it is never written anywhere, and
 * {@link #toString()} leaves it out.
 */
final class LlmSettings {

    /** The environment variable that holds the API key. */
    static final String API_KEY_VARIABLE = "ANTHROPIC_API_KEY";

    /** The entry of the properties file that holds the API key. */
    static final String API_KEY_PROPERTY = "api_key";

    /** The entry of the properties file that names the model. */
    static final String MODEL_PROPERTY = "model";

    /** The entry of the properties file that holds the time to wait, in seconds. */
    static final String TIMEOUT_PROPERTY = "timeout_seconds";

    /** The model used unless the properties file names another. */
    static final String DEFAULT_MODEL = "claude-haiku-5-5";

    /** The production endpoint. Test transports may target an isolated loopback server. */
    static final String DEFAULT_BASE_URL = "https://api.anthropic.com";

    /** The seconds a whole request may take unless the properties file says otherwise. */
    static final int DEFAULT_TIMEOUT_SECONDS = 12;

    private static final char FIRST_KEY_CHARACTER = '!';
    private static final char LAST_KEY_CHARACTER = '~';
    private static final String QUOTES = "\"'";
    private static final String BYTE_ORDER_MARK = "\uFEFF";
    private static final String PATH_SEPARATOR = "/";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;

    /**
     * Creates settings from values that are already known. A value that cannot
     * be used is replaced by its default; an API key that cannot be used
     * counts as no key.
     *
     * @param apiKey the API key, or null if there is none
     * @param model the name of the model, or null for the default
     * @param baseUrl the root address of the service, or null for the default
     * @param timeoutSeconds the seconds a whole request may take; a number
     *        that is not positive gives the default
     */
    LlmSettings(String apiKey, String model, String baseUrl, int timeoutSeconds) {
        this.apiKey = usableKey(apiKey);
        this.model = textOrDefault(model, DEFAULT_MODEL);
        this.baseUrl = withoutTrailingSeparators(textOrDefault(baseUrl, DEFAULT_BASE_URL));
        if (timeoutSeconds > 0) {
            this.timeout = Duration.ofSeconds(timeoutSeconds);
        }
        else {
            this.timeout = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);
        }
    }

    /**
     * Reads the settings from the environment and a properties file.
     *
     * @param environment the environment variables, by name
     * @param file the properties file; it does not have to exist, and null
     *        means there is none
     * @return the settings
     * @throws IllegalArgumentException if environment is null
     */
    static LlmSettings load(Map<String, String> environment, Path file) {
        if (environment == null) {
            throw new IllegalArgumentException("environment must not be null");
        }
        Properties properties = readProperties(file);
        String apiKey = usableKey(environment.get(API_KEY_VARIABLE));
        if (apiKey.isEmpty()) {
            apiKey = properties.getProperty(API_KEY_PROPERTY);
        }
        return new LlmSettings(apiKey, properties.getProperty(MODEL_PROPERTY),
                DEFAULT_BASE_URL, wholeNumber(properties.getProperty(TIMEOUT_PROPERTY)));
    }

    /**
     * Returns whether there is an API key, without which the service cannot
     * be used.
     */
    boolean isConfigured() {
        return !this.apiKey.isEmpty();
    }

    /**
     * Returns the API key, or an empty text if there is none.
     */
    String getApiKey() {
        return this.apiKey;
    }

    String getModel() {
        return this.model;
    }

    /**
     * Returns the root address of the service, without a path and without a
     * slash at the end.
     */
    String getBaseUrl() {
        return this.baseUrl;
    }

    /**
     * Returns the longest a whole request may take.
     */
    Duration getTimeout() {
        return this.timeout;
    }

    /**
     * Describes the settings without revealing the API key.
     */
    @Override
    public String toString() {
        String key = "not set";
        if (isConfigured()) {
            key = "set";
        }
        return "LlmSettings{model=" + this.model + ", baseUrl=" + this.baseUrl
                + ", timeout=" + this.timeout.getSeconds() + "s, apiKey=" + key + "}";
    }

    private static Properties readProperties(Path file) {
        Properties properties = new Properties();
        if (file != null && Files.isRegularFile(file)) {
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (text.startsWith(BYTE_ORDER_MARK)) {
                    // Some editors put this mark at the start of a file; it is not part of the first entry.
                    text = text.substring(BYTE_ORDER_MARK.length());
                }
                properties.load(new StringReader(text));
            }
            catch (IOException | IllegalArgumentException exception) {
                // A file that cannot be read counts as no file, so nothing half-read is used.
                properties = new Properties();
            }
        }
        return properties;
    }

    /**
     * Returns the key without the white space around it, or an empty text if
     * it is missing or cannot be an API key. A key is made of visible ASCII
     * characters only, which is also what an HTTP header can carry. A pair of
     * quotation marks around the key, which is easily copied along with it,
     * is dropped.
     *
     * @param candidate the text that may be a key, or null
     * @return the key, or an empty text
     */
    private static String usableKey(String candidate) {
        if (candidate == null) {
            return "";
        }
        String key = candidate.strip();
        if (key.length() >= 2 && QUOTES.indexOf(key.charAt(0)) >= 0 && key.charAt(0) == key.charAt(key.length() - 1)) {
            key = key.substring(1, key.length() - 1).strip();
        }
        for (int i = 0; i < key.length(); i++) {
            char character = key.charAt(i);
            if (character < FIRST_KEY_CHARACTER || character > LAST_KEY_CHARACTER) {
                return "";
            }
        }
        return key;
    }

    private static String textOrDefault(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.strip();
    }

    private static String withoutTrailingSeparators(String address) {
        String result = address;
        while (result.endsWith(PATH_SEPARATOR)) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.isEmpty()) {
            result = DEFAULT_BASE_URL;
        }
        return result;
    }

    /**
     * Reads the whole number written in a text.
     *
     * @param text the text, or null
     * @return the number, or 0 if the text is null or is not a whole number
     */
    private static int wholeNumber(String text) {
        int number = 0;
        if (text != null) {
            try {
                number = Integer.parseInt(text.strip());
            }
            catch (NumberFormatException exception) {
                number = 0;
            }
        }
        return number;
    }
}
