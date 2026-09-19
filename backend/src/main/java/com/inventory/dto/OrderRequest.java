package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull
    private Long vendorId;

    @NotBlank
    private String productName;

    @NotNull
    @Min(1)
    private Integer quantity;

    private Integer deliveryWithinDays;
}
