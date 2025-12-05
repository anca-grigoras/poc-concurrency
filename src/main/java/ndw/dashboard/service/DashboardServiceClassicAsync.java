package ndw.dashboard.service;

import ndw.dashboard.model.DashboardDto;
import ndw.dashboard.model.UserProfile;
import ndw.dashboard.model.UserStats;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DashboardServiceClassicAsync implements DashboardService {

    private final SlowUserClient client;
    private final ExecutorService executor =
            Executors.newFixedThreadPool(20); // you must size/tune this

    public DashboardServiceClassicAsync(SlowUserClient client) {
        this.client = client;
    }

    @Override
    public DashboardDto getDashboard(String userId) throws Exception {
        CompletableFuture<UserProfile> profileFuture =
                CompletableFuture.supplyAsync(
                        () -> client.fetchProfile(userId),
                        executor
                );

        CompletableFuture<UserStats> statsFuture =
                CompletableFuture.supplyAsync(
                        () -> client.fetchStats(userId),
                        executor
                );

        return profileFuture
                .thenCombine(statsFuture, DashboardDto::new)
                .get(); // block and unwrap, with checked Exception
    }

    @Override
    public String getImplementationType() {
        return "CompletableFuture (Fixed Thread Pool)";
    }
}
