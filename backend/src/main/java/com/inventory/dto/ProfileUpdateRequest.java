package com.inventory.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    private String firstName;
    private String lastName;
    /** Must be one of the predefined map locations. */
    private String location;
    private String phone;
    private String language;
    private String shopName;
}
