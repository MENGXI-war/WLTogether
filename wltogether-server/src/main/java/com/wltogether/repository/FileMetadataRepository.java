package com.wltogether.repository;

import com.wltogether.model.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findByFileHash(String fileHash);
}
