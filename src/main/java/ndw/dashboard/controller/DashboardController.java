package ndw.dashboard.controller;

import ndw.dashboard.model.DashboardDto;
import ndw.dashboard.service.DashboardServiceClassicAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardServiceClassicAsync service;
    // or use the sequential one to compare later

    public DashboardController(DashboardServiceClassicAsync service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public DashboardDto getDashboard(@PathVariable String userId) throws Exception {
        return service.getDashboard(userId);
    }
}
