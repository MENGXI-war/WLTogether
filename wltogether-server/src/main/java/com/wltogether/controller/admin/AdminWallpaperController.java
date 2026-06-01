package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.Wallpaper;
import com.wltogether.service.WallpaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
                                             @RequestBody Map<String, Object> body) {
        Long userId = (Long) auth.getPrincipal();
        String imageUrl = (String) body.get("imageUrl");
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        LocalDate publishDate = body.get("publishDate") != null
                ? LocalDate.parse((String) body.get("publishDate"))
                : LocalDate.now();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wallpaperService.create(imageUrl, title, description, publishDate, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        wallpaperService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("壁纸已删除"));
    }
}
