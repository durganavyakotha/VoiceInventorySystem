package com.inventory.config;

import com.inventory.entity.InventoryItem;
import com.inventory.entity.Product;
import com.inventory.entity.Shop;
import com.inventory.entity.User;
import com.inventory.enums.Role;
import com.inventory.enums.UserStatus;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.ShopRepository;
import com.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    private static final double MARKAPUR_LAT = 15.735;
    private static final double MARKAPUR_LON = 79.270;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedDemoUsers();
    }

    private void seedAdmin() {
        String adminEmail = "admin@admin.gmail.com";
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = userRepository.save(User.builder()
                .firstName("System")
                .lastName("Admin")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .location("Markapur")
                .language("en")
                .status(UserStatus.ACTIVE)
                .latitude(MARKAPUR_LAT)
                .longitude(MARKAPUR_LON)
                .build());
        log.info("Seeded default admin: {}", admin.getEmail());
    }

    private void seedDemoUsers() {
        if (userRepository.findByEmail("shop@demo.com").isPresent()) {
            return;
        }

        User shopkeeper = userRepository.save(User.builder()
                .firstName("Ravi")
                .lastName("Shop")
                .email("shop@demo.com")
                .passwordHash(passwordEncoder.encode("Shop@123"))
                .role(Role.SHOPKEEPER)
                .location("Markapur")
                .language("te")
                .status(UserStatus.ACTIVE)
                .latitude(MARKAPUR_LAT)
                .longitude(MARKAPUR_LON)
                .build());

        shopRepository.save(Shop.builder()
                .user(shopkeeper)
                .shopName("Ravi Kirana Store")
                .location("Markapur")
                .latitude(MARKAPUR_LAT)
                .longitude(MARKAPUR_LON)
                .build());

        User vendor = userRepository.save(User.builder()
                .firstName("Suresh")
                .lastName("Vendor")
                .email("vendor@demo.com")
                .passwordHash(passwordEncoder.encode("Vendor@123"))
                .role(Role.VENDOR)
                .location("Markapur")
                .language("te")
                .status(UserStatus.ACTIVE)
                .latitude(MARKAPUR_LAT + 0.01)
                .longitude(MARKAPUR_LON + 0.01)
                .build());

        shopRepository.save(Shop.builder()
                .user(vendor)
                .shopName("Suresh Wholesale")
                .location("Markapur")
                .latitude(vendor.getLatitude())
                .longitude(vendor.getLongitude())
                .build());

        Product rice = productRepository.save(Product.builder().name("Rice").category("Grains").build());
        Product oil = productRepository.save(Product.builder().name("Oil").category("Grocery").build());
        Product soap = productRepository.save(Product.builder().name("Soap").category("Personal Care").build());
        Product pepsi = productRepository.save(Product.builder().name("Pepsi").category("Beverages").barcode("8901234567890").build());

        seedInventory(shopkeeper, rice, 20, 10);
        seedInventory(shopkeeper, oil, 8, 5);
        seedInventory(shopkeeper, soap, 3, 5);
        seedInventory(shopkeeper, pepsi, 15, 10);

        seedInventory(vendor, rice, 200, 20);
        seedInventory(vendor, oil, 150, 20);
        seedInventory(vendor, soap, 100, 10);
        seedInventory(vendor, pepsi, 80, 10);

        log.info("Seeded demo shopkeeper shop@demo.com / Shop@123 and vendor vendor@demo.com / Vendor@123");
    }

    private void seedInventory(User user, Product product, int qty, int threshold) {
        inventoryRepository.save(InventoryItem.builder()
                .user(user)
                .product(product)
                .quantity(qty)
                .threshold(threshold)
                .build());
    }
}
