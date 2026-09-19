package com.inventory.service;

import com.inventory.dto.InventoryResponse;
import com.inventory.entity.InventoryItem;
import com.inventory.entity.User;
import com.inventory.enums.Role;
import com.inventory.enums.UserStatus;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.ShopRepository;
import com.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final UserService userService;

    public List<Map<String, Object>> listAllVendors() {
        User current = userService.getCurrentUser();
        List<User> vendors = userRepository.findByRoleAndStatus(Role.VENDOR, UserStatus.ACTIVE);
        List<Map<String, Object>> results = new ArrayList<>();

        for (User vendor : vendors) {
            if (vendor.getId().equals(current.getId())) {
                continue;
            }
            results.add(toVendorCard(vendor));
        }
        return results;
    }

    public Map<String, Object> getVendorWithItems(Long vendorId) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        if (vendor.getRole() != Role.VENDOR) {
            throw new RuntimeException("User is not a vendor");
        }
        Map<String, Object> card = toVendorCard(vendor);
        List<InventoryResponse> items = inventoryRepository.findByUserId(vendorId).stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
        card.put("items", items);
        return card;
    }

    private Map<String, Object> toVendorCard(User vendor) {
        Map<String, Object> row = new HashMap<>();
        row.put("vendorId", vendor.getId());
        row.put("firstName", vendor.getFirstName());
        row.put("lastName", vendor.getLastName());
        row.put("name", vendor.getFirstName() + " " + vendor.getLastName());
        row.put("email", vendor.getEmail());
        row.put("phone", vendor.getPhone() != null ? vendor.getPhone() : "—");
        row.put("location", vendor.getLocation());
        row.put("language", vendor.getLanguage());
        row.put("profileImageUrl", vendor.getProfileImageUrl());
        shopRepository.findByUser(vendor).ifPresent(shop -> row.put("shopName", shop.getShopName()));
        if (!row.containsKey("shopName")) {
            row.put("shopName", vendor.getFirstName() + "'s Shop");
        }
        List<InventoryItem> stock = inventoryRepository.findByUserId(vendor.getId());
        row.put("itemCount", stock.size());
        row.put("availableItemsSummary", stock.stream()
                .map(i -> i.getProduct().getName() + " (" + i.getQuantity()
                        + (i.getUnit() != null ? " " + i.getUnit() : "") + ")")
                .limit(5)
                .collect(Collectors.joining(", ")));
        return row;
    }

    public List<Map<String, Object>> searchNearby(String product, Double radiusKm, Integer minQty,
                                                   Double latitude, Double longitude) {
        User current = userService.getCurrentUser();
        double lat = latitude != null ? latitude : (current.getLatitude() != null ? current.getLatitude() : 15.735);
        double lon = longitude != null ? longitude : (current.getLongitude() != null ? current.getLongitude() : 79.270);
        double radius = radiusKm != null ? radiusKm : 10.0;
        int min = minQty != null ? minQty : 1;

        List<InventoryItem> stocks = inventoryRepository.findVendorStockByProductName(product, min);
        List<Map<String, Object>> results = new ArrayList<>();

        for (InventoryItem item : stocks) {
            User vendor = item.getUser();
            if (vendor.getId().equals(current.getId())) {
                continue;
            }
            if (vendor.getLatitude() == null || vendor.getLongitude() == null) {
                // Still include by same location name if no coords
                if (current.getLocation() != null && current.getLocation().equalsIgnoreCase(vendor.getLocation())) {
                    Map<String, Object> row = baseNearbyRow(vendor, item, 0.0);
                    results.add(row);
                }
                continue;
            }
            double distance = haversineKm(lat, lon, vendor.getLatitude(), vendor.getLongitude());
            if (distance <= radius) {
                results.add(baseNearbyRow(vendor, item, distance));
            }
        }

        results.sort((a, b) -> Double.compare((Double) a.get("distanceKm"), (Double) b.get("distanceKm")));
        return results;
    }

    private Map<String, Object> baseNearbyRow(User vendor, InventoryItem item, double distance) {
        Map<String, Object> row = new HashMap<>();
        row.put("vendorId", vendor.getId());
        row.put("firstName", vendor.getFirstName());
        row.put("lastName", vendor.getLastName());
        row.put("email", vendor.getEmail());
        row.put("phone", vendor.getPhone());
        row.put("location", vendor.getLocation());
        row.put("productName", item.getProduct().getName());
        row.put("availableQty", item.getQuantity());
        row.put("unit", item.getUnit());
        row.put("distanceKm", Math.round(distance * 100.0) / 100.0);
        return row;
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
