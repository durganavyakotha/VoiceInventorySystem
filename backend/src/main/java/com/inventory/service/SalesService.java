package com.inventory.service;

import com.inventory.dto.SalesSummaryResponse;
import com.inventory.entity.Product;
import com.inventory.entity.Sale;
import com.inventory.entity.User;
import com.inventory.enums.TransactionType;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final InventoryService inventoryService;

    @Transactional
    public Map<String, Object> recordSale(String productName, Integer quantity) {
        User shopkeeper = userService.getCurrentUser();
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be positive");
        }
        Product product = productRepository.findByNameIgnoreCase(productName.trim())
                .orElseThrow(() -> new RuntimeException("Product not found: " + productName));

        inventoryService.adjustStock(shopkeeper, product, quantity, TransactionType.SALE);

        Sale sale = saleRepository.save(Sale.builder()
                .shopkeeper(shopkeeper)
                .product(product)
                .quantity(quantity)
                .soldAt(LocalDateTime.now())
                .build());

        return toSaleMap(sale);
    }

    public List<Map<String, Object>> listSales() {
        User user = userService.getCurrentUser();
        return saleRepository.findByShopkeeperIdOrderBySoldAtDesc(user.getId()).stream()
                .map(this::toSaleMap)
                .collect(Collectors.toList());
    }

    public SalesSummaryResponse history(String period, LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();
        LocalDateTime[] range = resolveRange(period, startDate, endDate);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        List<Sale> sales = saleRepository.findByShopkeeperIdAndSoldAtBetweenOrderBySoldAtDesc(
                user.getId(), start, end);
        Long total = saleRepository.sumQuantityByShopkeeperAndDateRange(user.getId(), start, end);
        List<Object[]> highest = saleRepository.findHighestSelling(user.getId(), start, end);

        String highestProduct = null;
        Long highestQty = 0L;
        if (!highest.isEmpty()) {
            highestProduct = (String) highest.get(0)[0];
            highestQty = (Long) highest.get(0)[1];
        }

        return SalesSummaryResponse.builder()
                .totalSold(total != null ? total : 0L)
                .period(period != null ? period : "custom")
                .highestSellingProduct(highestProduct)
                .highestSellingQuantity(highestQty)
                .sales(sales.stream().map(this::toSaleMap).collect(Collectors.toList()))
                .build();
    }

    public Map<String, Object> highestSelling(String period, LocalDate startDate, LocalDate endDate) {
        SalesSummaryResponse summary = history(period, startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        result.put("product", summary.getHighestSellingProduct());
        result.put("quantity", summary.getHighestSellingQuantity());
        result.put("period", summary.getPeriod());
        result.put("totalSold", summary.getTotalSold());
        return result;
    }

    private LocalDateTime[] resolveRange(String period, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        String p = period != null ? period.toLowerCase() : "custom";
        return switch (p) {
            case "today" -> new LocalDateTime[]{
                    today.atStartOfDay(), today.atTime(LocalTime.MAX)
            };
            case "yesterday" -> {
                LocalDate y = today.minusDays(1);
                yield new LocalDateTime[]{y.atStartOfDay(), y.atTime(LocalTime.MAX)};
            }
            case "week" -> new LocalDateTime[]{
                    today.minusDays(6).atStartOfDay(), today.atTime(LocalTime.MAX)
            };
            case "month" -> new LocalDateTime[]{
                    today.minusDays(29).atStartOfDay(), today.atTime(LocalTime.MAX)
            };
            default -> {
                LocalDate s = startDate != null ? startDate : today.minusDays(30);
                LocalDate e = endDate != null ? endDate : today;
                yield new LocalDateTime[]{s.atStartOfDay(), e.atTime(LocalTime.MAX)};
            }
        };
    }

    private Map<String, Object> toSaleMap(Sale sale) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sale.getId());
        map.put("productId", sale.getProduct().getId());
        map.put("productName", sale.getProduct().getName());
        map.put("quantity", sale.getQuantity());
        map.put("soldAt", sale.getSoldAt());
        return map;
    }
}
