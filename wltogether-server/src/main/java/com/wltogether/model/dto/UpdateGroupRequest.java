package com.wltogether.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateGroupRequest {
    @Size(min = 1, max = 100)
    private String name;

    @Size(max = 500)
    private String description;
}
