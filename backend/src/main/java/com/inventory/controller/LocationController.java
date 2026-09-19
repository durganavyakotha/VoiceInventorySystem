package com.inventory.controller;

import com.inventory.config.AppLocations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(AppLocations.asMaps());
    }
}
