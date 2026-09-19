package com.inventory.controller;

import com.inventory.dto.UserResponse;
import com.inventory.enums.Role;
import com.inventory.enums.UserStatus;
import com.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contacts for voice communication FAB (shopkeepers ↔ vendors).
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactDirectoryController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> listContacts() {
        var me = userService.getCurrentUser();
        Role target = me.getRole() == Role.VENDOR ? Role.SHOPKEEPER : Role.VENDOR;
        // Vendors can also talk to other vendors
        List<UserResponse> primary = userService.listByRole(target, null, null, UserStatus.ACTIVE);
        if (me.getRole() == Role.VENDOR) {
            List<UserResponse> otherVendors = userService.listByRole(Role.VENDOR, null, null, UserStatus.ACTIVE)
                    .stream()
                    .filter(u -> !u.getId().equals(me.getId()))
                    .collect(Collectors.toList());
            primary = primary.stream().collect(Collectors.toList());
            primary.addAll(otherVendors);
        }
        // Shopkeepers only see vendors
        return ResponseEntity.ok(primary.stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .filter(u -> !"assistant@voicestock.bot".equalsIgnoreCase(u.getEmail()))
                .collect(Collectors.toList()));
    }
}
