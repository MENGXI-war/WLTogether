package com.wltogether.service;

import com.wltogether.model.entity.ErrorLog;
import com.wltogether.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    public Page<ErrorLog> list(String status, String severity, String source, Pageable pageable) {
        List<ErrorLog> result;
        if (status != null && severity != null) {
            result = errorLogRepository.findByStatusAndSeverityOrderByOccurredAtDesc(status, severity, pageable);
        } else if (status != null) {
            result = errorLogRepository.findByStatusOrderByOccurredAtDesc(status, pageable);
        } else if (severity != null) {
            result = errorLogRepository.findBySeverityOrderByOccurredAtDesc(severity, pageable);
        } else {
            return errorLogRepository.findAllByOrderByOccurredAtDesc(pageable);
        }
        return new PageImpl<>(result, pageable, result.size());
    }

    @Transactional
    public ErrorLog updateStatus(Long id, String status, String resolutionNote) {
        ErrorLog log = errorLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("错误记录不存在"));
        log.setStatus(status);
        if (resolutionNote != null) log.setResolutionNote(resolutionNote);
        if ("RESOLVED".equals(status)) log.setResolvedAt(Instant.now());
        return errorLogRepository.save(log);
    }
}
