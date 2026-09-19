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

    /** Required: product picture URL or upload path after multipart save. */
    private String imageUrl;

    /** Unit: kg, packets, bottles, pieces, bags, L, etc. */
    private String unit;
}
