package com.wltogether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WallpaperRequest {
    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    @Size(max = 200, message = "标题最长200字符")
    private String title;

    private String description;
    private String publishDate;
}
