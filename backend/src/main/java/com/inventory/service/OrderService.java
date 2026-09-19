package com.inventory.service;

import com.inventory.dto.OrderOfferRequest;
import com.inventory.dto.OrderRequest;
import com.inventory.entity.InventoryItem;
import com.inventory.entity.OrderEntity;
import com.inventory.entity.Product;
import com.inventory.entity.User;
import com.inventory.enums.AlertType;
import com.inventory.enums.OrderStatus;
import com.inventory.enums.Role;
import com.inventory.enums.TransactionType;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final AlertService alertService;

    @Transactional
    public Map<String, Object> createOrder(OrderRequest request) {
        User buyer = userService.getCurrentUser();
        if (buyer.getRole() != Role.SHOPKEEPER
                && buyer.getRole() != Role.VENDOR
                && buyer.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only shopkeepers or vendors can create orders");
        }
        User vendor = userService.getUserById(request.getVendorId());
        if (vendor.getRole() != Role.VENDOR) {
            throw new RuntimeException("Target user is not a vendor");
        }
        if (vendor.getId().equals(buyer.getId())) {
            throw new RuntimeException("Cannot book from yourself");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be positive");
        }

        // Deduct from vendor listed stock immediately
        InventoryItem vendorItem = findVendorItem(vendor.getId(), request.getProductName());
        if (vendorItem.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient vendor stock. Available: " + vendorItem.getQuantity());
        }
        inventoryService.adjustStock(vendor, vendorItem.getProduct(), request.getQuantity(), TransactionType.REMOVE);

        OrderEntity order = OrderEntity.builder()
                .shopkeeper(buyer)
                .vendor(vendor)
                .status(OrderStatus.PENDING)
                .productName(request.getProductName().trim())
                .quantity(request.getQuantity())
                .deliveryWithinDays(request.getDeliveryWithinDays() != null ? request.getDeliveryWithinDays() : 2)
                .expectedDeliveryDate(LocalDate.now().plusDays(
                        request.getDeliveryWithinDays() != null ? request.getDeliveryWithinDays() : 2))
                .build();
        order = orderRepository.save(order);

        alertService.createAlert(vendor, AlertType.NEW_ORDER,
                "New order from " + buyer.getFirstName() + " for " + order.getQuantity()
                        + " " + order.getProductName());

        // Refresh remaining after deduct
        InventoryItem updated = findVendorItem(vendor.getId(), request.getProductName());
        Map<String, Object> map = toMap(order);
        map.put("vendorRemainingQty", updated.getQuantity());
        return map;
    }

    @Transactional
    public Map<String, Object> createFromOffer(OrderOfferRequest request) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setVendorId(request.getVendorId());
        orderRequest.setProductName(request.getProductName());
        orderRequest.setQuantity(request.getQuantity());
        orderRequest.setDeliveryWithinDays(request.getDeliveryWithinDays());
        return createOrder(orderRequest);
    }

    /** Voice: "book 10 kg rice" / "book rice 10" while viewing a vendor. */
    @Transactional
    public Map<String, Object> voiceBook(Long vendorId, String command) {
        ParsedBook parsed = parseBookCommand(command);
        if (parsed == null) {
            throw new RuntimeException("Say e.g. book 10 kg rice or book rice 10 packets");
        }
        OrderRequest req = new OrderRequest();
        req.setVendorId(vendorId);
        req.setProductName(parsed.product);
        req.setQuantity(parsed.qty);
        req.setDeliveryWithinDays(2);
        Map<String, Object> result = createOrder(req);
        result.put("spokenResponse", "Booked " + parsed.qty + " " + parsed.product
                + ". Order #" + result.get("id"));
        result.put("action", "BOOK_ORDER");
        result.put("success", true);
        return result;
    }

    /** Voice: "withdraw rice" / "withdraw order 12" / "cancel book rice" — restore vendor stock. */
    @Transactional
    public Map<String, Object> voiceWithdraw(Long vendorId, String command) {
        User buyer = userService.getCurrentUser();
        String normalized = command.toLowerCase(Locale.ROOT);
        Long orderId = extractOrderId(normalized);
        OrderEntity order;
        if (orderId != null) {
            order = getOrder(orderId);
        } else {
            String product = extractProductForWithdraw(command);
            List<OrderEntity> pending = orderRepository.findByShopkeeperIdOrderByRequestedAtDesc(buyer.getId())
                    .stream()
                    .filter(o -> o.getStatus() == OrderStatus.PENDING)
                    .filter(o -> vendorId == null || o.getVendor().getId().equals(vendorId))
                    .filter(o -> product == null || o.getProductName().toLowerCase(Locale.ROOT).contains(product))
                    .toList();
            if (pending.isEmpty()) {
                throw new RuntimeException("No pending order found to withdraw");
            }
            order = pending.get(0);
        }
        Map<String, Object> result = withdraw(order.getId());
        result.put("spokenResponse", "Order #" + order.getId() + " withdrawn. "
                + order.getQuantity() + " " + order.getProductName() + " restored to vendor stock.");
        result.put("action", "WITHDRAW_ORDER");
        result.put("success", true);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrders() {
        User user = userService.getCurrentUser();
        List<OrderEntity> orders;
        if (user.getRole() == Role.VENDOR) {
            List<OrderEntity> incoming = orderRepository.findByVendorIdOrderByRequestedAtDesc(user.getId());
            List<OrderEntity> outgoing = orderRepository.findByShopkeeperIdOrderByRequestedAtDesc(user.getId());
            Map<Long, OrderEntity> merged = new HashMap<>();
            for (OrderEntity o : incoming) merged.put(o.getId(), o);
            for (OrderEntity o : outgoing) merged.put(o.getId(), o);
            orders = merged.values().stream()
                    .sorted((a, b) -> b.getRequestedAt().compareTo(a.getRequestedAt()))
                    .collect(Collectors.toList());
        } else if (user.getRole() == Role.SHOPKEEPER) {
            orders = orderRepository.findByShopkeeperIdOrderByRequestedAtDesc(user.getId());
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream().map(this::toMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> accept(Long orderId) {
        return updateAsVendor(orderId, OrderStatus.ACCEPTED, "Order accepted");
    }

    @Transactional
    public Map<String, Object> reject(Long orderId) {
        OrderEntity order = getOrder(orderId);
        User user = userService.getCurrentUser();
        if (!order.getVendor().getId().equals(user.getId())) {
            throw new RuntimeException("Only vendor can reject");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new RuntimeException("Cannot reject this order");
        }
        restoreVendorStock(order);
        order.setStatus(OrderStatus.REJECTED);
        order = orderRepository.save(order);
        alertService.createAlert(order.getShopkeeper(), AlertType.ORDER_UPDATE,
                "Order #" + order.getId() + " was rejected — stock restored");
        return toMap(order);
    }

    @Transactional
    public Map<String, Object> withdraw(Long orderId) {
        User user = userService.getCurrentUser();
        OrderEntity order = getOrder(orderId);
        if (!order.getShopkeeper().getId().equals(user.getId())) {
            throw new RuntimeException("Only the buyer can withdraw order");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new RuntimeException("Only pending/accepted orders can be withdrawn");
        }
        restoreVendorStock(order);
        order.setStatus(OrderStatus.WITHDRAWN);
        order = orderRepository.save(order);
        alertService.createAlert(order.getVendor(), AlertType.ORDER_UPDATE,
                "Order #" + order.getId() + " was withdrawn — quantity restored");
        return toMap(order);
    }

    @Transactional
    public Map<String, Object> cancel(Long orderId) {
        User user = userService.getCurrentUser();
        OrderEntity order = getOrder(orderId);
        boolean isBuyer = order.getShopkeeper().getId().equals(user.getId());
        boolean isVendor = order.getVendor().getId().equals(user.getId());
        if (!isBuyer && !isVendor && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.WITHDRAWN) {
            throw new RuntimeException("Cannot cancel this order");
        }
        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.ACCEPTED) {
            restoreVendorStock(order);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        User notify = isBuyer ? order.getVendor() : order.getShopkeeper();
        alertService.createAlert(notify, AlertType.ORDER_UPDATE,
                "Order #" + order.getId() + " was cancelled");
        return toMap(order);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long orderId, OrderStatus status) {
        User user = userService.getCurrentUser();
        OrderEntity order = getOrder(orderId);

        if (user.getRole() == Role.VENDOR && !order.getVendor().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        if (user.getRole() == Role.SHOPKEEPER && !order.getShopkeeper().getId().equals(user.getId())
                && status != OrderStatus.CANCELLED && status != OrderStatus.WITHDRAWN) {
            throw new RuntimeException("Access denied");
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(status);
        if (status == OrderStatus.OUT_FOR_DELIVERY && order.getExpectedDeliveryDate() == null
                && order.getDeliveryWithinDays() != null) {
            order.setExpectedDeliveryDate(LocalDate.now().plusDays(order.getDeliveryWithinDays()));
        }
        order = orderRepository.save(order);

        if (status == OrderStatus.DELIVERED && previous != OrderStatus.DELIVERED) {
            onDelivered(order);
        }

        alertService.createAlert(order.getShopkeeper(), AlertType.ORDER_UPDATE,
                "Order #" + order.getId() + " status: " + status);
        return toMap(order);
    }

    /** On deliver: add stock to buyer (vendor already deducted at book). */
    private void onDelivered(OrderEntity order) {
        Product product = productRepository.findByNameIgnoreCase(order.getProductName())
                .orElseGet(() -> productRepository.save(Product.builder()
                        .name(order.getProductName())
                        .imageUrl("/uploads/products/placeholder.svg")
                        .category("General")
                        .build()));
        inventoryService.adjustStock(order.getShopkeeper(), product, order.getQuantity(), TransactionType.RESTOCK);
    }

    private void restoreVendorStock(OrderEntity order) {
        Product product = productRepository.findByNameIgnoreCase(order.getProductName())
                .orElse(null);
        if (product == null) {
            return;
        }
        inventoryService.adjustStock(order.getVendor(), product, order.getQuantity(), TransactionType.RESTOCK);
    }

    private InventoryItem findVendorItem(Long vendorId, String productName) {
        return inventoryRepository.findByUserIdAndProductNameContaining(vendorId, productName.trim())
                .stream()
                .filter(i -> i.getProduct().getName().equalsIgnoreCase(productName.trim())
                        || i.getProduct().getName().toLowerCase(Locale.ROOT)
                        .contains(productName.trim().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found in vendor inventory: " + productName));
    }

    private Map<String, Object> updateAsVendor(Long orderId, OrderStatus status, String msg) {
        User user = userService.getCurrentUser();
        OrderEntity order = getOrder(orderId);
        if (!order.getVendor().getId().equals(user.getId())) {
            throw new RuntimeException("Only vendor can " + status.name().toLowerCase());
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not pending");
        }
        order.setStatus(status);
        order = orderRepository.save(order);
        alertService.createAlert(order.getShopkeeper(), AlertType.ORDER_UPDATE,
                "Order #" + order.getId() + ": " + msg);
        return toMap(order);
    }

    private OrderEntity getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    private ParsedBook parseBookCommand(String command) {
        Matcher m = Pattern.compile(
                "(?i)(?:book|order|book\\s*cheyyi)\\s+(\\d+)\\s*(?:kgs?|packets?|bottles?|pieces?|bags?|l|ml|g)?\\s+([a-zA-Z][\\w\\s]*)")
                .matcher(command);
        if (m.find()) {
            return new ParsedBook(Integer.parseInt(m.group(1)), m.group(2).trim());
        }
        m = Pattern.compile("(?i)(?:book|order)\\s+([a-zA-Z][\\w\\s]*?)\\s+(\\d+)").matcher(command);
        if (m.find()) {
            return new ParsedBook(Integer.parseInt(m.group(2)), m.group(1).trim());
        }
        return null;
    }

    private Long extractOrderId(String normalized) {
        Matcher m = Pattern.compile("(?:order\\s*#?|#)\\s*(\\d+)").matcher(normalized);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return null;
    }

    private String extractProductForWithdraw(String command) {
        Matcher m = Pattern.compile("(?i)(?:withdraw|cancel)\\s+(?:order\\s+)?(?:for\\s+)?([a-zA-Z][\\w\\s]*)")
                .matcher(command);
        if (m.find()) {
            return m.group(1).replaceAll("(?i)\\border\\b", "").trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private Map<String, Object> toMap(OrderEntity order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("shopkeeperId", order.getShopkeeper().getId());
        map.put("shopkeeperName", order.getShopkeeper().getFirstName() + " " + order.getShopkeeper().getLastName());
        map.put("vendorId", order.getVendor().getId());
        map.put("vendorName", order.getVendor().getFirstName() + " " + order.getVendor().getLastName());
        map.put("status", order.getStatus());
        map.put("productName", order.getProductName());
        map.put("quantity", order.getQuantity());
        map.put("deliveryWithinDays", order.getDeliveryWithinDays());
        map.put("requestedAt", order.getRequestedAt());
        map.put("expectedDeliveryDate", order.getExpectedDeliveryDate());
        map.put("updatedAt", order.getUpdatedAt());
        return map;
    }

    private record ParsedBook(int qty, String product) {}
}
