package com.inventory.service;

import com.inventory.dto.InventoryRequest;
import com.inventory.dto.InventoryResponse;
import com.inventory.entity.InventoryItem;
import com.inventory.entity.Product;
import com.inventory.entity.StockTransaction;
import com.inventory.entity.User;
import com.inventory.enums.AlertType;
import com.inventory.enums.TransactionType;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockTransactionRepository;
import com.inventory.util.FuzzyMatcher;
import com.inventory.util.ProductAliases;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final UserService userService;
    private final AlertService alertService;

    public List<InventoryResponse> listInventory() {
        User user = userService.getCurrentUser();
        return inventoryRepository.findByUser(user).stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> filterInventory(String name, String category, String availability) {
        User user = userService.getCurrentUser();
        return inventoryRepository.findByUser(user).stream()
                .map(InventoryResponse::from)
                .filter(item -> name == null || name.isBlank()
                        || item.getProductName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
                .filter(item -> category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)
                        || (item.getCategory() != null
                        && item.getCategory().equalsIgnoreCase(category)))
                .filter(item -> matchesAvailability(item, availability))
                .collect(Collectors.toList());
    }

    private boolean matchesAvailability(InventoryResponse item, String availability) {
        if (availability == null || availability.isBlank() || "ALL".equalsIgnoreCase(availability)) {
            return true;
        }
        return availability.equalsIgnoreCase(item.getAvailability());
    }

    public List<InventoryResponse> searchByProductName(String name) {
        return filterInventory(name, null, null);
    }

    public List<InventoryResponse> listLowStock() {
        User user = userService.getCurrentUser();
        return inventoryRepository.findLowStockByUserId(user.getId()).stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> listByUserId(Long userId) {
        return inventoryRepository.findByUserId(userId).stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryResponse addOrUpdate(InventoryRequest request) {
        User user = userService.getCurrentUser();
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new RuntimeException("Product picture is required");
        }
        String unit = normalizeUnit(request.getUnit());
        Product product = findOrCreateProduct(request.getProductName(), request.getBarcode(),
                request.getCategory(), request.getImageUrl());

        InventoryItem item = inventoryRepository.findByUserAndProduct(user, product)
                .orElse(InventoryItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .unit(unit)
                        .threshold(request.getThreshold() != null ? request.getThreshold() : 5)
                        .build());

        int addQty = request.getQuantity() != null ? request.getQuantity() : 0;
        item.setQuantity(item.getQuantity() + addQty);
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            item.setUnit(unit);
        }
        if (request.getCostPerUnit() != null) {
            item.setCostPerUnit(request.getCostPerUnit());
        }
        if (request.getThreshold() != null) {
            item.setThreshold(request.getThreshold());
        }
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            product.setImageUrl(request.getImageUrl());
            productRepository.save(product);
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            product.setCategory(request.getCategory());
            productRepository.save(product);
        }
        item = inventoryRepository.save(item);

        if (addQty > 0) {
            recordTransaction(user, product, TransactionType.ADD, addQty);
        }

        checkLowStock(user, item);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse addWithUnit(String productName, Integer quantity, String unit, String category, String imageUrl) {
        InventoryRequest request = new InventoryRequest();
        request.setProductName(productName);
        request.setQuantity(quantity);
        request.setUnit(unit);
        request.setCategory(category != null ? category : "General");
        request.setImageUrl(imageUrl != null ? imageUrl : "/uploads/products/placeholder.svg");
        return addOrUpdate(request);
    }

    @Transactional
    public InventoryResponse updateQuantity(Long itemId, Integer quantity) {
        User user = userService.getCurrentUser();
        InventoryItem item = getOwnedItem(itemId, user);
        int oldQty = item.getQuantity();
        item.setQuantity(quantity);
        item = inventoryRepository.save(item);

        int diff = quantity - oldQty;
        if (diff != 0) {
            recordTransaction(user, item.getProduct(), TransactionType.ADJUSTMENT, Math.abs(diff));
        }
        checkLowStock(user, item);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse setThreshold(Long itemId, Integer threshold) {
        User user = userService.getCurrentUser();
        InventoryItem item = getOwnedItem(itemId, user);
        item.setThreshold(threshold);
        item = inventoryRepository.save(item);
        checkLowStock(user, item);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse removeStock(Long itemId, Integer quantity) {
        User user = userService.getCurrentUser();
        InventoryItem item = getOwnedItem(itemId, user);
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be positive");
        }
        if (item.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }
        item.setQuantity(item.getQuantity() - quantity);
        item = inventoryRepository.save(item);
        recordTransaction(user, item.getProduct(), TransactionType.REMOVE, quantity);
        checkLowStock(user, item);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse addStockByProductName(String productName, Integer quantity, String unit) {
        return addStockByProductName(productName, quantity, unit, null);
    }

    @Transactional
    public InventoryResponse addStockByProductName(String productName, Integer quantity, String unit, Double costPerUnit) {
        User user = userService.getCurrentUser();
        String resolved = resolveNearName(productName);
        Product product = findOrCreateProduct(resolved, null, "General",
                "/uploads/products/placeholder.svg");
        InventoryItem item = inventoryRepository.findByUserAndProduct(user, product)
                .orElse(InventoryItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .unit(normalizeUnit(unit))
                        .costPerUnit(costPerUnit != null ? costPerUnit : 0.0)
                        .threshold(5)
                        .build());
        item.setQuantity(item.getQuantity() + quantity);
        if (unit != null && !unit.isBlank()) {
            item.setUnit(normalizeUnit(unit));
        }
        if (costPerUnit != null && costPerUnit > 0) {
            item.setCostPerUnit(costPerUnit);
        }
        item = inventoryRepository.save(item);
        recordTransaction(user, product, TransactionType.ADD, quantity);
        checkLowStock(user, item);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse removeStockByProductName(String productName, Integer quantity) {
        User user = userService.getCurrentUser();
        String resolved = resolveNearName(productName);
        InventoryItem item = inventoryRepository.findByUserIdAndProductNameContaining(user.getId(), resolved)
                .stream()
                .filter(i -> {
                    String n = i.getProduct().getName();
                    return n.equalsIgnoreCase(resolved)
                            || n.toLowerCase().contains(resolved.toLowerCase())
                            || FuzzyMatcher.similarity(
                            n.toLowerCase(Locale.ROOT), resolved.toLowerCase(Locale.ROOT)) >= 0.78;
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found in inventory: " + productName));

        if (item.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for " + item.getProduct().getName());
        }
        item.setQuantity(item.getQuantity() - quantity);
        item = inventoryRepository.save(item);
        recordTransaction(user, item.getProduct(), TransactionType.REMOVE, quantity);
        checkLowStock(user, item);
        return InventoryResponse.from(item);
    }

    /** Fuzzy + alias resolve against known products. */
    public String resolveNearName(String raw) {
        String aliased = ProductAliases.resolve(raw);
        Set<String> candidates = new HashSet<>(ProductAliases.knownNames());
        productRepository.findAll().forEach(p -> candidates.add(p.getName()));
        String best = FuzzyMatcher.bestMatch(aliased, candidates, 0.72);
        return best != null ? best : aliased;
    }

    @Transactional
    public InventoryResponse barcodeLookupOrCreate(String barcode, String productName, Integer quantity,
                                                    String unit, String category, String imageUrl) {
        User user = userService.getCurrentUser();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new RuntimeException("Product picture is required");
        }
        Product product = productRepository.findByBarcode(barcode).orElse(null);
        if (product == null) {
            if (productName == null || productName.isBlank()) {
                throw new RuntimeException("Product name required for new barcode");
            }
            product = productRepository.save(Product.builder()
                    .name(productName.trim())
                    .barcode(barcode)
                    .category(category != null ? category : "General")
                    .imageUrl(imageUrl)
                    .build());
        } else if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            product.setImageUrl(imageUrl);
            productRepository.save(product);
        }

        InventoryItem item = inventoryRepository.findByUserAndProduct(user, product)
                .orElse(InventoryItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .unit(normalizeUnit(unit))
                        .threshold(5)
                        .build());

        int addQty = quantity != null ? quantity : 0;
        if (addQty > 0) {
            item.setQuantity(item.getQuantity() + addQty);
            recordTransaction(user, product, TransactionType.ADD, addQty);
        }
        if (unit != null && !unit.isBlank()) {
            item.setUnit(normalizeUnit(unit));
        }
        item = inventoryRepository.save(item);
        checkLowStock(user, item);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse addProductImage(Long itemId, MultipartFile file) {
        User user = userService.getCurrentUser();
        InventoryItem item = getOwnedItem(itemId, user);
        String path = userService.storeFile(file, "products");
        Product product = item.getProduct();
        product.setImageUrl("/uploads/" + path);
        productRepository.save(product);
        return InventoryResponse.from(item);
    }

    @Transactional
    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Product picture is required");
        }
        String path = userService.storeFile(file, "products");
        return "/uploads/" + path;
    }

    @Transactional
    public void deleteItem(Long itemId) {
        User user = userService.getCurrentUser();
        InventoryItem item = getOwnedItem(itemId, user);
        inventoryRepository.delete(item);
    }

    @Transactional
    public void adjustStock(User user, Product product, int quantity, TransactionType type) {
        InventoryItem item = inventoryRepository.findByUserAndProduct(user, product)
                .orElse(InventoryItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .unit("pieces")
                        .threshold(5)
                        .build());

        if (type == TransactionType.REMOVE || type == TransactionType.SALE) {
            if (item.getQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock for " + product.getName());
            }
            item.setQuantity(item.getQuantity() - quantity);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
        inventoryRepository.save(item);
        recordTransaction(user, product, type, quantity);
        checkLowStock(user, item);
    }

    public Product findOrCreateProduct(String name, String barcode, String category, String imageUrl) {
        if (barcode != null && !barcode.isBlank()) {
            Product byBarcode = productRepository.findByBarcode(barcode).orElse(null);
            if (byBarcode != null) {
                if (imageUrl != null && !imageUrl.isBlank()
                        && (byBarcode.getImageUrl() == null || byBarcode.getImageUrl().isBlank())) {
                    byBarcode.setImageUrl(imageUrl);
                    return productRepository.save(byBarcode);
                }
                return byBarcode;
            }
        }
        return productRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> productRepository.save(Product.builder()
                        .name(name.trim())
                        .barcode(barcode)
                        .category(category != null ? category : "General")
                        .imageUrl(imageUrl)
                        .build()));
    }

    public static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "pieces";
        }
        String u = unit.trim().toLowerCase(Locale.ROOT);
        return switch (u) {
            case "kgs", "kilogram", "kilograms" -> "kg";
            case "grams", "gram" -> "g";
            case "litre", "litres", "liter", "liters" -> "L";
            case "packet", "pack", "packs" -> "packets";
            case "bottle" -> "bottles";
            case "piece", "pcs", "pc", "units", "unit", "items", "item" -> "pieces";
            case "bag" -> "bags";
            default -> u;
        };
    }

    private InventoryItem getOwnedItem(Long itemId, User user) {
        InventoryItem item = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return item;
    }

    private void recordTransaction(User user, Product product, TransactionType type, int quantity) {
        stockTransactionRepository.save(StockTransaction.builder()
                .user(user)
                .product(product)
                .transactionType(type)
                .quantity(quantity)
                .build());
    }

    private void checkLowStock(User user, InventoryItem item) {
        if (item.getQuantity() <= item.getThreshold()) {
            String unit = item.getUnit() != null ? item.getUnit() : "";
            alertService.createAlert(user, AlertType.LOW_STOCK,
                    "Low stock: " + item.getProduct().getName() + " (" + item.getQuantity() + " " + unit
                            + " left). Consider contacting nearby vendors.");
        }
    }

    public Map<String, Object> suggestVendorsMessage(String productName) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", "Low stock on " + productName + ". Try searching nearby vendors.");
        map.put("product", productName);
        return map;
    }
}
