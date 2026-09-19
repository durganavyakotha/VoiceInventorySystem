package com.inventory.dto;

import com.inventory.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessageRequest {

    @NotNull
    private Long receiverId;

    private Long conversationId;

    private MessageType messageType;

    private String messageText;

    private String audioUrl;

    private String transcript;

    private String sourceLanguage;
}
