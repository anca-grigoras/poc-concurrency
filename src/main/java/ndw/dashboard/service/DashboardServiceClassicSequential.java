package ndw.dashboard.service;

import ndw.dashboard.model.DashboardDto;
import ndw.dashboard.model.UserProfile;
import ndw.dashboard.model.UserStats;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceClassicSequential {

    private final SlowUserClient client;

    public DashboardServiceClassicSequential(SlowUserClient client) {
        this.client = client;
    }

    public DashboardDto getDashboard(String userId) {
        UserProfile profile = client.fetchProfile(userId); // ~500 ms
        UserStats stats = client.fetchStats(userId);       // ~700 ms
        // total ~1.2 s
        return new DashboardDto(profile, stats);
    }
}
