package com.inventory.controller;

import com.inventory.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping("/nearby")
    public ResponseEntity<List<Map<String, Object>>> nearby(
            @RequestParam String product,
            @RequestParam(required = false, defaultValue = "10") Double radius,
            @RequestParam(required = false, defaultValue = "1") Integer minQty,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ResponseEntity.ok(vendorService.searchNearby(product, radius, minQty, latitude, longitude));
    }
}
