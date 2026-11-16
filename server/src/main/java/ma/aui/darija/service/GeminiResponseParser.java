package ma.aui.darija.service;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import ma.aui.darija.exception.ApiException;
import ma.aui.darija.model.TranslationResult;

import java.io.StringReader;

/**
 * Extracts the translation from a Gemini {@code generateContent} response.
 * <p>
 * Expected shape (abridged):
 * <pre>
 * {"candidates":[{"content":{"parts":[{"text":"{\"detectedLanguage\":\"English\",\"translation\":\"...\"}"}]},
 *                 "finishReason":"STOP"}]}
 * </pre>
 * If the model ignores the JSON format, the whole text is used as the translation.
 */
public final class GeminiResponseParser {

    private GeminiResponseParser() {
    }

    public static TranslationResult parse(String responseBody) {
        JsonObject root = readObject(responseBody);
        if (root == null) {
            throw new ApiException(502, "Gemini returned a response that is not valid JSON");
        }

        JsonObject feedback = root.getJsonObject("promptFeedback");
        if (feedback != null && feedback.containsKey("blockReason")) {
            throw new ApiException(422, "Gemini refused to translate this text (reason: "
                    + feedback.getString("blockReason", "unknown") + ")");
        }

        JsonArray candidates = root.getJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new ApiException(502, "Gemini returned no translation");
        }
        JsonObject candidate = candidates.getJsonObject(0);
        String text = joinTextParts(candidate);

        if (text.isBlank()) {
            String reason = candidate.getString("finishReason", "UNKNOWN");
            if ("SAFETY".equals(reason) || "PROHIBITED_CONTENT".equals(reason)) {
                throw new ApiException(422, "Gemini refused to translate this text (reason: " + reason + ")");
            }
            throw new ApiException(502, "Gemini returned an empty translation (finishReason: " + reason + ")");
        }
        return parseModelText(text);
    }

    /** Parses the model's own output: JSON {detectedLanguage, translation} or, as a fallback, plain text. */
    static TranslationResult parseModelText(String text) {
        String cleaned = stripCodeFence(text.trim());
        JsonObject json = readObject(cleaned);
        if (json != null && json.get("translation") instanceof JsonValue v && v.getValueType() == JsonValue.ValueType.STRING) {
            String translation = json.getString("translation").trim();
            String language = json.getString("detectedLanguage", "unknown").trim();
            if (!translation.isEmpty()) {
                return new TranslationResult(language.isEmpty() ? "unknown" : language, translation);
            }
        }
        return new TranslationResult("unknown", cleaned);
    }

    /** Concatenates the text parts of a candidate, skipping "thought" parts of thinking models. */
    private static String joinTextParts(JsonObject candidate) {
        JsonObject content = candidate.getJsonObject("content");
        if (content == null || content.getJsonArray("parts") == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonValue partValue : content.getJsonArray("parts")) {
            if (partValue.getValueType() != JsonValue.ValueType.OBJECT) {
                continue;
            }
            JsonObject part = partValue.asJsonObject();
            if (part.getBoolean("thought", false)) {
                continue;
            }
            sb.append(part.getString("text", ""));
        }
        return sb.toString();
    }

    /** Reads the "error.message" field of a Gemini error response, if present. */
    public static String errorMessage(String responseBody) {
        JsonObject root = readObject(responseBody);
        if (root != null && root.get("error") instanceof JsonObject error) {
            return error.getString("message", "");
        }
        return "";
    }

    private static String stripCodeFence(String text) {
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private static JsonObject readObject(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonValue value = reader.readValue();
            return value.getValueType() == JsonValue.ValueType.OBJECT ? value.asJsonObject() : null;
        } catch (JsonException | IllegalStateException e) {
            return null;
        }
    }
}
