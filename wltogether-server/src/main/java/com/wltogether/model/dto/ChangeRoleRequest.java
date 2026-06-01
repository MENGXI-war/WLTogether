package com.wltogether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    @NotBlank
    @Pattern(regexp = "ADMIN|MEMBER")
    private String role;
}
