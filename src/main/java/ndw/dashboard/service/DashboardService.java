package ndw.dashboard.service;

import ndw.dashboard.model.DashboardDto;

public interface DashboardService {
    DashboardDto getDashboard(String userId) throws Exception;
    String getImplementationType();
}
