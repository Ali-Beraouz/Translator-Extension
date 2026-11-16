package ma.aui.darija.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ma.aui.darija.config.AppConfig;
import ma.aui.darija.exception.ApiException;
import ma.aui.darija.model.DarijaScript;
import ma.aui.darija.model.TranslationResult;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * {@link TranslationService} backed by the Google Gemini REST API
 * ({@code POST {base}/models/{model}:generateContent}).
 * Uses the JDK {@link HttpClient}, so no extra library is needed.
 */
@ApplicationScoped
public class GeminiTranslationService implements TranslationService {

    private static final Logger LOG = Logger.getLogger(GeminiTranslationService.class.getName());

    @Inject
    AppConfig config;

    private HttpClient httpClient;

    @PostConstruct
    void init() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        if (!config.isGeminiConfigured()) {
            LOG.warning("GEMINI_API_KEY is not set: translate() will answer 503 until it is configured.");
        }
    }

    @Override
    public TranslationResult translate(String text, DarijaScript script) {
        String apiKey = config.geminiApiKey();
        if (apiKey.isBlank()) {
            throw new ApiException(503, "The server has no Gemini API key. Set the GEMINI_API_KEY environment variable and restart.");
        }

        String model = config.geminiModel();
        URI uri = URI.create(config.geminiBaseUrl() + "/models/"
                + URLEncoder.encode(model, StandardCharsets.UTF_8) + ":generateContent");

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(config.geminiTimeoutSeconds()))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                        PromptBuilder.requestBody(text, script).toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new ApiException(504, "Gemini did not answer in time, please try again", e);
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new ApiException(502, "Could not reach the Gemini API (" + reason + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(503, "Translation was interrupted", e);
        }

        int status = response.statusCode();
        if (status == 200) {
            return GeminiResponseParser.parse(response.body());
        }

        String detail = GeminiResponseParser.errorMessage(response.body());
        String suffix = detail.isBlank() ? "" : ": " + detail;
        boolean keyProblem = status == 401 || status == 403
                || (status == 400 && detail.toLowerCase(Locale.ROOT).contains("api key"));
        if (keyProblem) {
            throw new ApiException(502, "Gemini rejected the API key (check GEMINI_API_KEY)" + suffix);
        }
        throw switch (status) {
            case 400 -> new ApiException(502, "Gemini rejected the request" + suffix);
            case 404 -> new ApiException(502, "Gemini model '" + model + "' was not found (check GEMINI_MODEL)" + suffix);
            case 429 -> new ApiException(429, "Gemini free-tier rate limit reached, wait a minute and try again" + suffix);
            default -> new ApiException(502, "Gemini API error (HTTP " + status + ")" + suffix);
        };
    }

    @Override
    public String modelName() {
        return config.geminiModel();
    }
}
