package com.inventory.repository;

import com.inventory.entity.ChatConversation;
import com.inventory.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationOrderByCreatedAtAsc(ChatConversation conversation);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
