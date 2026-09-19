package com.inventory.service;

import com.inventory.entity.InventoryItem;
import com.inventory.entity.User;
import com.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final InventoryRepository inventoryRepository;
    private final UserService userService;

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
                continue;
            }
            double distance = haversineKm(lat, lon, vendor.getLatitude(), vendor.getLongitude());
            if (distance <= radius) {
                Map<String, Object> row = new HashMap<>();
                row.put("vendorId", vendor.getId());
                row.put("firstName", vendor.getFirstName());
                row.put("lastName", vendor.getLastName());
                row.put("email", vendor.getEmail());
                row.put("location", vendor.getLocation());
                row.put("latitude", vendor.getLatitude());
                row.put("longitude", vendor.getLongitude());
                row.put("productName", item.getProduct().getName());
                row.put("availableQty", item.getQuantity());
                row.put("distanceKm", Math.round(distance * 100.0) / 100.0);
                results.add(row);
            }
        }

        results.sort((a, b) -> Double.compare((Double) a.get("distanceKm"), (Double) b.get("distanceKm")));
        return results;
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
