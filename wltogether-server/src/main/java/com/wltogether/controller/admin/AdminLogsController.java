package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogsController {

    @Value("${logging.file.path:./logs}")
    private String logPath;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> list(
            @RequestParam(defaultValue = "100") int lines,
            @RequestParam(defaultValue = "INFO") String level) {
        List<String> result = new ArrayList<>();
        Path logFile = Paths.get(logPath, "wltogether.log");

        if (!Files.exists(logFile)) {
            // Try spring default log file
            logFile = Paths.get("logs", "spring.log");
        }

        if (!Files.exists(logFile)) {
            return ResponseEntity.ok(ApiResponse.ok("暂无日志文件", result));
        }

        try (Stream<String> stream = Files.lines(logFile)) {
            stream
                    .filter(line -> matchesLevel(line, level))
                    .skip(Math.max(0, countLines(logFile) - lines))
                    .forEach(result::add);
        } catch (IOException e) {
            log.warn("Failed to read log file: {}", e.getMessage());
            result.add("日志文件读取失败: " + e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.ok("ok", result));
    }

    private boolean matchesLevel(String line, String level) {
        if ("ALL".equalsIgnoreCase(level)) return true;
        return line.contains(" " + level.toUpperCase() + " ");
    }

    private long countLines(Path path) throws IOException {
        try (Stream<String> lines = Files.lines(path)) {
            return lines.count();
        }
    }
}
