package ma.aui.darija.model;

/**
 * JSON body of {@code POST /api/translator/translate}.
 *
 * <pre>{"text": "How are you?", "script": "arabic"}</pre>
 *
 * @param text   text to translate, in any language (required)
 * @param script "arabic" (default) or "latin"
 */
public record TranslationRequest(String text, String script) {
}
