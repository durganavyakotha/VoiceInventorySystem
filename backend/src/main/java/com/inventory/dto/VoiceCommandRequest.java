package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoiceCommandRequest {

    @NotBlank
    private String command;

    private String language;

    /** If true, parse only and return preview card fields without saving stock. */
    private Boolean previewOnly;
}
