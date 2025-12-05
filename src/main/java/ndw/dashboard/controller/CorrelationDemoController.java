package ndw.dashboard.controller;

import ndw.dashboard.service.RequestContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class CorrelationDemoController {

    @GetMapping("/correlation-id")
    public Map<String, String> getCurrentCorrelationId() {
        String correlationId = RequestContext.CORRELATION_ID.orElse("no-corr-id");

        Map<String, String> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("threadName", Thread.currentThread().getName());
        response.put("explanation", "This correlation ID should appear in all logs for this request");

        return response;
    }
}
