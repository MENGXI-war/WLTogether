package com.wltogether.service;

import com.wltogether.model.entity.Announcement;
import com.wltogether.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public List<Announcement> listAnnouncements(boolean publishedOnly) {
        if (publishedOnly) {
            return announcementRepository.findByIsPublishedTrueOrderByCreatedAtDesc();
        }
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Announcement create(String title, String content, Long createdBy,
                                boolean isPinned, Instant expiredAt) {
        Announcement ann = Announcement.builder()
                .title(title)
                .content(content)
                .createdBy(createdBy)
                .isPinned(isPinned)
                .isPublished(true)
                .publishedAt(Instant.now())
                .expiredAt(expiredAt)
                .build();
        return announcementRepository.save(ann);
    }

    @Transactional
    public Announcement update(Long id, String title, String content,
                                Boolean isPinned, Boolean isPublished, Instant expiredAt) {
        Announcement ann = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在"));
        if (title != null) ann.setTitle(title);
        if (content != null) ann.setContent(content);
        if (isPinned != null) ann.setIsPinned(isPinned);
        if (isPublished != null) {
            ann.setIsPublished(isPublished);
            if (isPublished && ann.getPublishedAt() == null) {
                ann.setPublishedAt(Instant.now());
            }
        }
        if (expiredAt != null) ann.setExpiredAt(expiredAt);
        return announcementRepository.save(ann);
    }

    @Transactional
    public void delete(Long id) {
        announcementRepository.deleteById(id);
    }
}
