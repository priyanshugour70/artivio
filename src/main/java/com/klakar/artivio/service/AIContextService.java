package com.klakar.artivio.service;

public interface AIContextService {
    void storeConversationContext(String conversationId, String messageContent, String sender);
    String getRelevantContext(String conversationId, String query, int maxResults);
    void clearConversationContext(String conversationId);
}