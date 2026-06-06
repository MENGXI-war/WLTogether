package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogsController {

    @Value("${app.logs.dir:./logs}")
    private String logsDir;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        Path dir = Paths.get(logsDir);
        List<Map<String, Object>> files = new ArrayList<>();

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return ResponseEntity.ok(ApiResponse.ok("ok", files));
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.log*")) {
            for (Path path : stream) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", path.getFileName().toString());
                info.put("size", path.toFile().length());
                info.put("lastModified", path.toFile().lastModified());
                files.add(info);
            }
        } catch (IOException e) {
            log.error("Failed to list log files", e);
            throw new IllegalArgumentException("无法读取日志目录");
        }

        files.sort((a, b) -> Long.compare(
                (Long) b.get("lastModified"), (Long) a.get("lastModified")));
        return ResponseEntity.ok(ApiResponse.ok("ok", files));
    }

    @GetMapping("/{filename}")
    public ResponseEntity<ApiResponse<String>> read(@PathVariable String filename,
                                                     @RequestParam(defaultValue = "200") int lines) {
        // Security: prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("无效的文件名");
        }
        if (!filename.endsWith(".log") && !filename.contains(".log.")) {
            throw new IllegalArgumentException("仅支持读取 .log 文件");
        }
        if (lines < 1 || lines > 2000) {
            lines = Math.clamp(lines, 1, 2000);
        }

        Path filePath = Paths.get(logsDir).resolve(filename).normalize();
        if (!filePath.startsWith(Paths.get(logsDir).normalize())) {
            throw new IllegalArgumentException("无效的文件路径");
        }
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("日志文件不存在: " + filename);
        }

        try {
            List<String> allLines = Files.readAllLines(filePath);
            int from = Math.max(0, allLines.size() - lines);
            List<String> tail = allLines.subList(from, allLines.size());
            return ResponseEntity.ok(ApiResponse.ok("ok", String.join("\n", tail)));
        } catch (IOException e) {
            log.error("Failed to read log file: {}", filename, e);
            throw new IllegalArgumentException("无法读取日志文件");
        }
    }
}
