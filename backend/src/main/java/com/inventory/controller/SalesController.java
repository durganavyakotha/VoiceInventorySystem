package com.inventory.controller;

import com.inventory.dto.SalesSummaryResponse;
import com.inventory.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(salesService.listSales());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> record(
            @RequestParam String productName,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(salesService.recordSale(productName, quantity));
    }

    @GetMapping("/history")
    public ResponseEntity<SalesSummaryResponse> history(
            @RequestParam(required = false, defaultValue = "today") String period,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(salesService.history(period, startDate, endDate));
    }

    @GetMapping("/highest-selling")
    public ResponseEntity<Map<String, Object>> highestSelling(
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(salesService.highestSelling(period, startDate, endDate));
    }
}
