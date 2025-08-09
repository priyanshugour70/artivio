package com.klakar.artivio.dto;

import com.klakar.artivio.entity.Conversation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {
    private Long id;
    private Conversation.ConversationType conversationType;
    private String title;
    private UserDTO createdBy;
    private UserDTO participant;
    private List<MessageDTO> messages;
    private Boolean isActive;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer unreadCount;
    private MessageDTO lastMessage;
}