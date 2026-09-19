package com.inventory.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Predefined map locations. Users pick a city name; coordinates are applied server-side.
 */
public final class AppLocations {

    public record LocationOption(String name, double latitude, double longitude) {}

    public static final List<LocationOption> ALL = List.of(
            new LocationOption("Markapur", 15.7350, 79.2700),
            new LocationOption("Ongole", 15.5057, 80.0499),
            new LocationOption("Guntur", 16.3067, 80.4365),
            new LocationOption("Vijayawada", 16.5062, 80.6480),
            new LocationOption("Nellore", 14.4426, 79.9865),
            new LocationOption("Tirupati", 13.6288, 79.4192),
            new LocationOption("Kurnool", 15.8281, 78.0373),
            new LocationOption("Hyderabad", 17.3850, 78.4867),
            new LocationOption("Chennai", 13.0827, 80.2707),
            new LocationOption("Bangalore", 12.9716, 77.5946)
    );

    private AppLocations() {}

    public static Optional<LocationOption> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return ALL.stream()
                .filter(l -> l.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public static List<Map<String, Object>> asMaps() {
        return ALL.stream()
                .map(l -> Map.<String, Object>of(
                        "name", l.name(),
                        "latitude", l.latitude(),
                        "longitude", l.longitude()))
                .toList();
    }
}
