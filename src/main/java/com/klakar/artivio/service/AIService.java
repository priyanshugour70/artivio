package com.klakar.artivio.service;

import java.util.concurrent.CompletableFuture;

public interface AIService {
    CompletableFuture<String> generateResponse(String prompt, String context);
    CompletableFuture<String> generateResponse(String prompt);
    boolean isConfigured();
    String getModelName();
}