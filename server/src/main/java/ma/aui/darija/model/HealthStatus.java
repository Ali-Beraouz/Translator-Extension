package ma.aui.darija.model;

/**
 * Body of the public {@code GET /api/health} endpoint.
 *
 * @param status           always "UP" when the service answers
 * @param model            configured Gemini model
 * @param geminiConfigured whether GEMINI_API_KEY is set on the server
 */
public record HealthStatus(String status, String model, boolean geminiConfigured) {
}
