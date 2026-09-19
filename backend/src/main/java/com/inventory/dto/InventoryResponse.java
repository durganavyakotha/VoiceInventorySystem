package com.inventory.dto;

import com.inventory.entity.InventoryItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String barcode;
    private String category;
    private String imageUrl;
    private Integer quantity;
    private String unit;
    private Double costPerUnit;
    private Integer threshold;
    private boolean lowStock;
    private String availability;
    private LocalDateTime updatedAt;

    public static InventoryResponse from(InventoryItem item) {
        boolean low = item.getQuantity() <= item.getThreshold();
        String availability = item.getQuantity() <= 0 ? "OUT_OF_STOCK"
                : (low ? "LOW_STOCK" : "AVAILABLE");
        return InventoryResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .barcode(item.getProduct().getBarcode())
                .category(item.getProduct().getCategory())
                .imageUrl(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .unit(item.getUnit() != null ? item.getUnit() : "pieces")
                .costPerUnit(item.getCostPerUnit() != null ? item.getCostPerUnit() : 0.0)
                .threshold(item.getThreshold())
                .lowStock(low)
                .availability(availability)
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
