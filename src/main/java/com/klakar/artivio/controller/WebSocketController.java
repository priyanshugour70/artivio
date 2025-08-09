package com.klakar.artivio.controller;

import com.klakar.artivio.dto.ChatMessageRequest;
import com.klakar.artivio.dto.MessageDTO;
import com.klakar.artivio.service.ChatService;
import com.klakar.artivio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserService userService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest chatMessage, Principal principal) {
        try {
            String username = principal.getName();
            log.info("WebSocket message received from {}: conversation {}", username, chatMessage.getConversationId());

            MessageDTO message = chatService.sendMessage(
                    username,
                    chatMessage.getConversationId(),
                    chatMessage.getContent()
            );

            log.info("WebSocket message sent successfully: {}", message.getId());
        } catch (Exception e) {
            log.error("Error processing WebSocket message from {}: {}",
                    principal != null ? principal.getName() : "unknown", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.typing")
    public void sendTypingNotification(@Payload TypingNotification notification, Principal principal) {
        try {
            String username = principal.getName();
            notification.setSender(username);

            log.debug("Typing notification from {}: conversation {}, typing: {}",
                    username, notification.getConversationId(), notification.isTyping());

            // Send typing notification to other participants in the conversation
            messagingTemplate.convertAndSend("/topic/typing/" + notification.getConversationId(), notification);
        } catch (Exception e) {
            log.error("Error processing typing notification: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.markRead")
    public void markMessageAsRead(@Payload ReadNotification notification, Principal principal) {
        try {
            String username = principal.getName();

            if (notification.getMessageId() != null) {
                chatService.markMessageAsRead(notification.getMessageId(), username);
            }

            if (notification.getConversationId() != null) {
                chatService.markConversationAsRead(notification.getConversationId(), username);
            }

            log.debug("Message/Conversation marked as read by {}", username);
        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "Unknown";

        log.info("WebSocket user connected: {}", username);

        if (!"Unknown".equals(username)) {
            try {
                userService.setUserOnline(username);

                // Notify other users about online status
                UserStatusMessage statusMessage = new UserStatusMessage(username, true);
                messagingTemplate.convertAndSend("/topic/user.status", statusMessage);

                log.info("User {} set online and status broadcasted", username);
            } catch (Exception e) {
                log.error("Error handling user connection for {}: {}", username, e.getMessage(), e);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "Unknown";

        log.info("WebSocket user disconnected: {}", username);

        if (!"Unknown".equals(username)) {
            try {
                userService.setUserOffline(username);

                // Notify other users about offline status
                UserStatusMessage statusMessage = new UserStatusMessage(username, false);
                messagingTemplate.convertAndSend("/topic/user.status", statusMessage);

                log.info("User {} set offline and status broadcasted", username);
            } catch (Exception e) {
                log.error("Error handling user disconnection for {}: {}", username, e.getMessage(), e);
            }
        }
    }

    // WebSocket message classes
    public static class TypingNotification {
        private String sender;
        private Long conversationId;
        private boolean typing;

        public TypingNotification() {}

        public TypingNotification(String sender, Long conversationId, boolean typing) {
            this.sender = sender;
            this.conversationId = conversationId;
            this.typing = typing;
        }

        // Getters and setters
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public boolean isTyping() { return typing; }
        public void setTyping(boolean typing) { this.typing = typing; }
    }

    public static class UserStatusMessage {
        private String username;
        private boolean online;

        public UserStatusMessage() {}

        public UserStatusMessage(String username, boolean online) {
            this.username = username;
            this.online = online;
        }

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }
    }

    public static class ReadNotification {
        private Long messageId;
        private Long conversationId;

        public ReadNotification() {}

        public ReadNotification(Long messageId, Long conversationId) {
            this.messageId = messageId;
            this.conversationId = conversationId;
        }

        // Getters and setters
        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    }
}