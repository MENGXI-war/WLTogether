package com.wltogether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * duration: "1h" / "8h" / "forever" / "none" (remove mute)
 */
@Data
public class MuteMemberRequest {
    @NotBlank
    @Pattern(regexp = "1h|8h|forever|none")
    private String duration;
}
