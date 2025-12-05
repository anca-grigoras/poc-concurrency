# POC: Structured Concurrency vs CompletableFuture

A Spring Boot demo comparing three concurrency approaches in Java 23:
1. **Sequential (Blocking)** - Traditional blocking I/O (~1200ms)
2. **CompletableFuture (Async)** - Classic async with thread pools (~700ms)
3. **Structured Concurrency** - Java 23's new paradigm (~700ms, cleaner code)

## Key Differences

| Approach | Performance | Code Complexity | ScopedValue Propagation | Thread Management |
|----------|-------------|-----------------|-------------------------|-------------------|
| Sequential | ❌ Slow | ✅ Simple | ✅ Automatic | ✅ None needed |
| CompletableFuture | ✅ Fast | ⚠️ Complex | ❌ Manual | ⚠️ Manual pool sizing |
| Structured Concurrency | ✅ Fast | ✅ Simple | ✅ Automatic | ✅ Automatic |

### Why Structured Concurrency?

The **correlation ID problem** demonstrates a key advantage:
- In `CompletableFuture.supplyAsync()`, `ScopedValue` is lost across threads
- You must manually capture and restore context in each task
- **Structured Concurrency automatically propagates ScopedValue** to child tasks!

## Prerequisites

- Java 23+
- Maven 3.6+

## Quick Start

```bash
# Build and run
mvn clean spring-boot:run

# The app starts on http://localhost:8080
```

## Usage

### 1. Check Current Implementation

```bash
curl http://localhost:8080/dashboard/info
```

Response:
```json
{
  "currentImplementation": "Structured Concurrency (Java 23)",
  "hint": "Change via application.properties: dashboard.service.implementation=[sequential|async|structured]"
}
```

### 2. Test Dashboard Endpoint

```bash
curl http://localhost:8080/dashboard/user123
```

Response:
```json
{
  "profile": {
    "userId": "user123",
    "name": "Alice",
    "email": "alice@example.com"
  },
  "stats": {
    "posts": 42,
    "followers": 1000
  }
}
```

### 3. Run Performance Comparison (RECOMMENDED!)

Compare all implementations with 10 iterations:

```bash
curl "http://localhost:8080/benchmark/compare?iterations=10"
```

Example output:
```json
{
  "Sequential (Blocking)": {
    "iterations": 10,
    "averageMs": "1205.32",
    "minMs": "1201.45",
    "maxMs": "1212.89",
    "sampleResult": { ... }
  },
  "CompletableFuture (Fixed Thread Pool)": {
    "iterations": 10,
    "averageMs": "703.21",
    "minMs": "701.12",
    "maxMs": "708.44",
    "sampleResult": { ... }
  },
  "Structured Concurrency (Java 23)": {
    "iterations": 10,
    "averageMs": "701.88",
    "minMs": "700.23",
    "maxMs": "705.11",
    "sampleResult": { ... }
  }
}
```

### 4. Benchmark Single Implementation

```bash
# Test structured concurrency with 20 iterations
curl "http://localhost:8080/benchmark/single?type=structured&iterations=20"

# Test async (CompletableFuture)
curl "http://localhost:8080/benchmark/single?type=async&iterations=20"

# Test sequential
curl "http://localhost:8080/benchmark/single?type=sequential&iterations=20"
```

## Switch Implementations

Edit `src/main/resources/application.properties`:

```properties
# Options: sequential | async | structured
dashboard.service.implementation=structured
```

Then restart the app to see the difference!

## Project Structure

```
src/main/java/ndw/dashboard/
├── DashboardApplication.java          # Spring Boot entry point
├── config/
│   └── DashboardServiceConfig.java    # Service selection config
├── controller/
│   ├── DashboardController.java       # Main dashboard endpoint
│   └── BenchmarkController.java       # Performance comparison
├── model/
│   ├── DashboardDto.java
│   ├── UserProfile.java
│   └── UserStats.java
└── service/
    ├── DashboardService.java                      # Common interface
    ├── DashboardServiceClassicSequential.java     # Sequential impl
    ├── DashboardServiceClassicAsync.java          # CompletableFuture impl
    ├── DashboardServiceStructuredConcurrency.java # Structured Concurrency impl
    ├── SlowUserClient.java                        # Simulates slow I/O
    └── RequestContext.java                        # ScopedValue for correlation IDs
```

## Code Highlights

### Sequential (Blocking)
```java
public DashboardDto getDashboard(String userId) {
    UserProfile profile = client.fetchProfile(userId); // 500ms
    UserStats stats = client.fetchStats(userId);       // 700ms
    return new DashboardDto(profile, stats);           // Total: ~1200ms
}
```

### CompletableFuture (Async)
```java
public DashboardDto getDashboard(String userId) throws Exception {
    CompletableFuture<UserProfile> profileFuture =
        CompletableFuture.supplyAsync(() -> client.fetchProfile(userId), executor);
    CompletableFuture<UserStats> statsFuture =
        CompletableFuture.supplyAsync(() -> client.fetchStats(userId), executor);

    return profileFuture.thenCombine(statsFuture, DashboardDto::new).get();
    // Total: ~700ms (parallel execution)
    // ⚠️ ScopedValue lost! Correlation IDs won't propagate
}
```

### Structured Concurrency (Java 23)
```java
public DashboardDto getDashboard(String userId) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        var profileTask = scope.fork(() -> client.fetchProfile(userId));
        var statsTask = scope.fork(() -> client.fetchStats(userId));

        scope.join();           // Wait for all
        scope.throwIfFailed();  // Propagate errors

        return new DashboardDto(profileTask.get(), statsTask.get());
        // Total: ~700ms (parallel execution)
        // ✅ ScopedValue propagates automatically!
    }
}
```

## Expected Results

- **Sequential**: ~1200ms (500 + 700 = sequential execution)
- **CompletableFuture**: ~700ms (max of 500, 700 = parallel execution)
- **Structured Concurrency**: ~700ms (parallel) + cleaner code + automatic context propagation

## Learning Points

1. **Performance**: Both async approaches cut latency by ~40% compared to sequential
2. **Simplicity**: Structured concurrency is as simple as sequential code
3. **Context Propagation**: Structured concurrency inherits `ScopedValue` automatically
4. **Error Handling**: `ShutdownOnFailure` cancels all tasks on first failure
5. **Resource Management**: Try-with-resources ensures proper cleanup

## Further Exploration

Try modifying `SlowUserClient.java` to:
- Add failures and see error handling
- Increase delays to see performance differences more clearly
- Add more operations to see structured concurrency's benefits at scale

## References

- [JEP 462: Structured Concurrency](https://openjdk.org/jeps/462)
- [JEP 464: Scoped Values](https://openjdk.org/jeps/464)
