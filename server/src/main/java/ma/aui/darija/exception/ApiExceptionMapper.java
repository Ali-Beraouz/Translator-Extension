package ma.aui.darija.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ma.aui.darija.model.ErrorResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Turns an {@link ApiException} into a JSON error response with the right HTTP status. */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(ApiException e) {
        int status = e.getStatus();
        if (status >= 500) {
            LOG.log(Level.WARNING, "Translation failed: " + e.getMessage(), e.getCause());
        }
        Response.StatusType statusType = Response.Status.fromStatusCode(status);
        String reason = statusType != null ? statusType.getReasonPhrase() : "Error";

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(status, reason, e.getMessage()))
                .build();
    }
}
