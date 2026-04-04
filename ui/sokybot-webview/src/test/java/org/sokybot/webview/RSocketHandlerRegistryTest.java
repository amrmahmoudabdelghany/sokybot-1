package org.sokybot.webview;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.handler.WorkspaceSummaryHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests for the RSocketHandlerRegistry.
 */
@DisplayName("RSocketHandlerRegistry Tests")
class RSocketHandlerRegistryTest {
    
    private RSocketHandlerRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new RSocketHandlerRegistry();
    }
    
    @Test
    @DisplayName("Should register and invoke request handler")
    void testRegisterAndInvokeHandler() {
        // Create a test handler
        IRSocketHandler handler = new IRSocketHandler() {
            @Override
            public String[] getMethods() {
                return new String[] { "test.echo" };
            }
            
            @Override
            public Mono<RSocketResponse> handle(RSocketRequest request) {
                String message = request.getString("message", "default");
                return Mono.just(RSocketResponse.success(Map.of("echo", message)));
            }
        };
        
        // Register the handler
        registry.bindHandler(handler);
        
        // Verify it's registered
        assertTrue(registry.hasHandler("test.echo"));
        
        // Create a request
        RSocketRequest request = new RSocketRequest();
        request.setMethod("test.echo");
        request.setParams(Map.of("message", "hello"));
        
        // Invoke and verify
        StepVerifier.create(registry.handleRequest(request))
            .assertNext(response -> {
                assertTrue(response.isSuccess());
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getResult();
                assertEquals("hello", result.get("echo"));
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should return error for unknown method")
    void testUnknownMethod() {
        RSocketRequest request = new RSocketRequest();
        request.setMethod("unknown.method");
        
        StepVerifier.create(registry.handleRequest(request))
            .assertNext(response -> {
                assertTrue(response.isError());
                assertEquals(RSocketResponse.ErrorCode.METHOD_NOT_FOUND, response.getError().getCode());
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle null request")
    void testNullRequest() {
        StepVerifier.create(registry.handleRequest(null))
            .assertNext(response -> {
                assertTrue(response.isError());
                assertEquals(RSocketResponse.ErrorCode.INVALID_REQUEST, response.getError().getCode());
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle handler exceptions")
    void testHandlerException() {
        IRSocketHandler handler = new IRSocketHandler() {
            @Override
            public String[] getMethods() {
                return new String[] { "test.error" };
            }
            
            @Override
            public Mono<RSocketResponse> handle(RSocketRequest request) {
                return Mono.error(new RuntimeException("Test error"));
            }
        };
        
        registry.bindHandler(handler);
        
        RSocketRequest request = new RSocketRequest();
        request.setMethod("test.error");
        
        StepVerifier.create(registry.handleRequest(request))
            .assertNext(response -> {
                assertTrue(response.isError());
                assertEquals(RSocketResponse.ErrorCode.INTERNAL_ERROR, response.getError().getCode());
                assertTrue(response.getError().getMessage().contains("Test error"));
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should register and invoke stream handler")
    void testStreamHandler() {
        IRSocketStreamHandler handler = new IRSocketStreamHandler() {
            @Override
            public String getStreamName() {
                return "test.stream";
            }
            
            @Override
            public Flux<Object> handleStream(RSocketRequest request) {
                int count = request.getInt("count", 3);
                return Flux.range(1, count).map(i -> Map.of("value", i));
            }
        };
        
        registry.bindStreamHandler(handler);
        assertTrue(registry.hasStreamHandler("test.stream"));
        
        RSocketRequest request = new RSocketRequest();
        request.setMethod("test.stream");
        request.setParams(Map.of("count", 5));
        
        StepVerifier.create(registry.handleStream(request))
            .expectNextCount(5)
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should unregister handlers")
    void testUnregisterHandler() {
        IRSocketHandler handler = new IRSocketHandler() {
            @Override
            public String[] getMethods() {
                return new String[] { "test.temp" };
            }
            
            @Override
            public Mono<RSocketResponse> handle(RSocketRequest request) {
                return Mono.just(RSocketResponse.success("ok"));
            }
        };
        
        registry.bindHandler(handler);
        assertTrue(registry.hasHandler("test.temp"));
        
        registry.unbindHandler(handler);
        assertFalse(registry.hasHandler("test.temp"));
    }
    
    @Test
    @DisplayName("Should preserve request ID in response")
    void testRequestIdPreservation() {
        IRSocketHandler handler = new IRSocketHandler() {
            @Override
            public String[] getMethods() {
                return new String[] { "test.id" };
            }
            
            @Override
            public Mono<RSocketResponse> handle(RSocketRequest request) {
                return Mono.just(RSocketResponse.success("ok"));
            }
        };
        
        registry.bindHandler(handler);
        
        RSocketRequest request = new RSocketRequest();
        request.setMethod("test.id");
        request.setId("req-123");
        
        StepVerifier.create(registry.handleRequest(request))
            .assertNext(response -> {
                assertEquals("req-123", response.getId());
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should list registered methods and streams")
    void testGetRegisteredMethodsAndStreams() {
        IRSocketHandler handler1 = createHandler("method.one");
        IRSocketHandler handler2 = createHandler("method.two");
        IRSocketStreamHandler stream1 = createStreamHandler("stream.one");
        
        registry.bindHandler(handler1);
        registry.bindHandler(handler2);
        registry.bindStreamHandler(stream1);
        
        assertEquals(2, registry.getRegisteredMethods().size());
        assertTrue(registry.getRegisteredMethods().contains("method.one"));
        assertTrue(registry.getRegisteredMethods().contains("method.two"));
        
        assertEquals(1, registry.getRegisteredStreams().size());
        assertTrue(registry.getRegisteredStreams().contains("stream.one"));
    }
    
    private IRSocketHandler createHandler(String method) {
        return new IRSocketHandler() {
            @Override
            public String[] getMethods() {
                return new String[] { method };
            }
            
            @Override
            public Mono<RSocketResponse> handle(RSocketRequest request) {
                return Mono.just(RSocketResponse.success("ok"));
            }
        };
    }
    
    private IRSocketStreamHandler createStreamHandler(String stream) {
        return new IRSocketStreamHandler() {
            @Override
            public String getStreamName() {
                return stream;
            }
            
            @Override
            public Flux<Object> handleStream(RSocketRequest request) {
                return Flux.just("data");
            }
        };
    }

    @Test
    @DisplayName("workspace.summary handler is invocable when bound (CI contract smoke)")
    void workspaceSummaryHandlerInvocable() {
        WorkspaceSummaryHandler handler = new WorkspaceSummaryHandler();
        registry.bindHandler(handler);

        RSocketRequest request = new RSocketRequest();
        request.setMethod("workspace.summary");

        StepVerifier.create(registry.handleRequest(request))
                .assertNext(r -> assertTrue(r.isSuccess()))
                .verifyComplete();
    }
}
