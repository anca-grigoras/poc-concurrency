package ndw.dashboard.service;

import ndw.dashboard.model.DashboardDto;
import ndw.dashboard.model.UserProfile;
import ndw.dashboard.model.UserStats;
import org.springframework.stereotype.Service;

import java.util.concurrent.StructuredTaskScope;

@Service
public class DashboardServiceStructuredConcurrency implements DashboardService {

    private final SlowUserClient client;

    public DashboardServiceStructuredConcurrency(SlowUserClient client) {
        this.client = client;
    }

    @Override
    public DashboardDto getDashboard(String userId) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Fork tasks - they inherit ScopedValue automatically!
            // Note: ScopedValue is FINALIZED in Java 25 (production ready!)
            // Structured Concurrency is Fifth Preview (API stable)
            var profileTask = scope.fork(() -> client.fetchProfile(userId));
            var statsTask = scope.fork(() -> client.fetchStats(userId));

            // Wait for both to complete (or first failure)
            scope.join();
            scope.throwIfFailed();

            // Get results
            return new DashboardDto(profileTask.get(), statsTask.get());
        }
    }

    @Override
    public String getImplementationType() {
        return "Structured Concurrency (Java 25 - Fifth Preview)";
    }
}
