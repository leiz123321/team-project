package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests where the settings of the LLM opponent come from. The environment is
 * always a map made by the test, never the real one.
 */
class LlmSettingsTest {
    private static final String ENVIRONMENT_KEY = "sk-ant-from-the-environment";
    private static final String FILE_KEY = "sk-ant-from-the-file";
    private static final Map<String, String> NO_ENVIRONMENT = Map.of();

    @TempDir
    Path directory;

    @Test
    void withNothingSetTheDefaultsApplyAndThereIsNoKey() {
        LlmSettings settings = LlmSettings.load(NO_ENVIRONMENT, this.directory.resolve("missing.properties"));
        assertFalse(settings.isConfigured());
        assertEquals("", settings.getApiKey());
        assertEquals("claude-haiku-5-5", settings.getModel());
        assertEquals("https://api.anthropic.com", settings.getBaseUrl());
        assertEquals(Duration.ofSeconds(12), settings.getTimeout());
    }

    @Test
    void noFileAtAllIsAllowed() {
        LlmSettings settings = LlmSettings.load(Map.of("ANTHROPIC_API_KEY", ENVIRONMENT_KEY), null);
        assertTrue(settings.isConfigured());
        assertEquals(ENVIRONMENT_KEY, settings.getApiKey());
        assertEquals("claude-haiku-5-5", settings.getModel());
    }

    @Test
    void theKeyIsTakenFromTheEnvironment() throws IOException {
        LlmSettings settings = LlmSettings.load(Map.of("ANTHROPIC_API_KEY", ENVIRONMENT_KEY, "HOME", "/home/me"),
                file("model=claude-sonnet-5-5"));
        assertTrue(settings.isConfigured());
        assertEquals(ENVIRONMENT_KEY, settings.getApiKey());
        assertEquals("claude-sonnet-5-5", settings.getModel(), "the file still supplies the other settings");
    }

    @Test
    void theEnvironmentWinsOverTheFile() throws IOException {
        LlmSettings settings = LlmSettings.load(Map.of("ANTHROPIC_API_KEY", ENVIRONMENT_KEY),
                file("api_key=" + FILE_KEY));
        assertEquals(ENVIRONMENT_KEY, settings.getApiKey());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n", "has a space", "caf\u00e9-key", "line\nbreak"})
    void theFileSuppliesTheKeyWhenTheEnvironmentHasNoUsableOne(String inEnvironment) throws IOException {
        LlmSettings settings = LlmSettings.load(Map.of("ANTHROPIC_API_KEY", inEnvironment),
                file("api_key=" + FILE_KEY));
        assertEquals(FILE_KEY, settings.getApiKey());
    }

    @Test
    void theFileSuppliesTheKeyWhenTheVariableIsNotSet() throws IOException {
        LlmSettings settings = LlmSettings.load(NO_ENVIRONMENT, file("# my key", "api_key = " + FILE_KEY + "  "));
        assertTrue(settings.isConfigured());
        assertEquals(FILE_KEY, settings.getApiKey(), "white space around the key is dropped");
    }

    @Test
    void whiteSpaceAroundAKeyFromTheEnvironmentIsDropped() {
        LlmSettings settings = LlmSettings.load(Map.of("ANTHROPIC_API_KEY", "  " + ENVIRONMENT_KEY + "\n"), null);
        assertEquals(ENVIRONMENT_KEY, settings.getApiKey());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"sk-ant-from-the-file\"", "'sk-ant-from-the-file'", " \" sk-ant-from-the-file \" "})
    void quotationMarksAroundAKeyAreDropped(String written) {
        assertEquals(FILE_KEY, new LlmSettings(written, null, null, 0).getApiKey());
    }

    @Test
    void aQuotationMarkWithoutItsPartnerIsLeftAlone() {
        assertEquals("'sk-ant-from-the-file", new LlmSettings("'sk-ant-from-the-file", null, null, 0).getApiKey());
        assertEquals("sk-ant-from-the-file\"", new LlmSettings("sk-ant-from-the-file\"", null, null, 0).getApiKey());
        assertEquals("\"sk-ant-from-the-file'", new LlmSettings("\"sk-ant-from-the-file'", null, null, 0).getApiKey());
    }

