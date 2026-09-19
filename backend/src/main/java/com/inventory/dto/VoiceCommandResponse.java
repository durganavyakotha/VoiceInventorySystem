package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCommandResponse {

    private String action;
    private String spokenResponse;
    private boolean success;
    private Object data;
    private List<Map<String, Object>> vendors;

    /** Parsed fields for voice-add preview cards */
    private String productName;
    private Integer quantity;
    private String unit;
    private Double costPerUnit;
    private Double confidence;
    private String matchedFrom;
}
