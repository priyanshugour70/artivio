package com.klakar.artivio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAIService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:demo}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String model;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";

    public CompletableFuture<String> generateResponse(String prompt, String context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callGeminiAPI(prompt, context);
            } catch (Exception e) {
                log.error("Error calling Gemini API", e);
                return "I'm having trouble generating a response right now. Please try again.";
            }
        });
    }

    private String callGeminiAPI(String prompt, String context) {
        try {
            WebClient webClient = webClientBuilder.build();

            // Build the full prompt with context
            String fullPrompt = buildFullPrompt(prompt, context);

            // Prepare the request body for Gemini API
            Map<String, Object> requestBody = Map.of(
                    "contents", new Object[]{
                            Map.of(
                                    "parts", new Object[]{
                                            Map.of("text", fullPrompt)
                                    }
                            )
                    },
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "topK", 32,
                            "topP", 1.0,
                            "maxOutputTokens", 500,
                            "stopSequences", new String[]{}
                    ),
                    "safetySettings", new Object[]{
                            Map.of(
                                    "category", "HARM_CATEGORY_HARASSMENT",
                                    "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                            ),
                            Map.of(
                                    "category", "HARM_CATEGORY_HATE_SPEECH",
                                    "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                            ),
                            Map.of(
                                    "category", "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                                    "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                            ),
                            Map.of(
                                    "category", "HARM_CATEGORY_DANGEROUS_CONTENT",
                                    "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                            )
                    }
            );

            String response = webClient.post()
                    .uri(GEMINI_API_URL, model)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractTextFromResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Gemini API error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return handleAPIError(e);
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini API", e);
            return "Sorry, I encountered an unexpected error. Please try again.";
        }
    }

    private String buildFullPrompt(String userMessage, String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are Artivio AI, a helpful and creative assistant integrated into the Artivio chat application. ");
        prompt.append("Artivio's tagline is 'Where creativity meets intelligence'. ");
        prompt.append("Be conversational, friendly, and provide helpful responses. ");
        prompt.append("Keep responses concise but informative. ");

        if (context != null && !context.trim().isEmpty()) {
            prompt.append("\n\nPrevious conversation context:\n").append(context).append("\n\n");
        }

        prompt.append("User message: ").append(userMessage);

        return prompt.toString();
    }

    private String extractTextFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.get("candidates");

            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");

                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        JsonNode text = firstPart.get("text");
                        if (text != null) {
                            return text.asText().trim();
                        }
                    }
                }
            }

            log.warn("Unexpected response format from Gemini API: {}", response);
            return "I received an unexpected response format. Please try again.";

        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return "I had trouble processing the response. Please try again.";
        }
    }

    private String handleAPIError(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        switch (statusCode) {
            case 400:
                log.error("Bad request to Gemini API: {}", responseBody);
                return "I received an invalid request. Please rephrase your message.";
            case 401:
                log.error("Unauthorized request to Gemini API - check API key");
                return "I'm having authentication issues. Please contact support.";
            case 403:
                log.error("Forbidden request to Gemini API: {}", responseBody);
                return "I don't have permission to process this request.";
            case 429:
                log.error("Rate limit exceeded for Gemini API");
                return "I'm getting too many requests right now. Please wait a moment and try again.";
            case 500:
            case 502:
            case 503:
                log.error("Gemini API server error: {}", responseBody);
                return "The AI service is temporarily unavailable. Please try again in a moment.";
            default:
                log.error("Unknown error from Gemini API: Status {}, Body: {}", statusCode, responseBody);
                return "I encountered an unexpected error. Please try again.";
        }
    }

    public boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.equals("demo") && !geminiApiKey.isEmpty();
    }
}