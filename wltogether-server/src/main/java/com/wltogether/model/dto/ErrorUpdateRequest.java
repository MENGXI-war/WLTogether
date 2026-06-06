package com.wltogether.model.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ErrorUpdateRequest {
    @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED", message = "状态值无效，仅支持 OPEN/IN_PROGRESS/RESOLVED")
    private String status;

    @jakarta.validation.constraints.Size(max = 1000, message = "处理备注最长1000字符")
    private String resolutionNote;
}
