package ma.aui.darija.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ma.aui.darija.config.AppConfig;
import ma.aui.darija.model.HealthStatus;

/**
 * Public (no login) health check: {@code GET /api/health}.
 * Clients use it to test the server URL before sending credentials.
 */
@Path("/health")
@RequestScoped
public class HealthResource {

    @Inject
    AppConfig config;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public HealthStatus health() {
        return new HealthStatus("UP", config.geminiModel(), config.isGeminiConfigured());
    }
}
