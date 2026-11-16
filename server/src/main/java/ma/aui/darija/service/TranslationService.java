package ma.aui.darija.service;

import ma.aui.darija.model.DarijaScript;
import ma.aui.darija.model.TranslationResult;

/**
 * Translates text from any language into Moroccan Arabic (Darija).
 * The REST layer depends only on this interface, so the LLM provider can be swapped
 * (e.g. Gemini today, a local model later) without touching the endpoint.
 */
public interface TranslationService {

    /**
     * @param text   text to translate (already validated: not blank, within size limit)
     * @param script writing system for the Darija output
     * @return detected source language and the Darija translation
     * @throws ma.aui.darija.exception.ApiException if the LLM call fails
     */
    TranslationResult translate(String text, DarijaScript script);

    /** Name of the model that performs the translation (shown in responses). */
    String modelName();
}
