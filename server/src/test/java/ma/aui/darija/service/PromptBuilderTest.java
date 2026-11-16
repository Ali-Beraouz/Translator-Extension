package ma.aui.darija.service;

import jakarta.json.JsonObject;
import ma.aui.darija.model.DarijaScript;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    @Test
    void requestBodyWrapsTextAndAsksForJson() {
        JsonObject body = PromptBuilder.requestBody("Good morning", DarijaScript.ARABIC);

        String userText = body.getJsonArray("contents").getJsonObject(0)
                .getJsonArray("parts").getJsonObject(0).getString("text");
        assertEquals("<text>\nGood morning\n</text>", userText);

        JsonObject generation = body.getJsonObject("generationConfig");
        assertEquals("application/json", generation.getString("responseMimeType"));
        assertTrue(generation.getJsonObject("responseSchema").getJsonObject("properties").containsKey("translation"));
    }

    @Test
    void systemInstructionDependsOnScript() {
        assertTrue(PromptBuilder.systemInstruction(DarijaScript.ARABIC).contains("Arabic script"));
        assertTrue(PromptBuilder.systemInstruction(DarijaScript.LATIN).contains("Latin letters"));
    }
}
