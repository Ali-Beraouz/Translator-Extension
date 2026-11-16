package ma.aui.darija.model;

/**
 * JSON returned by the translate() endpoint.
 *
 * @param sourceText       the text that was translated
 * @param detectedLanguage source language as detected by the LLM (e.g. "English")
 * @param translation      the Moroccan Darija translation
 * @param script           "arabic" or "latin"
 * @param model            the Gemini model that produced the translation
 * @param durationMs       time spent on the translation, in milliseconds
 */
public record TranslationResponse(
        String sourceText,
        String detectedLanguage,
        String translation,
        String script,
        String model,
        long durationMs) {
}
