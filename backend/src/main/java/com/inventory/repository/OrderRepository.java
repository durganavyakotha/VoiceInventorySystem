package com.inventory.repository;

import com.inventory.entity.OrderEntity;
import com.inventory.entity.User;
import com.inventory.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByShopkeeperOrderByRequestedAtDesc(User shopkeeper);

    List<OrderEntity> findByShopkeeperIdOrderByRequestedAtDesc(Long shopkeeperId);

    List<OrderEntity> findByVendorOrderByRequestedAtDesc(User vendor);

    List<OrderEntity> findByVendorIdOrderByRequestedAtDesc(Long vendorId);

    List<OrderEntity> findByShopkeeperIdAndStatus(Long shopkeeperId, OrderStatus status);

    List<OrderEntity> findByVendorIdAndStatus(Long vendorId, OrderStatus status);
}
