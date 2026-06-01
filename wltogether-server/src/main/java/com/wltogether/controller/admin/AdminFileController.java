package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.FileMetadata;
import com.wltogether.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

    private final FileMetadataRepository fileMetadataRepository;

    @GetMapping
    public ResponseEntity<Page<FileMetadata>> list(Pageable pageable) {
        return ResponseEntity.ok(fileMetadataRepository.findAll(pageable));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        fileMetadataRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("文件元数据已删除"));
    }
}
