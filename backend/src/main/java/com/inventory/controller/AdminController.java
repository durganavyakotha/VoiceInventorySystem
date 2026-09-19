package com.inventory.controller;

import com.inventory.dto.QueryUpdateRequest;
import com.inventory.dto.UserResponse;
import com.inventory.enums.Role;
import com.inventory.enums.UserStatus;
import com.inventory.service.ContactService;
import com.inventory.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ContactService contactService;

    @GetMapping("/vendors")
    public ResponseEntity<List<UserResponse>> vendors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(userService.listByRole(Role.VENDOR, search, location, status, language));
    }

    @GetMapping("/shopkeepers")
    public ResponseEntity<List<UserResponse>> shopkeepers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(userService.listByRole(Role.SHOPKEEPER, search, location, status, language));
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Object>> counts() {
        return ResponseEntity.ok(userService.getCounts());
    }

    @GetMapping("/location/{location}")
    public ResponseEntity<Map<String, Long>> locationStats(@PathVariable String location) {
        return ResponseEntity.ok(userService.getLocationStats(location));
    }

    @GetMapping("/locations")
    public ResponseEntity<Map<String, Object>> locationBreakdown() {
        return ResponseEntity.ok(userService.getLocationBreakdown());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        return ResponseEntity.ok(userService.updateStatus(id, status));
    }

    @GetMapping("/queries")
    public ResponseEntity<List<Map<String, Object>>> queries() {
        return ResponseEntity.ok(contactService.listAll());
    }

    @PutMapping("/queries/{id}")
    public ResponseEntity<Map<String, Object>> updateQuery(
            @PathVariable Long id,
            @Valid @RequestBody QueryUpdateRequest request) {
        return ResponseEntity.ok(contactService.updateStatus(id, request));
    }
}
