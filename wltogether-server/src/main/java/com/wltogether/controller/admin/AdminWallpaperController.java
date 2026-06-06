package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.dto.WallpaperRequest;
import com.wltogether.model.entity.Wallpaper;
import com.wltogether.service.WallpaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/wallpapers")
@RequiredArgsConstructor
public class AdminWallpaperController {

    private final WallpaperService wallpaperService;

    @GetMapping
    public ResponseEntity<List<Wallpaper>> list() {
        return ResponseEntity.ok(wallpaperService.listWallpapers());
    }

    @PostMapping
    public ResponseEntity<Wallpaper> create(Authentication auth,
                                             @Valid @RequestBody WallpaperRequest body) {
        Long userId = (Long) auth.getPrincipal();
        LocalDate publishDate = body.getPublishDate() != null
                ? LocalDate.parse(body.getPublishDate())
                : LocalDate.now();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wallpaperService.create(body.getImageUrl(), body.getTitle(),
                        body.getDescription(), publishDate, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Wallpaper> update(@PathVariable Long id,
                                             @RequestBody WallpaperRequest body) {
        LocalDate publishDate = body.getPublishDate() != null
                ? LocalDate.parse(body.getPublishDate())
                : null;

        return ResponseEntity.ok(wallpaperService.update(id, body.getImageUrl(), body.getTitle(),
                body.getDescription(), publishDate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        wallpaperService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("壁纸已删除"));
    }
}
