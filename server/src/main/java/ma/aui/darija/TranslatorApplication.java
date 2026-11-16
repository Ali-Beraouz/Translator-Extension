package ma.aui.darija;

import jakarta.annotation.security.DeclareRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import ma.aui.darija.security.TranslatorIdentityStore;

/**
 * JAX-RS application: every resource lives under {@code /api}.
 * <p>
 * Security: {@code @BasicAuthenticationMechanismDefinition} enables HTTP Basic authentication
 * through Jakarta Security, which plugs into the container via the Jakarta Authentication SPI
 * (a ServerAuthModule bridge). Credentials are checked by {@link TranslatorIdentityStore};
 * which URLs need a login is declared in {@code WEB-INF/web.xml}.
 * <p>
 * Full URL of the endpoint: {@code http://localhost:8080/darija/api/translator/translate}
 */
@ApplicationPath("/api")
@ApplicationScoped
@BasicAuthenticationMechanismDefinition(realmName = "darija-translator")
@DeclareRoles({TranslatorIdentityStore.ROLE})
public class TranslatorApplication extends Application {
}
