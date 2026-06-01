package com.wltogether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSessionRequest {
    @NotBlank
    private String fileHash;

    @NotBlank
    private String fileName;

    private Long fileSize;
    private Integer duration;

    @NotBlank
    private String mediaType;

    private String videoCodec;
    private String audioCodec;
    private String magnetUri;
    private String transferMode;
}
