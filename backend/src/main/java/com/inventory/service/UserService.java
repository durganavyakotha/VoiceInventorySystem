package com.inventory.service;

import com.inventory.config.AppLocations;
import com.inventory.dto.ProfileUpdateRequest;
import com.inventory.dto.UserResponse;
import com.inventory.entity.Shop;
import com.inventory.entity.User;
import com.inventory.enums.Role;
import com.inventory.enums.UserStatus;
import com.inventory.repository.ShopRepository;
import com.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserResponse getProfile() {
        User user = getCurrentUser();
        UserResponse response = UserResponse.from(user);
        shopRepository.findByUser(user).ifPresent(shop -> response.setShopName(shop.getShopName()));
        return response;
    }

    @Transactional
    public UserResponse updateProfile(ProfileUpdateRequest request) {
        User user = getCurrentUser();
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getLocation() != null) {
            AppLocations.LocationOption loc = AppLocations.findByName(request.getLocation())
                    .orElseThrow(() -> new RuntimeException("Please select a location from the map list"));
            user.setLocation(loc.name());
            user.setLatitude(loc.latitude());
            user.setLongitude(loc.longitude());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getLanguage() != null) {
            user.setLanguage(request.getLanguage());
        }
        user = userRepository.save(user);

        if (request.getShopName() != null && !request.getShopName().isBlank()) {
            Shop shop = shopRepository.findByUser(user).orElse(null);
            if (shop == null) {
                shop = Shop.builder()
                        .user(user)
                        .shopName(request.getShopName().trim())
                        .location(user.getLocation())
                        .latitude(user.getLatitude())
                        .longitude(user.getLongitude())
                        .build();
            } else {
                shop.setShopName(request.getShopName().trim());
                shop.setLocation(user.getLocation());
                shop.setLatitude(user.getLatitude());
                shop.setLongitude(user.getLongitude());
            }
            shopRepository.save(shop);
        }

        UserResponse response = UserResponse.from(user);
        shopRepository.findByUser(user).ifPresent(shop -> response.setShopName(shop.getShopName()));
        return response;
    }

    @Transactional
    public UserResponse uploadProfileImage(MultipartFile file) {
        User user = getCurrentUser();
        String path = storeFile(file, "profiles");
        user.setProfileImageUrl("/uploads/" + path);
        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public List<UserResponse> listByRole(Role role, String search, String location, UserStatus status) {
        return listByRole(role, search, location, status, null);
    }

    public List<UserResponse> listByRole(Role role, String search, String location, UserStatus status, String language) {
        String langCode = com.inventory.util.LanguageNames.toCode(language);
        return userRepository.searchUsers(role, blankToNull(location), status, blankToNull(search), blankToNull(langCode))
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getCounts() {
        Map<String, Object> counts = new HashMap<>();
        counts.put("vendors", userRepository.countByRole(Role.VENDOR));
        counts.put("shopkeepers", userRepository.countByRole(Role.SHOPKEEPER));
        counts.put("admins", userRepository.countByRole(Role.ADMIN));
        counts.put("total", userRepository.count());
        return counts;
    }

    public Map<String, Long> getLocationStats(String location) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("vendors", userRepository.countByRoleAndLocation(Role.VENDOR, location));
        stats.put("shopkeepers", userRepository.countByRoleAndLocation(Role.SHOPKEEPER, location));
        return stats;
    }

    public Map<String, Object> getLocationBreakdown() {
        Map<String, Object> result = new HashMap<>();
        result.put("vendors", toLocationMap(userRepository.countByRoleGroupedByLocation(Role.VENDOR)));
        result.put("shopkeepers", toLocationMap(userRepository.countByRoleGroupedByLocation(Role.SHOPKEEPER)));
        return result;
    }

    @Transactional
    public UserResponse updateStatus(Long userId, UserStatus status) {
        User user = getUserById(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Cannot change status of admin users");
        }
        user.setStatus(status);
        return UserResponse.from(userRepository.save(user));
    }

    public String storeFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        try {
            Path dir = Paths.get(uploadDir, subDir);
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) {
                ext = original.substring(dot);
            }
            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return subDir + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    private Map<String, Long> toLocationMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            String loc = row[0] != null ? row[0].toString() : "Unknown";
            Long count = (Long) row[1];
            map.put(loc, count);
        }
        return map;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
