package com.inventory.service;

import com.inventory.dto.OrderOfferRequest;
import com.inventory.dto.OrderRequest;
import com.inventory.entity.OrderEntity;
import com.inventory.entity.Product;
import com.inventory.entity.User;
import com.inventory.enums.AlertType;
import com.inventory.enums.OrderStatus;
import com.inventory.enums.Role;
import com.inventory.enums.TransactionType;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final AlertService alertService;

    @Transactional
    public Map<String, Object> createOrder(OrderRequest request) {
        User shopkeeper = userService.getCurrentUser();
        if (shopkeeper.getRole() != Role.SHOPKEEPER && shopkeeper.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only shopkeepers can create orders");
        }
        User vendor = userService.getUserById(request.getVendorId());
        if (vendor.getRole() != Role.VENDOR) {
            throw new RuntimeException("Target user is not a vendor");
        }

        OrderEntity order = OrderEntity.builder()
                .shopkeeper(shopkeeper)
                .vendor(vendor)
                .status(OrderStatus.PENDING)
                .productName(request.getProductName().trim())
                .quantity(request.getQuantity())
                .deliveryWithinDays(request.getDeliveryWithinDays())
                .expectedDeliveryDate(request.getDeliveryWithinDays() != null
                        ? LocalDate.now().plusDays(request.getDeliveryWithinDays())
                        : null)
                .build();
        order = orderRepository.save(order);

        alertService.createAlert(vendor, AlertType.NEW_ORDER,
                "New order from " + shopkeeper.getFirstName() + " for " + order.getQuantity()
                        + " " + order.getProductName());

        return toMap(order);
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrders() {
        User user = userService.getCurrentUser();
        List<OrderEntity> orders;
        if (user.getRole() == Role.VENDOR) {
            orders = orderRepository.findByVendorIdOrderByRequestedAtDesc(user.getId());
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
        return updateAsVendor(orderId, OrderStatus.REJECTED, "Order rejected");
    }

    @Transactional
    public Map<String, Object> withdraw(Long orderId) {
        User user = userService.getCurrentUser();
        OrderEntity order = getOrder(orderId);
        if (!order.getShopkeeper().getId().equals(user.getId())) {
            throw new RuntimeException("Only shopkeeper can withdraw order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be withdrawn");
        }
        order.setStatus(OrderStatus.WITHDRAWN);
        order = orderRepository.save(order);
        alertService.createAlert(order.getVendor(), AlertType.ORDER_UPDATE,
                "Order #" + order.getId() + " was withdrawn");
        return toMap(order);
    }

    @Transactional
    public Map<String, Object> cancel(Long orderId) {
        User user = userService.getCurrentUser();
        OrderEntity order = getOrder(orderId);
        boolean isShopkeeper = order.getShopkeeper().getId().equals(user.getId());
        boolean isVendor = order.getVendor().getId().equals(user.getId());
        if (!isShopkeeper && !isVendor && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel delivered order");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        User notify = isShopkeeper ? order.getVendor() : order.getShopkeeper();
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
        if (!order.getVendor().getId().equals(user.getId())) {
            alertService.createAlert(order.getVendor(), AlertType.ORDER_UPDATE,
                    "Order #" + order.getId() + " status: " + status);
        }

        return toMap(order);
    }

    private void onDelivered(OrderEntity order) {
        Product product = productRepository.findByNameIgnoreCase(order.getProductName())
                .orElseGet(() -> productRepository.save(Product.builder()
                        .name(order.getProductName())
                        .build()));

        inventoryService.adjustStock(order.getShopkeeper(), product, order.getQuantity(), TransactionType.RESTOCK);
        try {
            inventoryService.adjustStock(order.getVendor(), product, order.getQuantity(), TransactionType.REMOVE);
        } catch (RuntimeException e) {
            // Vendor may not have tracked inventory for this product; continue
        }
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
}
