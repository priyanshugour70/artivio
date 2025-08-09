package com.klakar.artivio.config;

import com.klakar.artivio.service.AIContextService;
import com.klakar.artivio.service.impl.FallbackAIContextServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class AIConfig {

    @Value("${app.chroma.enabled:false}")
    private boolean chromaEnabled;

    @Value("${app.chroma.url:http://localhost:8000}")
    private String chromaUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024));
    }

    @Bean
    public AIContextService aiContextService() {
        if (chromaEnabled) {
            log.info("ChromaDB is enabled, but VectorStore configuration is manual");
            // For now, use fallback until ChromaDB is properly configured
            return new FallbackAIContextServiceImpl();
        } else {
            log.info("ChromaDB is disabled, using fallback AIContextService");
            return new FallbackAIContextServiceImpl();
        }
    }
}