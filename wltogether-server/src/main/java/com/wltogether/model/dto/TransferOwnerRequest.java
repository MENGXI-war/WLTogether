package com.wltogether.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferOwnerRequest {
    @NotNull
    private Long newOwnerId;
}
