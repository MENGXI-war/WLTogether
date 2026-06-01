package com.wltogether.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class CapabilitiesResponse {
    private String version;
    private Map<String, Boolean> transferModes;
    private Map<String, Object> limits;
}
