package com.klakar.artivio.controller;

import com.klakar.artivio.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final AIService aiService;

    @Value("${app.chroma.enabled:false}")
    private boolean chromaEnabled;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "Artivio",
                "description", "Where creativity meets intelligence",
                "timestamp", LocalDateTime.now(),
                "version", "1.0.0",
                "ai_configured", aiService.isConfigured(),
                "ai_model", aiService.getModelName(),
                "chroma_enabled", chromaEnabled,
                "features", Map.of(
                        "authentication", "JWT",
                        "websockets", "Enabled",
                        "real_time_chat", "Enabled",
                        "ai_chat", aiService.isConfigured(),
                        "vector_storage", chromaEnabled ? "ChromaDB" : "In-Memory Fallback"
                )
        ));
    }

    @GetMapping("/ai-status")
    public ResponseEntity<Map<String, Object>> aiStatus() {
        return ResponseEntity.ok(Map.of(
                "configured", aiService.isConfigured(),
                "model", aiService.getModelName(),
                "provider", "Google Gemini",
                "context_storage", chromaEnabled ? "ChromaDB" : "In-Memory Fallback"
        ));
    }
}