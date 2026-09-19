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
                .phone("9000000001")
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
                .phone("9876543210")
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
                .phone("9876543211")
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

        User vendor2 = userRepository.save(User.builder()
                .firstName("Lakshmi")
                .lastName("Traders")
                .email("vendor2@demo.com")
                .passwordHash(passwordEncoder.encode("Vendor@123"))
                .role(Role.VENDOR)
                .location("Ongole")
                .phone("9876543212")
                .language("hi")
                .status(UserStatus.ACTIVE)
                .latitude(15.5057)
                .longitude(80.0499)
                .build());

        shopRepository.save(Shop.builder()
                .user(vendor2)
                .shopName("Lakshmi Traders")
                .location("Ongole")
                .latitude(15.5057)
                .longitude(80.0499)
                .build());

        User vendor3 = userRepository.save(User.builder()
                .firstName("Anil")
                .lastName("Mart")
                .email("vendor3@demo.com")
                .passwordHash(passwordEncoder.encode("Vendor@123"))
                .role(Role.VENDOR)
                .location("Guntur")
                .phone("9876543213")
                .language("en")
                .status(UserStatus.ACTIVE)
                .latitude(16.3067)
                .longitude(80.4365)
                .build());

        shopRepository.save(Shop.builder()
                .user(vendor3)
                .shopName("Anil Mart")
                .location("Guntur")
                .latitude(16.3067)
                .longitude(80.4365)
                .build());

        Product rice = productRepository.save(Product.builder().name("Rice").category("Grains")
                .imageUrl("/uploads/products/placeholder.svg").build());
        Product oil = productRepository.save(Product.builder().name("Oil").category("Grocery")
                .imageUrl("/uploads/products/placeholder.svg").build());
        Product soap = productRepository.save(Product.builder().name("Soap").category("Personal Care")
                .imageUrl("/uploads/products/placeholder.svg").build());
        Product pepsi = productRepository.save(Product.builder().name("Pepsi").category("Beverages")
                .barcode("8901234567890").imageUrl("/uploads/products/placeholder.svg").build());
        Product biscuits = productRepository.save(Product.builder().name("Biscuits").category("Snacks")
                .imageUrl("/uploads/products/placeholder.svg").build());

        seedInventory(shopkeeper, rice, 20, 10, "kg");
        seedInventory(shopkeeper, oil, 8, 5, "L");
        seedInventory(shopkeeper, soap, 3, 5, "pieces");
        seedInventory(shopkeeper, pepsi, 15, 10, "bottles");

        seedInventory(vendor, rice, 200, 20, "kg");
        seedInventory(vendor, oil, 150, 20, "L");
        seedInventory(vendor, soap, 100, 10, "pieces");
        seedInventory(vendor, pepsi, 80, 10, "bottles");
        seedInventory(vendor, biscuits, 120, 20, "packets");

        seedInventory(vendor2, rice, 300, 30, "kg");
        seedInventory(vendor2, oil, 90, 15, "L");
        seedInventory(vendor2, biscuits, 200, 25, "packets");

        seedInventory(vendor3, rice, 150, 20, "kg");
        seedInventory(vendor3, soap, 80, 10, "pieces");
        seedInventory(vendor3, pepsi, 60, 10, "bottles");

        log.info("Seeded demo shopkeeper shop@demo.com / Shop@123 and vendors vendor@demo.com / Vendor@123");
    }

    private void seedInventory(User user, Product product, int qty, int threshold, String unit) {
        inventoryRepository.save(InventoryItem.builder()
                .user(user)
                .product(product)
                .quantity(qty)
                .unit(unit)
                .threshold(threshold)
                .build());
    }
}
