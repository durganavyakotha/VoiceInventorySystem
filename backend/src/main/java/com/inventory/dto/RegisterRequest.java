package com.inventory.dto;

import com.inventory.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    /** Must be one of the predefined map locations. */
    private String location;

    private String phone;

    private String language;

    @NotNull
    private Role role;

    private String shopName;
}
