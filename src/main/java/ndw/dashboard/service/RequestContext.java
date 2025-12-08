package ndw.dashboard.service;


import java.lang.ScopedValue;

/**
 * Request context using ScopedValue for correlation IDs.
 *
 * ScopedValue is in PREVIEW in Java 21 (requires --enable-preview)
 * It provides immutable, thread-safe context propagation that automatically
 * inherits to child tasks in Structured Concurrency.
 *
 * Key advantage over ThreadLocal:
 * - Immutable (safer)
 * - Better performance
 * - Automatic propagation with StructuredTaskScope.fork()
 */
public class RequestContext {
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();
}
