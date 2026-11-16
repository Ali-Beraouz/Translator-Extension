package ma.aui.darija.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import ma.aui.darija.config.AppConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * Identity store that checks the username/password sent with HTTP Basic authentication.
 * <p>
 * The Basic authentication mechanism (declared on {@link ma.aui.darija.TranslatorApplication})
 * extracts the credentials from the {@code Authorization} header and hands them to this store.
 * A valid caller is placed in the {@value #ROLE} group, which web.xml requires for
 * {@code /api/translator/*}.
 */
@ApplicationScoped
public class TranslatorIdentityStore implements IdentityStore {

    /** Role (and group) required to call the translator endpoint. */
    public static final String ROLE = "translator-user";

    @Inject
    AppConfig config;

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (!(credential instanceof UsernamePasswordCredential login)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
        boolean userOk = constantTimeEquals(login.getCaller(), config.username());
        boolean passwordOk = constantTimeEquals(login.getPasswordAsString(), config.password());

        if (userOk && passwordOk) {
            return new CredentialValidationResult(login.getCaller(), Set.of(ROLE));
        }
        return CredentialValidationResult.INVALID_RESULT;
    }

    /** Compares secrets without leaking their length/content through timing. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
