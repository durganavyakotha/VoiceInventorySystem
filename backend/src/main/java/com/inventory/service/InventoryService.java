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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public List<InventoryResponse> searchByProductName(String name) {
        User user = userService.getCurrentUser();
        return inventoryRepository.findByUserIdAndProductNameContaining(user.getId(), name).stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> listLowStock() {
        User user = userService.getCurrentUser();
        return inventoryRepository.findLowStockByUserId(user.getId()).stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryResponse addOrUpdate(InventoryRequest request) {
        User user = userService.getCurrentUser();
        Product product = findOrCreateProduct(request.getProductName(), request.getBarcode(),
                request.getCategory(), request.getImageUrl());

        InventoryItem item = inventoryRepository.findByUserAndProduct(user, product)
                .orElse(InventoryItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .threshold(request.getThreshold() != null ? request.getThreshold() : 5)
                        .build());

        int addQty = request.getQuantity() != null ? request.getQuantity() : 0;
        item.setQuantity(item.getQuantity() + addQty);
        if (request.getThreshold() != null) {
            item.setThreshold(request.getThreshold());
        }
        item = inventoryRepository.save(item);

        if (addQty > 0) {
            recordTransaction(user, product, TransactionType.ADD, addQty);
        }

        checkLowStock(user, item);
        return InventoryResponse.from(item);
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
            recordTransaction(user, item.getProduct(),
                    diff > 0 ? TransactionType.ADJUSTMENT : TransactionType.ADJUSTMENT,
                    Math.abs(diff));
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
    public InventoryResponse removeStockByProductName(String productName, Integer quantity) {
        User user = userService.getCurrentUser();
        InventoryItem item = inventoryRepository.findByUserIdAndProductNameContaining(user.getId(), productName)
                .stream()
                .filter(i -> i.getProduct().getName().equalsIgnoreCase(productName)
                        || i.getProduct().getName().toLowerCase().contains(productName.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found in inventory: " + productName));

        if (item.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for " + productName);
        }
        item.setQuantity(item.getQuantity() - quantity);
        item = inventoryRepository.save(item);
        recordTransaction(user, item.getProduct(), TransactionType.REMOVE, quantity);
        checkLowStock(user, item);
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse addStockByProductName(String productName, Integer quantity) {
        InventoryRequest request = new InventoryRequest();
        request.setProductName(productName);
        request.setQuantity(quantity);
        return addOrUpdate(request);
    }

    @Transactional
    public InventoryResponse barcodeLookupOrCreate(String barcode, String productName, Integer quantity) {
        User user = userService.getCurrentUser();
        Product product = productRepository.findByBarcode(barcode).orElse(null);
        if (product == null) {
            if (productName == null || productName.isBlank()) {
                throw new RuntimeException("Product name required for new barcode");
            }
            product = productRepository.save(Product.builder()
                    .name(productName.trim())
                    .barcode(barcode)
                    .build());
        }

        InventoryItem item = inventoryRepository.findByUserAndProduct(user, product)
                .orElse(InventoryItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .threshold(5)
                        .build());

        int addQty = quantity != null ? quantity : 0;
        if (addQty > 0) {
            item.setQuantity(item.getQuantity() + addQty);
            recordTransaction(user, product, TransactionType.ADD, addQty);
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
                return byBarcode;
            }
        }
        return productRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> productRepository.save(Product.builder()
                        .name(name.trim())
                        .barcode(barcode)
                        .category(category)
                        .imageUrl(imageUrl)
                        .build()));
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
            alertService.createAlert(user, AlertType.LOW_STOCK,
                    "Low stock: " + item.getProduct().getName() + " (" + item.getQuantity() + " left). Consider contacting nearby vendors.");
        }
    }

    public Map<String, Object> suggestVendorsMessage(String productName) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", "Low stock on " + productName + ". Try searching nearby vendors.");
        map.put("product", productName);
        return map;
    }
}
