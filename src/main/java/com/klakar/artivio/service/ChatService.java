package com.klakar.artivio.service;

import com.klakar.artivio.dto.ConversationDTO;
import com.klakar.artivio.dto.MessageDTO;
import com.klakar.artivio.entity.Conversation;

import java.util.List;

public interface ChatService {
    ConversationDTO createOrGetConversation(String username, Long recipientId, Conversation.ConversationType type);
    MessageDTO sendMessage(String senderUsername, Long conversationId, String content);
    List<ConversationDTO> getUserConversations(String username);
    List<MessageDTO> getConversationMessages(Long conversationId, String username);
    void markMessageAsRead(Long messageId, String username);
    void markConversationAsRead(Long conversationId, String username);
    void deleteConversation(Long conversationId, String username);
}