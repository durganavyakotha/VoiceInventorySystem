package com.inventory.controller;

import com.inventory.dto.InventoryRequest;
import com.inventory.dto.InventoryResponse;
import com.inventory.dto.VoiceCommandRequest;
import com.inventory.dto.VoiceCommandResponse;
import com.inventory.service.InventoryService;
import com.inventory.service.VoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final VoiceService voiceService;

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> list(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(inventoryService.searchByProductName(search));
        }
        return ResponseEntity.ok(inventoryService.listInventory());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> lowStock() {
        return ResponseEntity.ok(inventoryService.listLowStock());
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> add(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.addOrUpdate(request));
    }

    @PutMapping("/{id}/quantity")
    public ResponseEntity<InventoryResponse> updateQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.updateQuantity(id, quantity));
    }

    @PutMapping("/{id}/threshold")
    public ResponseEntity<InventoryResponse> setThreshold(
            @PathVariable Long id,
            @RequestParam Integer threshold) {
        return ResponseEntity.ok(inventoryService.setThreshold(id, threshold));
    }

    @PostMapping("/{id}/remove")
    public ResponseEntity<InventoryResponse> removeStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.removeStock(id, quantity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        inventoryService.deleteItem(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @PostMapping("/voice-command")
    public ResponseEntity<VoiceCommandResponse> voiceCommand(
            @Valid @RequestBody VoiceCommandRequest request) {
        return ResponseEntity.ok(voiceService.process(request));
    }

    @PostMapping("/barcode")
    public ResponseEntity<InventoryResponse> barcode(
            @RequestParam String barcode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer quantity) {
        return ResponseEntity.ok(inventoryService.barcodeLookupOrCreate(barcode, productName, quantity));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<InventoryResponse> addImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(inventoryService.addProductImage(id, file));
    }
}
