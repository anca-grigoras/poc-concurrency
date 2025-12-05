package ndw.dashboard.controller;

import ndw.dashboard.model.DashboardDto;
import ndw.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    private final List<DashboardService> services;

    public BenchmarkController(List<DashboardService> services) {
        this.services = services;
    }

    @GetMapping("/compare")
    public Map<String, Object> compareAll(@RequestParam(defaultValue = "user123") String userId,
                                           @RequestParam(defaultValue = "5") int iterations) {
        Map<String, Object> results = new HashMap<>();

        for (DashboardService service : services) {
            results.put(service.getImplementationType(), benchmarkService(service, userId, iterations));
        }

        return results;
    }

    @GetMapping("/single")
    public Map<String, Object> benchmarkSingle(@RequestParam String type,
                                                @RequestParam(defaultValue = "user123") String userId,
                                                @RequestParam(defaultValue = "10") int iterations) {
        DashboardService service = services.stream()
                .filter(s -> s.getImplementationType().toLowerCase().contains(type.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Service type not found: " + type));

        return Map.of(
                "service", service.getImplementationType(),
                "result", benchmarkService(service, userId, iterations)
        );
    }

    private Map<String, Object> benchmarkService(DashboardService service, String userId, int iterations) {
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        DashboardDto lastResult = null;

        // Warmup
        try {
            service.getDashboard(userId);
        } catch (Exception e) {
            // ignore warmup errors
        }

        // Actual benchmark
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            try {
                lastResult = service.getDashboard(userId);
                long elapsed = System.nanoTime() - start;
                totalTime += elapsed;
                minTime = Math.min(minTime, elapsed);
                maxTime = Math.max(maxTime, elapsed);
            } catch (Exception e) {
                return Map.of(
                        "error", e.getMessage(),
                        "iterations", i
                );
            }
        }

        double avgMs = totalTime / iterations / 1_000_000.0;
        double minMs = minTime / 1_000_000.0;
        double maxMs = maxTime / 1_000_000.0;

        return Map.of(
                "iterations", iterations,
                "averageMs", String.format("%.2f", avgMs),
                "minMs", String.format("%.2f", minMs),
                "maxMs", String.format("%.2f", maxMs),
                "sampleResult", lastResult
        );
    }
}
