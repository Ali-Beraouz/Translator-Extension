package ma.aui.darija.exception;

/**
 * Error raised anywhere in the service, carrying the HTTP status to send back.
 * Mapped to a JSON {@link ma.aui.darija.model.ErrorResponse} by {@link ApiExceptionMapper}.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }
}
