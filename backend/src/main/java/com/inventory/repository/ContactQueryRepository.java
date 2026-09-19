package com.inventory.repository;

import com.inventory.entity.ContactQuery;
import com.inventory.enums.QueryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactQueryRepository extends JpaRepository<ContactQuery, Long> {

    List<ContactQuery> findAllByOrderByCreatedAtDesc();

    List<ContactQuery> findByStatusOrderByCreatedAtDesc(QueryStatus status);

    List<ContactQuery> findByUserIdOrderByCreatedAtDesc(Long userId);
}
