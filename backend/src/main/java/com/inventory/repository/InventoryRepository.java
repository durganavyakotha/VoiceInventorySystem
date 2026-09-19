package com.inventory.repository;

import com.inventory.entity.InventoryItem;
import com.inventory.entity.Product;
import com.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByUser(User user);

    List<InventoryItem> findByUserId(Long userId);

    Optional<InventoryItem> findByUserAndProduct(User user, Product product);

    Optional<InventoryItem> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT i FROM InventoryItem i WHERE i.user.id = :userId AND " +
            "LOWER(i.product.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<InventoryItem> findByUserIdAndProductNameContaining(@Param("userId") Long userId,
                                                             @Param("name") String name);

    @Query("SELECT i FROM InventoryItem i WHERE i.user.id = :userId AND i.quantity <= i.threshold")
    List<InventoryItem> findLowStockByUserId(@Param("userId") Long userId);

    @Query("SELECT i FROM InventoryItem i WHERE i.user.role = com.inventory.enums.Role.VENDOR AND " +
            "LOWER(i.product.name) LIKE LOWER(CONCAT('%', :productName, '%')) AND i.quantity >= :minQty")
    List<InventoryItem> findVendorStockByProductName(@Param("productName") String productName,
                                                     @Param("minQty") Integer minQty);
}
