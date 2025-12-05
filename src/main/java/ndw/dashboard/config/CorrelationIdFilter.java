package ndw.dashboard.config;

import ndw.dashboard.service.RequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.ScopedValue;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Get correlation ID from header, or generate one
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        log.info("=== Request started with correlation ID: {} ===", correlationId);

        // Set the ScopedValue for this request thread
        final String finalCorrelationId = correlationId;
        ScopedValue.where(RequestContext.CORRELATION_ID, finalCorrelationId)
                .run(() -> {
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                });

        log.info("=== Request completed with correlation ID: {} ===", finalCorrelationId);
    }
}
