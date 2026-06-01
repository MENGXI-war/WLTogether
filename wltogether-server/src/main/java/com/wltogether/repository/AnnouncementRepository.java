package com.wltogether.repository;

import com.wltogether.model.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByIsPublishedTrueOrderByIsPinnedDescCreatedAtDesc();
    List<Announcement> findByIsPublishedTrueOrderByCreatedAtDesc();
    List<Announcement> findAllByOrderByCreatedAtDesc();
}
