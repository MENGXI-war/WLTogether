package com.wltogether.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateNicknameRequest {
    @Size(min = 1, max = 50)
    private String nicknameInGroup;
}
