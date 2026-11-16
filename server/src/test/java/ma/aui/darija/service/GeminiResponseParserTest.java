package ma.aui.darija.service;

import ma.aui.darija.exception.ApiException;
import ma.aui.darija.model.TranslationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiResponseParserTest {

    private static String candidate(String escapedModelText) {
        return """
                {"candidates":[{"content":{"role":"model","parts":[{"text":"%s"}]},"finishReason":"STOP"}]}
                """.formatted(escapedModelText);
    }

    @Test
    void parsesStructuredJsonAnswer() {
        String body = candidate("{\\\"detectedLanguage\\\":\\\"English\\\",\\\"translation\\\":\\\"كيداير؟\\\"}");
        TranslationResult result = GeminiResponseParser.parse(body);
        assertEquals("English", result.detectedLanguage());
        assertEquals("كيداير؟", result.translation());
    }

    @Test
    void skipsThoughtParts() {
        String body = """
                {"candidates":[{"content":{"parts":[
                  {"text":"thinking about it...","thought":true},
                  {"text":"{\\"detectedLanguage\\":\\"French\\",\\"translation\\":\\"شكرا بزاف\\"}"}
                ]},"finishReason":"STOP"}]}
                """;
        TranslationResult result = GeminiResponseParser.parse(body);
        assertEquals("French", result.detectedLanguage());
        assertEquals("شكرا بزاف", result.translation());
    }

    @Test
    void fallsBackToPlainTextWhenModelIgnoresJsonFormat() {
        TranslationResult result = GeminiResponseParser.parse(candidate("صباح الخير"));
        assertEquals("unknown", result.detectedLanguage());
        assertEquals("صباح الخير", result.translation());
    }

    @Test
    void stripsMarkdownCodeFence() {
        TranslationResult result = GeminiResponseParser.parseModelText(
                "```json\n{\"detectedLanguage\":\"English\",\"translation\":\"labas\"}\n```");
        assertEquals("labas", result.translation());
    }

    @Test
    void blockedPromptIsReportedAs422() {
        ApiException e = assertThrows(ApiException.class,
                () -> GeminiResponseParser.parse("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"));
        assertEquals(422, e.getStatus());
    }

    @Test
    void noCandidatesIsReportedAs502() {
        ApiException e = assertThrows(ApiException.class, () -> GeminiResponseParser.parse("{\"candidates\":[]}"));
        assertEquals(502, e.getStatus());
    }

    @Test
    void invalidJsonIsReportedAs502() {
        ApiException e = assertThrows(ApiException.class, () -> GeminiResponseParser.parse("<html>oops</html>"));
        assertEquals(502, e.getStatus());
    }

    @Test
    void readsGeminiErrorMessage() {
        assertEquals("API key not valid",
                GeminiResponseParser.errorMessage("{\"error\":{\"code\":400,\"message\":\"API key not valid\"}}"));
        assertEquals("", GeminiResponseParser.errorMessage("not json"));
    }
}
