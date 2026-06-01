package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.Announcement;
import com.wltogether.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<List<Announcement>> list(@RequestParam(defaultValue = "false") boolean publishedOnly) {
        return ResponseEntity.ok(announcementService.listAnnouncements(publishedOnly));
    }

    @PostMapping
    public ResponseEntity<Announcement> create(Authentication auth,
                                                @RequestBody Map<String, Object> body) {
        Long userId = (Long) auth.getPrincipal();
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        boolean isPinned = Boolean.TRUE.equals(body.get("isPinned"));
        Instant expiredAt = body.get("expiredAt") != null
                ? Instant.parse((String) body.get("expiredAt")) : null;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(announcementService.create(title, content, userId, isPinned, expiredAt));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Announcement> update(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Boolean isPinned = body.containsKey("isPinned") ? (Boolean) body.get("isPinned") : null;
        Boolean isPublished = body.containsKey("isPublished") ? (Boolean) body.get("isPublished") : null;
        Instant expiredAt = body.get("expiredAt") != null
                ? Instant.parse((String) body.get("expiredAt")) : null;

        return ResponseEntity.ok(announcementService.update(id, title, content, isPinned, isPublished, expiredAt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("公告已删除"));
    }
}
