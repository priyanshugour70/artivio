package com.klakar.artivio.service.impl;

import com.klakar.artivio.dto.ConversationDTO;
import com.klakar.artivio.dto.MessageDTO;
import com.klakar.artivio.dto.UserDTO;
import com.klakar.artivio.entity.Conversation;
import com.klakar.artivio.entity.Message;
import com.klakar.artivio.entity.User;
import com.klakar.artivio.repository.ConversationRepository;
import com.klakar.artivio.repository.MessageRepository;
import com.klakar.artivio.service.AIContextService;
import com.klakar.artivio.service.AIService;
import com.klakar.artivio.service.ChatService;
import com.klakar.artivio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AIService aiService;
    private final AIContextService aiContextService;

    @Override
    public ConversationDTO createOrGetConversation(String username, Long recipientId, Conversation.ConversationType type) {
        try {
            User user = userService.findUserByUsername(username);
            Conversation conversation;

            if (type == Conversation.ConversationType.HUMAN_TO_HUMAN) {
                if (recipientId == null) {
                    throw new RuntimeException("Recipient ID is required for human-to-human conversation");
                }
                User recipient = userService.findUserById(recipientId);
                conversation = conversationRepository.findHumanToHumanConversation(user, recipient)
                        .orElseGet(() -> createNewConversation(user, recipient, type));
            } else {
                conversation = conversationRepository.findHumanToAIConversation(user)
                        .orElseGet(() -> createNewConversation(user, null, type));
            }

            log.info("Created/Retrieved conversation {} for user {}", conversation.getId(), username);
            return convertToDTO(conversation);

        } catch (Exception e) {
            log.error("Error creating/getting conversation for user {}", username, e);
            throw new RuntimeException("Failed to create conversation: " + e.getMessage());
        }
    }

    private Conversation createNewConversation(User createdBy, User participant, Conversation.ConversationType type) {
        String title;
        if (type == Conversation.ConversationType.HUMAN_TO_AI) {
            title = "Chat with Artivio AI";
        } else {
            title = "Chat with " + (participant != null ? participant.getDisplayName() : "Unknown");
        }

        Conversation conversation = Conversation.builder()
                .conversationType(type)
                .createdBy(createdBy)
                .participant(participant)
                .title(title)
                .isActive(true)
                .lastMessageAt(LocalDateTime.now())
                .build();

        Conversation saved = conversationRepository.save(conversation);
        log.info("Created new conversation: {} - {}", saved.getId(), title);
        return saved;
    }

    @Override
    public MessageDTO sendMessage(String senderUsername, Long conversationId, String content) {
        try {
            User sender = userService.findUserByUsername(senderUsername);
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            // Validate user can send to this conversation
            validateUserAccess(conversation, sender);

            // Create user message
            Message message = Message.builder()
                    .conversation(conversation)
                    .sender(sender)
                    .content(content.trim())
                    .messageType(Message.MessageType.HUMAN)
                    .isRead(false)
                    .isDelivered(true)
                    .messageStatus(Message.MessageStatus.SENT)
                    .build();

            Message savedMessage = messageRepository.save(message);

            // Update conversation timestamp
            conversation.setLastMessageAt(LocalDateTime.now());
            conversationRepository.save(conversation);

            // Store context (safe with fallback)
            try {
                aiContextService.storeConversationContext(
                        conversationId.toString(),
                        content,
                        sender.getDisplayName()
                );
            } catch (Exception e) {
                log.warn("Failed to store AI context: {}", e.getMessage());
            }

            MessageDTO messageDTO = convertToMessageDTO(savedMessage);

            // Send to WebSocket subscribers
            sendMessageToSubscribers(conversation, messageDTO, sender);

            // Generate AI response if it's an AI conversation
            if (conversation.getConversationType() == Conversation.ConversationType.HUMAN_TO_AI) {
                generateAIResponseAsync(conversation, content, sender);
            }

            log.info("Message sent successfully: {} in conversation {}", savedMessage.getId(), conversationId);
            return messageDTO;

        } catch (Exception e) {
            log.error("Error sending message from {} to conversation {}", senderUsername, conversationId, e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    private void validateUserAccess(Conversation conversation, User user) {
        boolean hasAccess = conversation.getCreatedBy().equals(user) ||
                (conversation.getParticipant() != null && conversation.getParticipant().equals(user));

        if (!hasAccess) {
            throw new RuntimeException("Access denied to conversation");
        }
    }

    private void sendMessageToSubscribers(Conversation conversation, MessageDTO messageDTO, User sender) {
        try {
            if (conversation.getConversationType() == Conversation.ConversationType.HUMAN_TO_HUMAN) {
                // Send to both participants
                messagingTemplate.convertAndSendToUser(
                        conversation.getCreatedBy().getUsername(),
                        "/queue/messages",
                        messageDTO
                );

                if (conversation.getParticipant() != null) {
                    messagingTemplate.convertAndSendToUser(
                            conversation.getParticipant().getUsername(),
                            "/queue/messages",
                            messageDTO
                    );
                }
            } else {
                // For AI conversation, send to sender
                messagingTemplate.convertAndSendToUser(
                        sender.getUsername(),
                        "/queue/messages",
                        messageDTO
                );
            }
        } catch (Exception e) {
            log.error("Error sending message to WebSocket subscribers", e);
        }
    }

    private void generateAIResponseAsync(Conversation conversation, String userMessage, User user) {
        CompletableFuture.runAsync(() -> {
            try {
                // Get relevant context (safe with fallback)
                String context = "";
                try {
                    context = aiContextService.getRelevantContext(
                            conversation.getId().toString(),
                            userMessage,
                            5
                    );
                } catch (Exception e) {
                    log.warn("Failed to get AI context: {}", e.getMessage());
                }

                // Generate AI response
                aiService.generateResponse(userMessage, context)
                        .thenAccept(aiResponse -> {
                            try {
                                // Create AI message
                                Message aiMessage = Message.builder()
                                        .conversation(conversation)
                                        .sender(null) // AI message
                                        .content(aiResponse)
                                        .messageType(Message.MessageType.AI)
                                        .isRead(false)
                                        .isDelivered(true)
                                        .messageStatus(Message.MessageStatus.SENT)
                                        .build();

                                Message savedAIMessage = messageRepository.save(aiMessage);

                                // Store AI response in context (safe with fallback)
                                try {
                                    aiContextService.storeConversationContext(
                                            conversation.getId().toString(),
                                            aiResponse,
                                            "Artivio AI"
                                    );
                                } catch (Exception e) {
                                    log.warn("Failed to store AI response context: {}", e.getMessage());
                                }

                                // Update conversation timestamp
                                conversation.setLastMessageAt(LocalDateTime.now());
                                conversationRepository.save(conversation);

                                // Send streaming response
                                sendStreamingMessage(user.getUsername(), savedAIMessage);

                                log.info("AI response sent for conversation {}", conversation.getId());

                            } catch (Exception e) {
                                log.error("Error saving AI response", e);
                                sendErrorMessage(conversation, user);
                            }
                        })
                        .exceptionally(throwable -> {
                            log.error("Error generating AI response", throwable);
                            sendErrorMessage(conversation, user);
                            return null;
                        });

            } catch (Exception e) {
                log.error("Error in generateAIResponseAsync", e);
                sendErrorMessage(conversation, user);
            }
        });
    }

    private void sendErrorMessage(Conversation conversation, User user) {
        try {
            Message errorMessage = Message.builder()
                    .conversation(conversation)
                    .sender(null)
                    .content("Sorry, I'm having trouble processing your message right now. Please try again.")
                    .messageType(Message.MessageType.AI)
                    .isRead(false)
                    .isDelivered(true)
                    .messageStatus(Message.MessageStatus.SENT)
                    .build();

            Message savedErrorMessage = messageRepository.save(errorMessage);
            MessageDTO errorDTO = convertToMessageDTO(savedErrorMessage);

            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/messages",
                    errorDTO
            );
        } catch (Exception e) {
            log.error("Error sending error message", e);
        }
    }

    private void sendStreamingMessage(String username, Message message) {
        try {
            MessageDTO messageDTO = convertToMessageDTO(message);
            String[] words = message.getContent().split("\\s+");

            // Send initial message with empty content
            messageDTO.setContent("");
            messagingTemplate.convertAndSendToUser(username, "/queue/messages", messageDTO);

            // Send words one by one for streaming effect
            StringBuilder currentContent = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                try {
                    Thread.sleep(100); // 100ms delay between words
                    currentContent.append(words[i]);
                    if (i < words.length - 1) {
                        currentContent.append(" ");
                    }

                    messageDTO.setContent(currentContent.toString());

                    // Send update
                    messagingTemplate.convertAndSendToUser(
                            username,
                            "/queue/message-updates",
                            messageDTO
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error sending streaming message", e);
            // Fallback: send complete message
            MessageDTO fallbackDTO = convertToMessageDTO(message);
            messagingTemplate.convertAndSendToUser(username, "/queue/messages", fallbackDTO);
        }
    }

    @Override
    public List<ConversationDTO> getUserConversations(String username) {
        try {
            User user = userService.findUserByUsername(username);
            List<Conversation> conversations = conversationRepository.findUserConversations(user);

            return conversations.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting conversations for user {}", username, e);
            return List.of();
        }
    }

    @Override
    public List<MessageDTO> getConversationMessages(Long conversationId, String username) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            User user = userService.findUserByUsername(username);
            validateUserAccess(conversation, user);

            List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);

            return messages.stream()
                    .map(this::convertToMessageDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting messages for conversation {} and user {}", conversationId, username, e);
            throw new RuntimeException("Failed to get messages: " + e.getMessage());
        }
    }

    @Override
    public void markMessageAsRead(Long messageId, String username) {
        try {
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message not found"));

            User user = userService.findUserByUsername(username);

            // Only mark as read if user is not the sender
            if (message.getSender() == null || !message.getSender().equals(user)) {
                message.setIsRead(true);
                message.setMessageStatus(Message.MessageStatus.READ);
                messageRepository.save(message);

                log.debug("Message {} marked as read by {}", messageId, username);
            }
        } catch (Exception e) {
            log.error("Error marking message {} as read for user {}", messageId, username, e);
        }
    }

    @Override
    public void markConversationAsRead(Long conversationId, String username) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            User user = userService.findUserByUsername(username);
            validateUserAccess(conversation, user);

            List<Message> unreadMessages = messageRepository.findUnreadMessagesForUser(conversation, user);

            unreadMessages.forEach(message -> {
                message.setIsRead(true);
                message.setMessageStatus(Message.MessageStatus.READ);
            });

            messageRepository.saveAll(unreadMessages);

            log.info("Marked {} messages as read in conversation {} for user {}",
                    unreadMessages.size(), conversationId, username);

        } catch (Exception e) {
            log.error("Error marking conversation {} as read for user {}", conversationId, username, e);
        }
    }

    @Override
    public void deleteConversation(Long conversationId, String username) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            User user = userService.findUserByUsername(username);
            validateUserAccess(conversation, user);

            conversation.setIsActive(false);
            conversationRepository.save(conversation);

            log.info("Conversation {} deleted by user {}", conversationId, username);

        } catch (Exception e) {
            log.error("Error deleting conversation {} for user {}", conversationId, username, e);
            throw new RuntimeException("Failed to delete conversation: " + e.getMessage());
        }
    }

    private ConversationDTO convertToDTO(Conversation conversation) {
        ConversationDTO dto = modelMapper.map(conversation, ConversationDTO.class);

        // Get last message
        try {
            Message lastMessage = messageRepository.findLastMessageByConversation(conversation).orElse(null);
            if (lastMessage != null) {
                dto.setLastMessage(convertToMessageDTO(lastMessage));
            }
        } catch (Exception e) {
            log.warn("Error getting last message for conversation {}: {}", conversation.getId(), e.getMessage());
        }

        // Set default unread count
        dto.setUnreadCount(0);

        // Get recent messages (last 10)
        try {
            List<Message> recentMessages = messageRepository.findTopNMessagesByConversation(conversation, 10);
            List<MessageDTO> messageDTOs = recentMessages.stream()
                    .map(this::convertToMessageDTO)
                    .collect(Collectors.toList());
            dto.setMessages(messageDTOs);
        } catch (Exception e) {
            log.warn("Error getting recent messages for conversation {}: {}", conversation.getId(), e.getMessage());
            dto.setMessages(List.of());
        }

        return dto;
    }

    private MessageDTO convertToMessageDTO(Message message) {
        MessageDTO dto = modelMapper.map(message, MessageDTO.class);
        dto.setConversationId(message.getConversation().getId());

        if (message.getSender() != null) {
            dto.setSender(modelMapper.map(message.getSender(), UserDTO.class));
        }

        return dto;
    }
}
