package com.klakar.artivio.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private Long conversationId;
    private String content;
    private Long recipientId; // For human-to-human chat
}