# Correlation ID Propagation Demo

This demonstrates the **key difference** between CompletableFuture and Structured Concurrency when it comes to context propagation using Java's `ScopedValue`.

## The Problem

When using `CompletableFuture.supplyAsync()` with a thread pool:
- Tasks run on different threads from the executor pool
- `ScopedValue` (used for correlation IDs) **does NOT propagate** to these threads
- Result: Logs show `"no-corr-id"` even though a correlation ID was set for the request

## The Solution

Structured Concurrency with `StructuredTaskScope.fork()`:
- Child tasks **automatically inherit** `ScopedValue` from the parent thread
- Result: Correlation IDs propagate correctly, making distributed tracing work perfectly

## How to Test

### 1. Start the Application

```bash
mvn clean spring-boot:run
```

### 2. Test with Sequential Implementation (✅ Works)

Edit `application.properties`:
```properties
dashboard.service.implementation=sequential
```

Then restart and call:
```bash
curl -H "X-Correlation-Id: test-123" http://localhost:8080/dashboard/user456
```

**Expected Logs:**
```
INFO === Request started with correlation ID: test-123 ===
INFO [corr-id: test-123] [thread: http-nio-8080-exec-1] fetchProfile for userId=user456
INFO [corr-id: test-123] [thread: http-nio-8080-exec-1] fetchStats for userId=user456
INFO === Request completed with correlation ID: test-123 ===
```

✅ **Correlation ID is preserved** - everything runs on the same thread

---

### 3. Test with CompletableFuture (❌ BROKEN - Shows the Problem!)

Edit `application.properties`:
```properties
dashboard.service.implementation=async
```

Restart and call:
```bash
curl -H "X-Correlation-Id: test-456" http://localhost:8080/dashboard/user789
```

**Expected Logs:**
```
INFO === Request started with correlation ID: test-456 ===
INFO [corr-id: no-corr-id] [thread: pool-2-thread-1] fetchProfile for userId=user789
INFO [corr-id: no-corr-id] [thread: pool-2-thread-2] fetchStats for userId=user789
INFO === Request completed with correlation ID: test-456 ===
```

❌ **Correlation ID is LOST!**
- The main request thread has `test-456`
- But worker threads from the executor pool show `no-corr-id`
- This happens because `ScopedValue` doesn't transfer across `CompletableFuture.supplyAsync()`

**Why?** CompletableFuture runs on executor pool threads, and `ScopedValue` is not automatically inherited.

---

### 4. Test with Structured Concurrency (✅ FIXED!)

Edit `application.properties`:
```properties
dashboard.service.implementation=structured
```

Restart and call:
```bash
curl -H "X-Correlation-Id: test-789" http://localhost:8080/dashboard/user999
```

**Expected Logs:**
```
INFO === Request started with correlation ID: test-789 ===
INFO [corr-id: test-789] [thread: http-nio-8080-exec-1-virtual-123] fetchProfile for userId=user999
INFO [corr-id: test-789] [thread: http-nio-8080-exec-1-virtual-124] fetchStats for userId=user999
INFO === Request completed with correlation ID: test-789 ===
```

✅ **Correlation ID propagates correctly!**
- Even though tasks run on different virtual threads
- `StructuredTaskScope.fork()` automatically inherits the `ScopedValue`
- This is a **killer feature** of Structured Concurrency

**Why?** Structured Concurrency treats child tasks as part of the same logical operation, automatically propagating scoped context.

---

## Visual Comparison

### Sequential (✅ Works, but slow)
```
Request Thread [test-123]
    ├─ fetchProfile [test-123] ✅ (500ms)
    └─ fetchStats [test-123] ✅   (700ms)
Total: ~1200ms
```

### CompletableFuture (❌ Fast, but loses context)
```
Request Thread [test-456]
    ├─ pool-thread-1 [no-corr-id] ❌ (500ms) ← Lost correlation!
    └─ pool-thread-2 [no-corr-id] ❌ (700ms) ← Lost correlation!
Total: ~700ms (parallel)
```

### Structured Concurrency (✅ Fast AND preserves context)
```
Request Thread [test-789]
    ├─ virtual-thread-1 [test-789] ✅ (500ms) ← Correlation inherited!
    └─ virtual-thread-2 [test-789] ✅ (700ms) ← Correlation inherited!
Total: ~700ms (parallel)
```

---

## Real-World Impact

In production systems with distributed tracing (OpenTelemetry, Zipkin, etc.):

### With CompletableFuture:
- **Manual workaround needed**: You must capture and restore context in every async task
- Code becomes verbose and error-prone
- Easy to forget in new code

Example workaround (ugly!):
```java
String corrId = RequestContext.CORRELATION_ID.get();
CompletableFuture.supplyAsync(() -> {
    // Manual restoration required!
    ScopedValue.where(RequestContext.CORRELATION_ID, corrId).run(() -> {
        return client.fetchProfile(userId);
    });
}, executor);
```

### With Structured Concurrency:
- **Zero boilerplate**: Context propagates automatically
- Cleaner code, fewer bugs
- Works consistently across your entire codebase

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var task = scope.fork(() -> client.fetchProfile(userId));
    // ScopedValue automatically available! ✅
}
```

---

## Quick Test Endpoint

You can also quickly check the current correlation ID:

```bash
curl -H "X-Correlation-Id: my-test-id" http://localhost:8080/demo/correlation-id
```

Response:
```json
{
  "correlationId": "my-test-id",
  "threadName": "http-nio-8080-exec-2",
  "explanation": "This correlation ID should appear in all logs for this request"
}
```

---

## Key Takeaways

1. **Sequential**: Correlation IDs work, but performance is poor (~1200ms)
2. **CompletableFuture**: Fast (~700ms), but correlation IDs don't propagate ❌
3. **Structured Concurrency**: Fast (~700ms) AND correlation IDs propagate ✅

**Structured Concurrency gives you the best of both worlds!**
