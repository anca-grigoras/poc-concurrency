package ndw.dashboard.service;

import ndw.dashboard.model.UserProfile;
import ndw.dashboard.model.UserStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SlowUserClient {
    private static final Logger log = LoggerFactory.getLogger(SlowUserClient.class);

    public UserProfile fetchProfile(String userId) {
        logWithCorrelation("fetchProfile for userId=" + userId);
        sleep(500); // simulate slow I/O
        return new UserProfile(userId, "Alice", "alice@example.com");
    }

    public UserStats fetchStats(String userId) {
        logWithCorrelation("fetchStats for userId=" + userId);
        sleep(700); // simulate slow I/O
        return new UserStats(42, 1000);
    }

    private void logWithCorrelation(String operation) {
        String corr = RequestContext.CORRELATION_ID.orElse("no-corr-id");
        // [corr-id] operation
        log.info("[{}] {}", corr, operation);
    }


    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
