package com.wltogether.controller;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.service.FileTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileTransferService fileTransferService;

    /**
     * Upload a file for server relay transfer.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file) {
        try {
            String fileId = fileTransferService.uploadFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getSize()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("上传成功", Map.of(
                            "fileId", fileId,
                            "fileName", file.getOriginalFilename(),
                            "fileSize", String.valueOf(file.getSize())
                    )));
        } catch (IOException e) {
            throw new IllegalArgumentException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * Download a file (with HTTP Range support).
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> download(
            @PathVariable String fileId,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            FileTransferService.FileEntry info = fileTransferService.getFileInfo(fileId);

            long fileSize = info.fileSize();
            long start = 0;
            long end = fileSize - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] parts = rangeHeader.substring(6).split("-");
                start = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    end = Long.parseLong(parts[1]);
                }
            }

            byte[] data = fileTransferService.readFileRange(fileId, start, end);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Content-Disposition", "attachment; filename=\"" + info.originalFilename() + "\"");

            if (rangeHeader != null) {
                headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
                headers.setContentLength(data.length);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers).body(data);
            }

            headers.setContentLength(data.length);
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get file metadata.
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<ApiResponse<FileTransferService.FileEntry>> getInfo(@PathVariable String fileId) {
        try {
            FileTransferService.FileEntry info = fileTransferService.getFileInfo(fileId);
            return ResponseEntity.ok(ApiResponse.ok("ok", info));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a file.
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String fileId) {
        fileTransferService.deleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.ok("文件已删除"));
    }
}
