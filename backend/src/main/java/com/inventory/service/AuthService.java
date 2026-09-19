package com.inventory.service;

import com.inventory.config.AppLocations;
import com.inventory.dto.AuthResponse;
import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.dto.UserResponse;
import com.inventory.entity.Shop;
import com.inventory.entity.User;
import com.inventory.enums.Role;
import com.inventory.enums.UserStatus;
import com.inventory.repository.ShopRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ADMIN_EMAIL_PATTERN = "(?i).*@admin\\.gmail\\.com$";

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        validateAdminEmail(email, request.getRole());

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        AppLocations.LocationOption loc = resolveLocation(request.getLocation());

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .location(loc != null ? loc.name() : request.getLocation())
                .phone(request.getPhone())
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .status(UserStatus.ACTIVE)
                .latitude(loc != null ? loc.latitude() : null)
                .longitude(loc != null ? loc.longitude() : null)
                .build();

        user = userRepository.save(user);

        if (request.getShopName() != null && !request.getShopName().isBlank()) {
            Shop shop = Shop.builder()
                    .user(user)
                    .shopName(request.getShopName().trim())
                    .location(user.getLocation())
                    .latitude(user.getLatitude())
                    .longitude(user.getLongitude())
                    .build();
            shopRepository.save(shop);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.from(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new RuntimeException("Account is blocked");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.from(user))
                .build();
    }

    private AppLocations.LocationOption resolveLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        return AppLocations.findByName(location)
                .orElseThrow(() -> new RuntimeException(
                        "Please select a location from the list"));
    }

    private void validateAdminEmail(String email, Role role) {
        boolean isAdminEmail = email.matches(ADMIN_EMAIL_PATTERN);
        if (role == Role.ADMIN && !isAdminEmail) {
            throw new RuntimeException("Admin email must match pattern *@admin.gmail.com");
        }
        if (role != Role.ADMIN && isAdminEmail) {
            throw new RuntimeException("Only ADMIN role can use *@admin.gmail.com emails");
        }
    }
}
