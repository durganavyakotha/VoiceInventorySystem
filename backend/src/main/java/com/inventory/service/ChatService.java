package com.inventory.service;

import com.inventory.dto.ChatMessageRequest;
import com.inventory.entity.ChatConversation;
import com.inventory.entity.ChatMessage;
import com.inventory.entity.User;
import com.inventory.enums.AlertType;
import com.inventory.enums.MessageType;
import com.inventory.repository.ChatConversationRepository;
import com.inventory.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserService userService;
    private final TranslationService translationService;
    private final AlertService alertService;

    @Transactional
    public Map<String, Object> getOrCreateConversation(Long otherUserId) {
        User current = userService.getCurrentUser();
        User other = userService.getUserById(otherUserId);
        ChatConversation conversation = conversationRepository
                .findBetweenUsers(current.getId(), other.getId())
                .orElseGet(() -> conversationRepository.save(ChatConversation.builder()
                        .participant1(current)
                        .participant2(other)
                        .build()));
        return toConversationMap(conversation, current);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listConversations() {
        User current = userService.getCurrentUser();
        return conversationRepository.findConversationsForUser(current.getId()).stream()
                .map(c -> toConversationMap(c, current))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> sendText(ChatMessageRequest request) {
        User sender = userService.getCurrentUser();
        User receiver = userService.getUserById(request.getReceiverId());

        ChatConversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
        } else {
            conversation = conversationRepository.findBetweenUsers(sender.getId(), receiver.getId())
                    .orElseGet(() -> conversationRepository.save(ChatConversation.builder()
                            .participant1(sender)
                            .participant2(receiver)
                            .build()));
        }

        String sourceLang = request.getSourceLanguage() != null ? request.getSourceLanguage() : sender.getLanguage();
        String targetLang = receiver.getLanguage() != null ? receiver.getLanguage() : "en";
        String text = request.getMessageText();
        String translated = translationService.translate(text, sourceLang, targetLang);

        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .receiver(receiver)
                .messageType(MessageType.TEXT)
                .messageText(text)
                .sourceLanguage(sourceLang)
                .targetLanguage(targetLang)
                .translatedText(translated)
                .build());

        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);

        alertService.createAlert(receiver, AlertType.MESSAGE,
                "New message from " + sender.getFirstName() + " " + sender.getLastName());

        return toMessageMap(message);
    }

    @Transactional
    public Map<String, Object> sendVoice(ChatMessageRequest request) {
        User sender = userService.getCurrentUser();
        User receiver = userService.getUserById(request.getReceiverId());

        ChatConversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
        } else {
            conversation = conversationRepository.findBetweenUsers(sender.getId(), receiver.getId())
                    .orElseGet(() -> conversationRepository.save(ChatConversation.builder()
                            .participant1(sender)
                            .participant2(receiver)
                            .build()));
        }

        String sourceLang = request.getSourceLanguage() != null ? request.getSourceLanguage() : sender.getLanguage();
        String targetLang = receiver.getLanguage() != null ? receiver.getLanguage() : "en";
        String transcript = request.getTranscript() != null ? request.getTranscript() : request.getMessageText();
        String translated = translationService.translate(transcript, sourceLang, targetLang);

        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .receiver(receiver)
                .messageType(MessageType.VOICE)
                .messageText(transcript)
                .transcript(transcript)
                .audioUrl(request.getAudioUrl())
                .sourceLanguage(sourceLang)
                .targetLanguage(targetLang)
                .translatedText(translated)
                .build());

        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);

        alertService.createAlert(receiver, AlertType.MESSAGE,
                "New voice message from " + sender.getFirstName());

        return toMessageMap(message);
    }

    /** Send a message as a specific user (used by chatbot assistant). */
    @Transactional
    public Map<String, Object> sendAsUser(User sender, User receiver, ChatMessageRequest request) {
        ChatConversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
        } else {
            conversation = conversationRepository.findBetweenUsers(sender.getId(), receiver.getId())
                    .orElseGet(() -> conversationRepository.save(ChatConversation.builder()
                            .participant1(sender)
                            .participant2(receiver)
                            .build()));
        }
        String sourceLang = request.getSourceLanguage() != null ? request.getSourceLanguage() : "en";
        String targetLang = receiver.getLanguage() != null ? receiver.getLanguage() : "en";
        String text = request.getMessageText();
        String translated = translationService.translate(text, sourceLang, targetLang);

        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .receiver(receiver)
                .messageType(MessageType.TEXT)
                .messageText(text)
                .sourceLanguage(sourceLang)
                .targetLanguage(targetLang)
                .translatedText(translated)
                .build());

        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);
        return toMessageMap(message);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMessages(Long conversationId) {
        User current = userService.getCurrentUser();
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conversation.getParticipant1().getId().equals(current.getId())
                && !conversation.getParticipant2().getId().equals(current.getId())) {
            throw new RuntimeException("Access denied");
        }
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toConversationMap(ChatConversation c, User current) {
        User other = c.getParticipant1().getId().equals(current.getId())
                ? c.getParticipant2() : c.getParticipant1();
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("otherUser", Map.of(
                "id", other.getId(),
                "firstName", other.getFirstName(),
                "lastName", other.getLastName(),
                "email", other.getEmail(),
                "role", other.getRole().name(),
                "language", other.getLanguage() != null ? other.getLanguage() : "en"
        ));
        map.put("createdAt", c.getCreatedAt());
        map.put("updatedAt", c.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toMessageMap(ChatMessage m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("conversationId", m.getConversation().getId());
        map.put("senderId", m.getSender().getId());
        map.put("receiverId", m.getReceiver().getId());
        map.put("messageType", m.getMessageType());
        map.put("messageText", m.getMessageText());
        map.put("sourceLanguage", m.getSourceLanguage());
        map.put("targetLanguage", m.getTargetLanguage());
        map.put("translatedText", m.getTranslatedText());
        map.put("audioUrl", m.getAudioUrl());
        map.put("transcript", m.getTranscript());
        map.put("createdAt", m.getCreatedAt());
        return map;
    }
}
