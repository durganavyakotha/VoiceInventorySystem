package com.inventory.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    private String firstName;
    private String lastName;
    private String location;
    private String language;
    private Double latitude;
    private Double longitude;
    private String shopName;
}
