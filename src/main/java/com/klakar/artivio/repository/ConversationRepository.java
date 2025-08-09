package com.klakar.artivio.repository;

import com.klakar.artivio.entity.Conversation;
import com.klakar.artivio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE (c.createdBy = :user OR c.participant = :user) AND c.isActive = true ORDER BY c.lastMessageAt DESC")
    List<Conversation> findUserConversations(@Param("user") User user);

    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'HUMAN_TO_HUMAN' AND " +
            "((c.createdBy = :user1 AND c.participant = :user2) OR (c.createdBy = :user2 AND c.participant = :user1)) " +
            "AND c.isActive = true")
    Optional<Conversation> findHumanToHumanConversation(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'HUMAN_TO_AI' AND c.createdBy = :user AND c.isActive = true")
    Optional<Conversation> findHumanToAIConversation(@Param("user") User user);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE (c.createdBy = :user OR c.participant = :user) AND c.isActive = true")
    Long countUserConversations(@Param("user") User user);
}