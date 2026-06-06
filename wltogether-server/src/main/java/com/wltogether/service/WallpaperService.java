package com.wltogether.service;

import com.wltogether.model.entity.Wallpaper;
import com.wltogether.repository.WallpaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WallpaperService {

    private final WallpaperRepository wallpaperRepository;

    public List<Wallpaper> listWallpapers() {
        return wallpaperRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Wallpaper> getToday() {
        return wallpaperRepository.findByPublishDate(LocalDate.now());
    }

    @Transactional
    public Wallpaper create(String imageUrl, String title, String description,
                             LocalDate publishDate, Long createdBy) {
        Wallpaper wp = Wallpaper.builder()
                .imageUrl(imageUrl)
                .title(title)
                .description(description)
                .publishDate(publishDate)
                .createdBy(createdBy)
                .build();
        return wallpaperRepository.save(wp);
    }

    @Transactional
    public Wallpaper update(Long id, String imageUrl, String title, String description,
                             LocalDate publishDate) {
        Wallpaper wp = wallpaperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("壁纸不存在"));
        if (imageUrl != null) wp.setImageUrl(imageUrl);
        if (title != null) wp.setTitle(title);
        if (description != null) wp.setDescription(description);
        if (publishDate != null) wp.setPublishDate(publishDate);
        return wallpaperRepository.save(wp);
    }

    @Transactional
    public void delete(Long id) {
        wallpaperRepository.deleteById(id);
    }
}
