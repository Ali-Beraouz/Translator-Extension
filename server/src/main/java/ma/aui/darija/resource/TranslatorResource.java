package ma.aui.darija.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import ma.aui.darija.config.AppConfig;
import ma.aui.darija.exception.ApiException;
import ma.aui.darija.model.DarijaScript;
import ma.aui.darija.model.TranslationRequest;
import ma.aui.darija.model.TranslationResponse;
import ma.aui.darija.model.TranslationResult;
import ma.aui.darija.service.TranslationService;

/**
 * TranslatorResource: the RESTful endpoint of the project.
 * <p>
 * Protected by HTTP Basic authentication (see web.xml); the caller needs the
 * {@code translator-user} role.
 *
 * <ul>
 *   <li>{@code POST /api/translator/translate} with JSON {@code {"text":"...", "script":"arabic|latin"}}</li>
 *   <li>{@code GET  /api/translator/translate?text=...&script=arabic|latin} (handy for a browser or cURL)</li>
 * </ul>
 */
@Path("/translator")
@RequestScoped
public class TranslatorResource {

    @Inject
    TranslationService translationService;

    @Inject
    AppConfig config;

    /** Business method: translates the text in the JSON body to Darija. */
    @POST
    @Path("/translate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationResponse translate(TranslationRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Send a JSON body like {\"text\": \"Hello\"}");
        }
        return translate(request.text(), request.script());
    }

    /** Same business method, with the text passed as a query parameter. */
    @GET
    @Path("/translate")
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationResponse translate(@QueryParam("text") String text,
                                         @QueryParam("script") String script) {
        DarijaScript darijaScript = DarijaScript.parse(script);
        if (darijaScript == null) {
            throw ApiException.badRequest("Field 'script' must be \"arabic\" or \"latin\"");
        }
        if (text == null || text.isBlank()) {
            throw ApiException.badRequest("Field 'text' is required and cannot be empty");
        }
        String cleaned = text.strip();
        int max = config.maxTextLength();
        if (cleaned.length() > max) {
            throw ApiException.badRequest("Text is too long: " + cleaned.length()
                    + " characters (maximum is " + max + ")");
        }

        long start = System.nanoTime();
        TranslationResult result = translationService.translate(cleaned, darijaScript);
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        return new TranslationResponse(
                cleaned,
                result.detectedLanguage(),
                result.translation(),
                darijaScript.label(),
                translationService.modelName(),
                durationMs);
    }
}
