package com.klakar.artivio.dto;

import com.klakar.artivio.entity.Message;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private UserDTO sender;
    private String content;
    private Message.MessageType messageType;
    private Message.MessageStatus messageStatus;
    private Boolean isRead;
    private Boolean isDelivered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}