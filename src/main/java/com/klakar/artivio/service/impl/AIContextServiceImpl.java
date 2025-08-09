package com.klakar.artivio.service.impl;

import com.klakar.artivio.service.AIContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnBean(VectorStore.class)
public class AIContextServiceImpl implements AIContextService {

    private final VectorStore vectorStore;

    public AIContextServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        log.info("AIContextService initialized with VectorStore: {}", vectorStore.getClass().getSimpleName());
    }

    @Override
    public void storeConversationContext(String conversationId, String messageContent, String sender) {
        if (vectorStore == null) {
            log.warn("Vector store not available, skipping context storage");
            return;
        }

        try {
            // Create metadata for the document
            Map<String, Object> metadata = Map.of(
                    "conversationId", conversationId,
                    "sender", sender,
                    "timestamp", String.valueOf(System.currentTimeMillis()),
                    "messageLength", String.valueOf(messageContent.length())
            );

            Document document = new Document(messageContent, metadata);

            vectorStore.add(List.of(document));
            log.debug("Stored context for conversation {}: {} characters from {}",
                    conversationId, messageContent.length(), sender);

        } catch (Exception e) {
            log.error("Failed to store context for conversation {}: {}", conversationId, e.getMessage(), e);
        }
    }

    @Override
    public String getRelevantContext(String conversationId, String query, int maxResults) {
        if (vectorStore == null) {
            log.warn("Vector store not available, returning empty context");
            return "";
        }

        try {
            // Create search request
            SearchRequest searchRequest = SearchRequest.defaults()
                    .withQuery(query)
                    .withTopK(Math.min(maxResults, 10))
                    .withSimilarityThreshold(0.6);


            List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);

            // Filter by conversation ID and extract content
            String context = relevantDocs.stream()
                    .filter(doc -> {
                        Map<String, Object> metadata = doc.getMetadata();
                        return metadata != null && conversationId.equals(metadata.get("conversationId"));
                    })
                    .map(Document::getContent)
                    .limit(maxResults)
                    .collect(Collectors.joining("\n\n"));

            log.debug("Retrieved {} relevant documents for conversation {}",
                    relevantDocs.size(), conversationId);

            return context;

        } catch (Exception e) {
            log.error("Failed to retrieve context for conversation {}: {}", conversationId, e.getMessage(), e);
            return "";
        }
    }

    @Override
    public void clearConversationContext(String conversationId) {
        if (vectorStore == null) {
            log.warn("Vector store not available, cannot clear context");
            return;
        }

        try {
            // Note: ChromaDB doesn't have a direct delete by metadata method in Spring AI
            // This would need to be implemented differently based on the vector store capabilities
            log.info("Context clearing requested for conversation {}", conversationId);
            // Implementation would depend on the specific vector store capabilities
        } catch (Exception e) {
            log.error("Failed to clear context for conversation {}: {}", conversationId, e.getMessage(), e);
        }
    }
}