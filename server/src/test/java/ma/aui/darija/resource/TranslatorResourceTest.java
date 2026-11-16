package ma.aui.darija.resource;

import ma.aui.darija.config.AppConfig;
import ma.aui.darija.exception.ApiException;
import ma.aui.darija.model.DarijaScript;
import ma.aui.darija.model.TranslationRequest;
import ma.aui.darija.model.TranslationResponse;
import ma.aui.darija.model.TranslationResult;
import ma.aui.darija.service.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the endpoint logic with a fake LLM (no network, no container). */
class TranslatorResourceTest {

    private TranslatorResource resource;
    private DarijaScript lastScript;

    @BeforeEach
    void setUp() {
        resource = new TranslatorResource();
        resource.config = new AppConfig();
        resource.translationService = new TranslationService() {
            @Override
            public TranslationResult translate(String text, DarijaScript script) {
                lastScript = script;
                return new TranslationResult("English", "[darija] " + text);
            }

            @Override
            public String modelName() {
                return "fake-model";
            }
        };
    }

    @Test
    void translatesJsonRequest() {
        TranslationResponse response = resource.translate(new TranslationRequest("  Hello  ", null));
        assertEquals("Hello", response.sourceText());
        assertEquals("[darija] Hello", response.translation());
        assertEquals("English", response.detectedLanguage());
        assertEquals("arabic", response.script());
        assertEquals("fake-model", response.model());
        assertEquals(DarijaScript.ARABIC, lastScript);
    }

    @Test
    void supportsLatinScript() {
        TranslationResponse response = resource.translate("Hello", "LATIN");
        assertEquals("latin", response.script());
        assertEquals(DarijaScript.LATIN, lastScript);
    }

    @Test
    void rejectsMissingBody() {
        ApiException e = assertThrows(ApiException.class, () -> resource.translate((TranslationRequest) null));
        assertEquals(400, e.getStatus());
    }

    @Test
    void rejectsBlankText() {
        ApiException e = assertThrows(ApiException.class, () -> resource.translate(new TranslationRequest("   ", "arabic")));
        assertEquals(400, e.getStatus());
    }

    @Test
    void rejectsUnknownScript() {
        ApiException e = assertThrows(ApiException.class, () -> resource.translate("Hello", "klingon"));
        assertEquals(400, e.getStatus());
    }

    @Test
    void rejectsTooLongText() {
        String tooLong = "a".repeat(new AppConfig().maxTextLength() + 1);
        ApiException e = assertThrows(ApiException.class, () -> resource.translate(tooLong, null));
        assertEquals(400, e.getStatus());
    }
}
