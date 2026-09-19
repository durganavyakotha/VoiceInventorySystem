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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String availability) {
        String nameFilter = name != null ? name : search;
        if ((nameFilter != null && !nameFilter.isBlank())
                || (category != null && !category.isBlank())
                || (availability != null && !availability.isBlank())) {
            return ResponseEntity.ok(inventoryService.filterInventory(nameFilter, category, availability));
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

    @PostMapping(value = "/with-image", consumes = "multipart/form-data")
    public ResponseEntity<InventoryResponse> addWithImage(
            @RequestParam String productName,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer threshold,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) Double costPerUnit,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = inventoryService.storeProductImage(file);
        InventoryRequest request = new InventoryRequest();
        request.setProductName(productName);
        request.setQuantity(quantity);
        request.setUnit(unit);
        request.setCategory(category);
        request.setThreshold(threshold);
        request.setBarcode(barcode);
        request.setCostPerUnit(costPerUnit);
        request.setImageUrl(imageUrl);
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

    @PostMapping(value = "/barcode", consumes = "multipart/form-data")
    public ResponseEntity<InventoryResponse> barcodeWithImage(
            @RequestParam String barcode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String category,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = inventoryService.storeProductImage(file);
        return ResponseEntity.ok(inventoryService.barcodeLookupOrCreate(
                barcode, productName, quantity, unit, category, imageUrl));
    }

    @PostMapping("/barcode-json")
    public ResponseEntity<InventoryResponse> barcodeJson(@RequestBody Map<String, Object> body) {
        String barcode = String.valueOf(body.get("barcode"));
        String productName = body.get("productName") != null ? String.valueOf(body.get("productName")) : null;
        Integer quantity = body.get("quantity") != null ? Integer.valueOf(body.get("quantity").toString()) : 1;
        String unit = body.get("unit") != null ? String.valueOf(body.get("unit")) : "pieces";
        String category = body.get("category") != null ? String.valueOf(body.get("category")) : "General";
        String imageUrl = body.get("imageUrl") != null ? String.valueOf(body.get("imageUrl")) : null;
        return ResponseEntity.ok(inventoryService.barcodeLookupOrCreate(
                barcode, productName, quantity, unit, category, imageUrl));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<InventoryResponse> addImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(inventoryService.addProductImage(id, file));
    }
}
