package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.dto.ErrorUpdateRequest;
import com.wltogether.model.entity.ErrorLog;
import com.wltogether.service.ErrorLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                                            @Valid @RequestBody ErrorUpdateRequest body) {
        return ResponseEntity.ok(errorLogService.updateStatus(id, body.getStatus(), body.getResolutionNote()));
    }
}