    @Test
    void aFileThatStartsWithAByteOrderMarkIsReadLikeAnyOther() throws IOException {
        Path file = this.directory.resolve("llm.properties");
        Files.writeString(file, "\uFEFFapi_key=" + FILE_KEY + "\r\nmodel=claude-opus-5-5\r\n");
        LlmSettings settings = LlmSettings.load(NO_ENVIRONMENT, file);
        assertEquals(FILE_KEY, settings.getApiKey());
        assertEquals("claude-opus-5-5", settings.getModel());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "two words", "tab\tinside", "caf\u00e9", "bell\u0007", "emoji\ud83d\ude00",
        "\"\"", "' '"})
    void aKeyThatCannotBeAKeyCountsAsNoKey(String key) {
        LlmSettings settings = new LlmSettings(key, null, null, 0);
        assertFalse(settings.isConfigured());
        assertEquals("", settings.getApiKey());
    }

    @Test
    void theFileCanChangeModelAndTimeoutButCannotRedirectCredentials() throws IOException {
        LlmSettings settings = LlmSettings.load(NO_ENVIRONMENT, file(
                "api_key=" + FILE_KEY,
                "model = claude-opus-5-5 ",
                "base_url=http://localhost:8080/anthropic/",
                "timeout_seconds=30"));
        assertEquals("claude-opus-5-5", settings.getModel());
        assertEquals("https://api.anthropic.com", settings.getBaseUrl());
        assertEquals(Duration.ofSeconds(30), settings.getTimeout());
    }

    @Test
    void blankEntriesLeaveTheDefaults() throws IOException {
        LlmSettings settings = LlmSettings.load(NO_ENVIRONMENT, file(
                "api_key=", "model=", "base_url=   ", "timeout_seconds="));
        assertFalse(settings.isConfigured());
        assertEquals("claude-haiku-5-5", settings.getModel());
        assertEquals("https://api.anthropic.com", settings.getBaseUrl());
        assertEquals(Duration.ofSeconds(12), settings.getTimeout());
    }

    @Test
    void anAddressOfSlashesOnlyLeavesTheDefault() {
        assertEquals("https://api.anthropic.com", new LlmSettings(null, null, "///", 0).getBaseUrl());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-5", "1.5", "12s", "99999999999"})
    void aTimeThatIsNotAPositiveWholeNumberLeavesTheDefault(String seconds) throws IOException {
        LlmSettings settings = LlmSettings.load(NO_ENVIRONMENT, file("timeout_seconds=" + seconds));
        assertEquals(Duration.ofSeconds(12), settings.getTimeout());
    }

    @Test
    void aTimeMayHaveWhiteSpaceAroundIt() throws IOException {
        LlmSettings settings = LlmSettings.load(NO_ENVIRONMENT, file("timeout_seconds =  4  "));
        assertEquals(Duration.ofSeconds(4), settings.getTimeout());
    }

    @Test
    void aDirectoryInPlaceOfTheFileCountsAsNoFile() {
        LlmSettings settings = LlmSettings.load(NO_ENVIRONMENT, this.directory);
        assertFalse(settings.isConfigured());
        assertEquals("claude-haiku-5-5", settings.getModel());
    }

    @Test
    void aFileThatCannotBeReadCountsAsNoFile() throws IOException {
        // A broken escape makes the whole file unreadable; nothing from it is used, not even the lines before.
        LlmSettings broken = LlmSettings.load(NO_ENVIRONMENT, file(
                "api_key=" + FILE_KEY, "model=claude-opus-5-5", "base_url=\\uZZZZ"));
        assertFalse(broken.isConfigured());
        assertEquals("claude-haiku-5-5", broken.getModel());
        assertEquals("https://api.anthropic.com", broken.getBaseUrl());

        Path notText = this.directory.resolve("binary.properties");
        Files.write(notText, new byte[] {(byte) 0xC3, (byte) 0x28, 'a', '=', 'b'});
        assertFalse(LlmSettings.load(NO_ENVIRONMENT, notText).isConfigured());
    }

    @Test
    void readingTheSettingsLeavesTheFileAsItWas() throws IOException {
        Path file = file("api_key=" + FILE_KEY, "model=claude-opus-5-5");
        byte[] before = Files.readAllBytes(file);
        LlmSettings.load(NO_ENVIRONMENT, file);
        assertEquals(new String(before, StandardCharsets.UTF_8), Files.readString(file));
        try (Stream<Path> files = Files.list(this.directory)) {
            assertEquals(1, files.count(), "no other file may appear next to the settings");
        }
    }

    @Test
    void theDescriptionOfTheSettingsLeavesTheKeyOut() {
        LlmSettings withKey = new LlmSettings(FILE_KEY, "claude-haiku-5-5", "https://api.anthropic.com", 12);
        assertFalse(withKey.toString().contains(FILE_KEY));
        assertTrue(withKey.toString().contains("apiKey=set"), withKey.toString());
        assertTrue(withKey.toString().contains("claude-haiku-5-5"), withKey.toString());

        LlmSettings withoutKey = new LlmSettings(null, null, null, 0);
        assertTrue(withoutKey.toString().contains("apiKey=not set"), withoutKey.toString());
    }

    @Test
    void theEnvironmentMustBeGiven() {
        assertThrows(IllegalArgumentException.class, () -> LlmSettings.load(null, null));
    }

    private Path file(String... lines) throws IOException {
        Path file = this.directory.resolve("llm.properties");
        Files.writeString(file, String.join("\n", lines) + "\n");
        return file;
    }
}
