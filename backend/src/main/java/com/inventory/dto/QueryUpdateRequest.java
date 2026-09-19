package com.inventory.dto;

import com.inventory.enums.QueryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QueryUpdateRequest {

    @NotNull
    private QueryStatus status;
}
