package com.wltogether.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteMemberRequest {
    private String username;
    private String uid;
}
