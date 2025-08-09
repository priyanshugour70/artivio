package com.klakar.artivio.service.impl;

import com.klakar.artivio.service.AIContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;

@Service
@Slf4j
public class FallbackAIContextServiceImpl implements AIContextService {

    // In-memory storage for conversation context (fallback)
    private final Map<String, ConcurrentLinkedQueue<String>> conversationContexts = new ConcurrentHashMap<>();
    private final int MAX_CONTEXT_SIZE = 10; // Keep last 10 messages per conversation

    public FallbackAIContextServiceImpl() {
        log.info("Initialized FallbackAIContextService with in-memory storage");
    }

    @Override
    public void storeConversationContext(String conversationId, String messageContent, String sender) {
        try {
            String contextEntry = String.format("[%s]: %s", sender, messageContent);

            conversationContexts.computeIfAbsent(conversationId, k -> new ConcurrentLinkedQueue<>())
                    .offer(contextEntry);

            // Keep only recent messages
            ConcurrentLinkedQueue<String> queue = conversationContexts.get(conversationId);
            while (queue.size() > MAX_CONTEXT_SIZE) {
                queue.poll();
            }

            log.debug("Stored context for conversation {}: {} characters from {}",
                    conversationId, messageContent.length(), sender);
        } catch (Exception e) {
            log.error("Error storing context for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    @Override
    public String getRelevantContext(String conversationId, String query, int maxResults) {
        try {
            ConcurrentLinkedQueue<String> context = conversationContexts.get(conversationId);

            if (context == null || context.isEmpty()) {
                return "";
            }

            // Return recent context (simple implementation)
            StringBuilder contextBuilder = new StringBuilder();
            int count = 0;

            for (String entry : context) {
                if (count >= maxResults) break;
                contextBuilder.append(entry).append("\n");
                count++;
            }

            String result = contextBuilder.toString().trim();
            log.debug("Retrieved {} context entries for conversation {}", count, conversationId);

            return result;
        } catch (Exception e) {
            log.error("Error getting context for conversation {}: {}", conversationId, e.getMessage());
            return "";
        }
    }

    @Override
    public void clearConversationContext(String conversationId) {
        try {
            conversationContexts.remove(conversationId);
            log.info("Cleared context for conversation {}", conversationId);
        } catch (Exception e) {
            log.error("Error clearing context for conversation {}: {}", conversationId, e.getMessage());
        }
    }
}