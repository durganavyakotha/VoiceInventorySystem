package com.inventory.controller;

import com.inventory.dto.ProfileUpdateRequest;
import com.inventory.dto.UserResponse;
import com.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.ok(userService.getProfile());
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(@RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PostMapping("/image")
    public ResponseEntity<UserResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadProfileImage(file));
    }
}
