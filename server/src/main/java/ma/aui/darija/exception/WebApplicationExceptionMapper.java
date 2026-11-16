package ma.aui.darija.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ma.aui.darija.model.ErrorResponse;

/**
 * Gives standard JAX-RS errors (404 unknown path, 405 wrong method, 415 wrong content type,
 * 400 malformed JSON...) the same JSON shape as our own errors.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException e) {
        Response original = e.getResponse();
        int status = original.getStatus();
        String reason = original.getStatusInfo().getReasonPhrase();
        String message = e.getMessage() != null ? e.getMessage() : reason;

        Response.ResponseBuilder builder = Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(status, reason, message));
        // keep headers such as "Allow" on 405 responses
        original.getHeaders().forEach((name, values) -> {
            if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                values.forEach(v -> builder.header(name, v));
            }
        });
        return builder.build();
    }
}
