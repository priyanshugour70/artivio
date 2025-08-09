package com.klakar.artivio.controller;

import com.klakar.artivio.dto.ChatMessageRequest;
import com.klakar.artivio.dto.ConversationDTO;
import com.klakar.artivio.dto.MessageDTO;
import com.klakar.artivio.dto.UserDTO;
import com.klakar.artivio.entity.Conversation;
import com.klakar.artivio.service.ChatService;
import com.klakar.artivio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> getUserConversations(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.debug("Getting conversations for user: {}", username);

            List<ConversationDTO> conversations = chatService.getUserConversations(username);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("Error getting conversations", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationDTO> createOrGetConversation(
            @RequestParam(required = false) Long recipientId,
            @RequestParam(defaultValue = "HUMAN_TO_AI") String type,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Conversation.ConversationType conversationType = Conversation.ConversationType.valueOf(type);

            log.info("Creating/getting conversation for user: {}, type: {}, recipientId: {}",
                    username, type, recipientId);

            ConversationDTO conversation = chatService.createOrGetConversation(username, recipientId, conversationType);
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            log.error("Error creating/getting conversation", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageDTO>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.debug("Getting messages for conversation: {} by user: {}", conversationId, username);

            List<MessageDTO> messages = chatService.getConversationMessages(conversationId, username);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting messages for conversation: {}", conversationId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Sending message from user: {} to conversation: {}", username, request.getConversationId());

            MessageDTO message = chatService.sendMessage(username, request.getConversationId(), request.getContent());
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<Void> markMessageAsRead(
            @PathVariable Long messageId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            chatService.markMessageAsRead(messageId, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking message as read: {}", messageId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable Long conversationId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            chatService.markConversationAsRead(conversationId, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking conversation as read: {}", conversationId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long conversationId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            chatService.deleteConversation(conversationId, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting conversation: {}", conversationId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users/online")
    public ResponseEntity<List<UserDTO>> getOnlineUsers(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<UserDTO> onlineUsers = userService.getOnlineUsers(username);
            return ResponseEntity.ok(onlineUsers);
        } catch (Exception e) {
            log.error("Error getting online users", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<UserDTO> users = userService.getAllUsers(username);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error getting all users", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long userId) {
        try {
            UserDTO user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error getting user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ChatService",
                "timestamp", System.currentTimeMillis()
        ));
    }
}