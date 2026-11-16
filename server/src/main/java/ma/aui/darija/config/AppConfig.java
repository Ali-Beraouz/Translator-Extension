package ma.aui.darija.config;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Central place for runtime configuration.
 * <p>
 * Every value is read from an environment variable first, then from a JVM system
 * property with the same name (e.g. {@code -DGEMINI_API_KEY=...}), then falls back
 * to a default. Nothing secret is hard-coded except the demo login, which can be
 * overridden with TRANSLATOR_USERNAME / TRANSLATOR_PASSWORD.
 */
@ApplicationScoped
public class AppConfig {

    public static final String DEFAULT_GEMINI_MODEL = "gemini-3.8-flash";
    public static final String DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    public String geminiApiKey() {
        return read("GEMINI_API_KEY", "");
    }

    public String geminiModel() {
        return read("GEMINI_MODEL", DEFAULT_GEMINI_MODEL);
    }

    /** Base URL of the Gemini REST API (overridable so the service can be tested against a mock). */
    public String geminiBaseUrl() {
        String url = read("GEMINI_BASE_URL", DEFAULT_GEMINI_BASE_URL);
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public int geminiTimeoutSeconds() {
        return readInt("GEMINI_TIMEOUT_SECONDS", 60);
    }

    public String username() {
        return read("TRANSLATOR_USERNAME", "ali");
    }

    public String password() {
        return read("TRANSLATOR_PASSWORD", "darija2026");
    }

    /** Longest input accepted by the translate() endpoint, in characters. */
    public int maxTextLength() {
        return readInt("TRANSLATOR_MAX_CHARS", 5000);
    }

    /** Value of the Access-Control-Allow-Origin header (for browser-based clients). */
    public String corsAllowedOrigin() {
        return read("CORS_ALLOWED_ORIGIN", "*");
    }

    public boolean isGeminiConfigured() {
        return !geminiApiKey().isBlank();
    }

    private static String read(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private static int readInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(read(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
