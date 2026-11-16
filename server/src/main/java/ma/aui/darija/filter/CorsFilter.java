package ma.aui.darija.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import ma.aui.darija.config.AppConfig;

/**
 * Adds CORS headers so browser-based clients (e.g. Expo web, a test page) can call the API.
 * The Chrome extension and the PHP/Python/mobile clients do not need CORS, but it is harmless.
 * OPTIONS pre-flight requests are excluded from authentication in web.xml.
 */
@Provider
@ApplicationScoped
public class CorsFilter implements ContainerResponseFilter {

    @Inject
    AppConfig config;

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        MultivaluedMap<String, Object> headers = response.getHeaders();
        headers.putSingle("Access-Control-Allow-Origin", config.corsAllowedOrigin());
        headers.putSingle("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.putSingle("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
        headers.putSingle("Access-Control-Max-Age", "3600");
    }
}
