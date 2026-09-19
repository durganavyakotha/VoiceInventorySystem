package com.inventory.repository;

import com.inventory.entity.StockTransaction;
import com.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findByUserOrderByCreatedAtDesc(User user);

    List<StockTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
