package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InventoryRequest {

    @NotBlank
    private String productName;

    private String barcode;

    private String category;

    @Min(0)
    private Integer quantity;

    @Min(0)
    private Integer threshold;

    private String imageUrl;
}
