package com.inventory.repository;

import com.inventory.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    @Query("SELECT c FROM ChatConversation c WHERE " +
            "(c.participant1.id = :userId OR c.participant2.id = :userId) " +
            "ORDER BY c.updatedAt DESC")
    List<ChatConversation> findConversationsForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatConversation c WHERE " +
            "(c.participant1.id = :user1 AND c.participant2.id = :user2) OR " +
            "(c.participant1.id = :user2 AND c.participant2.id = :user1)")
    Optional<ChatConversation> findBetweenUsers(@Param("user1") Long user1, @Param("user2") Long user2);
}
