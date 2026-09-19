package com.inventory.repository;

import com.inventory.entity.Sale;
import com.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByShopkeeperOrderBySoldAtDesc(User shopkeeper);

    List<Sale> findByShopkeeperIdOrderBySoldAtDesc(Long shopkeeperId);

    List<Sale> findByShopkeeperAndSoldAtBetweenOrderBySoldAtDesc(
            User shopkeeper, LocalDateTime start, LocalDateTime end);

    List<Sale> findByShopkeeperIdAndSoldAtBetweenOrderBySoldAtDesc(
            Long shopkeeperId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s.product.name, SUM(s.quantity) FROM Sale s WHERE s.shopkeeper.id = :shopkeeperId " +
            "AND s.soldAt BETWEEN :start AND :end GROUP BY s.product.name ORDER BY SUM(s.quantity) DESC")
    List<Object[]> findHighestSelling(@Param("shopkeeperId") Long shopkeeperId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s WHERE s.shopkeeper.id = :shopkeeperId " +
            "AND s.soldAt BETWEEN :start AND :end")
    Long sumQuantityByShopkeeperAndDateRange(@Param("shopkeeperId") Long shopkeeperId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
