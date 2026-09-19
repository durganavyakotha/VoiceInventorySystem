package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoiceCommandRequest {

    @NotBlank
    private String command;

    private String language;
}
