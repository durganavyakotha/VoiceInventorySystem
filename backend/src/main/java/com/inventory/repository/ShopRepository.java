package com.inventory.repository;

import com.inventory.entity.Shop;
import com.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findByUser(User user);

    Optional<Shop> findByUserId(Long userId);
}
