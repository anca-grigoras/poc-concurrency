package ndw.dashboard.controller;

import ndw.dashboard.model.DashboardDto;
import ndw.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public DashboardDto getDashboard(@PathVariable String userId) throws Exception {
        return service.getDashboard(userId);
    }

    @GetMapping("/info")
    public Map<String, String> getInfo() {
        return Map.of(
                "currentImplementation", service.getImplementationType(),
                "hint", "Change via application.properties: dashboard.service.implementation=[sequential|async|structured]"
        );
    }
}
