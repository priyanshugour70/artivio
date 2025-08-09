package com.klakar.artivio.repository;

import com.klakar.artivio.entity.Conversation;
import com.klakar.artivio.entity.Message;
import com.klakar.artivio.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC")
    List<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.embeddingStored = false")
    List<Message> findMessagesWithoutEmbeddings(Conversation conversation);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC")
    List<Message> findLastMessageByConversationQuery(@Param("conversation") Conversation conversation, Pageable pageable);

    default Optional<Message> findLastMessageByConversation(Conversation conversation) {
        List<Message> messages = findLastMessageByConversationQuery(conversation, PageRequest.of(0, 1));
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.isRead = false AND " +
            "(m.sender IS NULL OR m.sender != :user)")
    List<Message> findUnreadMessagesForUser(@Param("conversation") Conversation conversation, @Param("user") User user);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conversation AND m.isRead = false AND " +
            "(m.sender IS NULL OR m.sender != :user)")
    Long countUnreadMessagesForUser(@Param("conversation") Conversation conversation, @Param("user") User user);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<Message> findRecentMessagesByConversation(@Param("conversation") Conversation conversation, @Param("since") LocalDateTime since);

    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC")
    List<Message> findTopNMessagesByConversationQuery(@Param("conversation") Conversation conversation, Pageable pageable);

    default List<Message> findTopNMessagesByConversation(Conversation conversation, int limit) {
        return findTopNMessagesByConversationQuery(conversation, PageRequest.of(0, limit));
    }
}