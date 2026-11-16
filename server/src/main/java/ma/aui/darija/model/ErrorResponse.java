package ma.aui.darija.model;

/**
 * Uniform JSON error body, e.g.
 * <pre>{"status": 400, "error": "Bad Request", "message": "Field 'text' is required"}</pre>
 */
public record ErrorResponse(int status, String error, String message) {
}
