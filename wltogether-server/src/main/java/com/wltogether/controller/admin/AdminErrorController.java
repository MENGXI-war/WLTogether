package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.ErrorLog;
import com.wltogether.service.ErrorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/errors")
@RequiredArgsConstructor
public class AdminErrorController {

    private final ErrorLogService errorLogService;

    @GetMapping
    public ResponseEntity<Page<ErrorLog>> list(Pageable pageable,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String severity) {
        return ResponseEntity.ok(errorLogService.list(status, severity, null, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ErrorLog> update(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        String resolutionNote = (String) body.get("resolutionNote");
        return ResponseEntity.ok(errorLogService.updateStatus(id, status, resolutionNote));
    }
}
