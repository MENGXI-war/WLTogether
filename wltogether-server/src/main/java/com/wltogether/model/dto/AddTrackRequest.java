package com.wltogether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddTrackRequest {
    @NotBlank
    @Size(max = 64)
    private String fileHash;

    @Size(max = 500)
    private String fileName;

    private Integer durationSeconds;

    @Size(max = 200)
    private String artist;

    @Size(max = 200)
    private String album;
}
