package com.wltogether.repository;

import com.wltogether.model.entity.Wallpaper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WallpaperRepository extends JpaRepository<Wallpaper, Long> {
    Optional<Wallpaper> findTopByOrderByPublishDateDesc();
}
