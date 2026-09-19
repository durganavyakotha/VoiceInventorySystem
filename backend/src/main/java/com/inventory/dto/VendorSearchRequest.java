package com.inventory.dto;

import lombok.Data;

@Data
public class VendorSearchRequest {

    private String product;
    private Double radius;
    private Integer minQty;
    private Double latitude;
    private Double longitude;
}
