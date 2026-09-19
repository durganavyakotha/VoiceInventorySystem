package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    /** Unit of measure: kg, g, L, ml, packets, bottles, pieces, bags, etc. */
    @Builder.Default
    private String unit = "pieces";

    @Builder.Default
    private Integer threshold = 5;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
