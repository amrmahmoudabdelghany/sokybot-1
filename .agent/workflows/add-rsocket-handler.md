---
description: How to add a new RSocket handler for frontend-backend communication
---

# How to Add a New RSocket Handler

RSocket handlers provide the backend API for the React frontend. They are automatically discovered via the OSGi whiteboard pattern.

## 1. Choose Handler Type

| Type | Interface | OSGi property | Use Case |
|------|-----------|---------------|----------|
| Request-Response | `IRSocketHandler` | `rsocket.method` | Single request → single response |
| Request-Stream | `IRSocketStreamHandler` | `rsocket.stream` | Single request → stream of responses |
| Fire-and-forget | `IRSocketFireAndForgetHandler` | `rsocket.fireAndForget` | Best-effort; no response payload |
| Request-channel | `IRSocketChannelHandler` | `rsocket.channel` | Bidirectional stream (initial frame + inbound flux) |

Reference channel: [`DiagnosticsChannelHandler`](../../ui/sokybot-webview/src/main/java/org/sokybot/webview/handler/DiagnosticsChannelHandler.java) (`diagnostics.stream`).

## 2. Create Handler Class

Create in `ui/sokybot-webview/src/main/java/org/sokybot/webview/handler/`:

### Request-Response Handler

```java
package org.sokybot.webview.handler;

import org.osgi.service.component.annotations.*;
import org.sokybot.webview.api.*;
import reactor.core.publisher.Mono;

@Component(
    service = IRSocketHandler.class,
    property = {
        IRSocketHandler.METHOD_PROPERTY + "=myfeature.action1",
        IRSocketHandler.METHOD_PROPERTY + "=myfeature.action2"
    }
)
public class MyFeatureHandler implements IRSocketHandler {
    
    // Use setter injection for dependencies
    private volatile IMyDependency dependency;
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setDependency(IMyDependency dep) {
        this.dependency = dep;
    }
    
    protected void unsetDependency(IMyDependency dep) {
        this.dependency = null;
    }
    
    @Override
    public String[] getMethods() {
        // MUST match the property values exactly
        return new String[] { "myfeature.action1", "myfeature.action2" };
    }
    
    @Override
    public String getDescription() {
        return "My feature operations";
    }
    
    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        switch (request.getMethod()) {
            case "myfeature.action1":
                return handleAction1(request);
            case "myfeature.action2":
                return handleAction2(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(request.getMethod()));
        }
    }
    
    private Mono<RSocketResponse> handleAction1(RSocketRequest request) {
        // Get typed parameters
        String param = request.getString("paramName");
        Integer count = request.getInt("count", 10); // with default
        
        if (param == null) {
            return Mono.just(RSocketResponse.invalidParams("paramName is required"));
        }
        
        // Return success with data
        return Mono.just(RSocketResponse.success(Map.of(
            "result", "processed: " + param,
            "count", count
        )));
    }
    
    private Mono<RSocketResponse> handleAction2(RSocketRequest request) {
        // Handle errors properly
        try {
            Object result = doSomething();
            return Mono.just(RSocketResponse.success(result));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }
}
```

### Request-Stream Handler

```java
@Component(
    service = IRSocketStreamHandler.class,
    property = IRSocketStreamHandler.STREAM_PROPERTY + "=myfeature.events"
)
public class MyFeatureStreamHandler implements IRSocketStreamHandler {
    
    @Override
    public String getStreamName() {
        return "myfeature.events";
    }
    
    @Override
    public Flux<Object> handleStream(RSocketRequest request) {
        String filter = request.getString("filter", "all");
        
        return Flux.interval(Duration.ofSeconds(1))
            .map(tick -> Map.of(
                "type", "tick",
                "value", tick,
                "filter", filter
            ));
    }
}
```

## 3. Response Helpers

Use `RSocketResponse` factory methods:

```java
// Success responses
RSocketResponse.success(data)                    // Wrap any data
RSocketResponse.success(Map.of("key", "value"))  // Map data

// Error responses
RSocketResponse.error(code, message)             // Custom error
RSocketResponse.methodNotFound(method)           // -32601
RSocketResponse.invalidParams(message)           // -32602
RSocketResponse.internalError(exception)         // -32603
RSocketResponse.notFound(message)                // -32604
```

## 3b. Fire-and-forget handler

```java
@Component(service = IRSocketFireAndForgetHandler.class, property = {
    IRSocketFireAndForgetHandler.METHOD_PROPERTY + "=myfeature.signal"
})
public class MyFeatureFnfHandler implements IRSocketFireAndForgetHandler {
    @Override
    public String[] getMethods() {
        return new String[] { "myfeature.signal" };
    }

    @Override
    public Mono<Void> handleFireAndForget(RSocketRequest request) {
        return Mono.empty();
    }
}
```

## 3c. Request-channel handler

Initial JSON frame `method` must equal `getChannelName()`. Further inbound frames use the same `method` (see `RSocketServerService` channel parsing).

## 4. Add Frontend Support

Update `ui/sokybot-webview/src/main/frontend/src/RSocketClient.ts`:

```typescript
// Add typed method
async myFeatureAction1(param: string, count?: number) {
    return this.request<MyResultType>('myfeature.action1', { 
        paramName: param, 
        count 
    });
}

// Add stream subscription
subscribeToMyEvents(
    onEvent: (event: MyEventType) => void,
    filter?: string
) {
    return this.subscribe<MyEventType>(
        'myfeature.events',
        onEvent,
        undefined,
        { filter }
    );
}
```

## 5. Deploy and Test

```bash
# Build and deploy
soky deploy sokybot-webview

# Check logs for registration
soky backend logs | grep "Registered RSocket handler"
```

You should see log lines such as `Registered RSocket handler: …`, `Registered RSocket stream handler: …`, `Registered fire-and-forget handler: …`, or `Registered channel handler: …`.

Also see [docs/RSOCKET_WEBVIEW.md](../../docs/RSOCKET_WEBVIEW.md) for metadata routing and stream contracts.

## Common Mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| `getMethods()` returns wildcard | "Method not found" errors | Return explicit method names |
| Field injection with `volatile` | Compile error | Use setter injection |
| Methods don't match properties | Handler not called | Ensure exact match |
| Missing `@Component` service | Handler not registered | Add `service = IRSocketHandler.class` |
| Forgot to rebuild | Old code runs | Use `soky deploy` |
