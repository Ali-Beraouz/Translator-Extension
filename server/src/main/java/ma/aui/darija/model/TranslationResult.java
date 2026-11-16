package ma.aui.darija.model;

/**
 * What the LLM layer returns to the REST layer.
 *
 * @param detectedLanguage source language detected by the model
 * @param translation      the Darija text
 */
public record TranslationResult(String detectedLanguage, String translation) {
}
