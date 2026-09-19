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
    private Integer threshold;
    private boolean lowStock;
    private LocalDateTime updatedAt;

    public static InventoryResponse from(InventoryItem item) {
        return InventoryResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .barcode(item.getProduct().getBarcode())
                .category(item.getProduct().getCategory())
                .imageUrl(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .threshold(item.getThreshold())
                .lowStock(item.getQuantity() <= item.getThreshold())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
