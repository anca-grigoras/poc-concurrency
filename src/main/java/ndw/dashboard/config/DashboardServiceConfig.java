package ndw.dashboard.config;

import ndw.dashboard.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DashboardServiceConfig {

    @Value("${dashboard.service.implementation:structured}")
    private String implementationType;

    @Bean
    @Primary
    public DashboardService primaryDashboardService(
            DashboardServiceClassicSequential sequential,
            DashboardServiceClassicAsync async,
            DashboardServiceStructuredConcurrency structured) {

        return switch (implementationType.toLowerCase()) {
            case "sequential" -> sequential;
            case "async", "completablefuture" -> async;
            case "structured", "structuredconcurrency" -> structured;
            default -> throw new IllegalArgumentException(
                    "Unknown implementation type: " + implementationType +
                            ". Valid options: sequential, async, structured"
            );
        };
    }
}
