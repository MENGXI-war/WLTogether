package com.wltogether.controller.admin;

import com.wltogether.model.dto.AnnouncementRequest;
import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.Announcement;
import com.wltogether.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

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
                                                @Valid @RequestBody AnnouncementRequest body) {
        Long userId = (Long) auth.getPrincipal();
        Instant expiredAt = body.getExpiredAt() != null
                ? Instant.parse(body.getExpiredAt()) : null;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(announcementService.create(body.getTitle(), body.getContent(),
                        userId, Boolean.TRUE.equals(body.getIsPinned()), expiredAt));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Announcement> update(@PathVariable Long id,
                                                @RequestBody AnnouncementRequest body) {
        Instant expiredAt = body.getExpiredAt() != null
                ? Instant.parse(body.getExpiredAt()) : null;

        return ResponseEntity.ok(announcementService.update(id, body.getTitle(), body.getContent(),
                body.getIsPinned(), body.getIsPublished(), expiredAt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("公告已删除"));
    }
}
