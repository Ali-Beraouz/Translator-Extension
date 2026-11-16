package ma.aui.darija.security;

import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import ma.aui.darija.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses the default demo login (ali / darija2026) unless overridden by environment variables. */
class TranslatorIdentityStoreTest {

    private TranslatorIdentityStore store;
    private AppConfig config;

    @BeforeEach
    void setUp() {
        config = new AppConfig();
        store = new TranslatorIdentityStore();
        store.config = config;
    }

    @Test
    void acceptsConfiguredCredentials() {
        CredentialValidationResult result =
                store.validate(new UsernamePasswordCredential(config.username(), config.password()));
        assertEquals(CredentialValidationResult.Status.VALID, result.getStatus());
        assertTrue(result.getCallerGroups().contains(TranslatorIdentityStore.ROLE));
    }

    @Test
    void rejectsWrongPassword() {
        CredentialValidationResult result =
                store.validate(new UsernamePasswordCredential(config.username(), "wrong-password"));
        assertEquals(CredentialValidationResult.Status.INVALID, result.getStatus());
    }

    @Test
    void rejectsWrongUser() {
        CredentialValidationResult result =
                store.validate(new UsernamePasswordCredential("someone-else", config.password()));
        assertEquals(CredentialValidationResult.Status.INVALID, result.getStatus());
    }
}
