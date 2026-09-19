package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryResponse {

    private Long totalSold;
    private String period;
    private String highestSellingProduct;
    private Long highestSellingQuantity;
    private List<Map<String, Object>> sales;
}
