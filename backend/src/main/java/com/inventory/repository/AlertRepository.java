package com.inventory.repository;

import com.inventory.entity.Alert;
import com.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserOrderByCreatedAtDesc(User user);

    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Alert> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
