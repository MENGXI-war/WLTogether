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
    public void delete(Long id) {
        wallpaperRepository.deleteById(id);
    }
}
