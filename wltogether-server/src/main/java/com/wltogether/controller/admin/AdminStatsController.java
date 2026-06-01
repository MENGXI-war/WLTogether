package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Memory
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        stats.put("memory", Map.of(
                "heapUsedMb", heapUsed / 1024 / 1024,
                "heapMaxMb", heapMax / 1024 / 1024,
                "usagePercent", heapMax > 0 ? (int) (heapUsed * 100 / heapMax) : 0
        ));

        // CPU
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        stats.put("cpu", Map.of(
                "availableProcessors", os.getAvailableProcessors(),
                "systemLoad", String.format("%.2f", os.getSystemLoadAverage())
        ));

        // JVM
        stats.put("jvm", Map.of(
                "uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000,
                "threadCount", ManagementFactory.getThreadMXBean().getThreadCount()
        ));

        return ResponseEntity.ok(ApiResponse.ok("ok", stats));
    }
}
