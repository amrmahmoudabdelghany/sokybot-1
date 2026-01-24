package org.sokybot.webview.handler;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.test.StepVerifier;

/**
 * Tests for FileSystemHandler.
 */
@DisplayName("FileSystemHandler Tests")
class FileSystemHandlerTest {
    
    private FileSystemHandler handler;
    
    @BeforeEach
    void setUp() {
        handler = new FileSystemHandler();
    }
    
    @Test
    @DisplayName("Should return correct methods")
    void testGetMethods() {
        String[] methods = handler.getMethods();
        assertEquals(2, methods.length);
        assertEquals("fs.list", methods[0]);
        assertEquals("fs.roots", methods[1]);
    }
    
    @Test
    @DisplayName("Should list files in current directory")
    void testListCurrentDirectory() {
        RSocketRequest request = new RSocketRequest();
        request.setMethod("fs.list");
        request.setParams(Map.of("path", "."));
        
        StepVerifier.create(handler.handle(request))
            .assertNext(response -> {
                assertTrue(response.isSuccess());
                
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getResult();
                assertNotNull(result.get("current"));
                assertNotNull(result.get("files"));
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> files = (List<Map<String, Object>>) result.get("files");
                assertFalse(files.isEmpty());
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should list filesystem roots")
    void testListRoots() {
        RSocketRequest request = new RSocketRequest();
        request.setMethod("fs.roots");
        
        StepVerifier.create(handler.handle(request))
            .assertNext(response -> {
                assertTrue(response.isSuccess());
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> roots = (List<Map<String, Object>>) response.getResult();
                assertFalse(roots.isEmpty());
                
                // Each root should have name, path, and isDirectory
                Map<String, Object> firstRoot = roots.get(0);
                assertNotNull(firstRoot.get("name"));
                assertNotNull(firstRoot.get("path"));
                assertTrue((Boolean) firstRoot.get("isDirectory"));
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should include parent directory entry")
    void testParentDirectoryEntry() {
        // Use a path that's not a root
        String testPath = System.getProperty("user.home");
        File testDir = new File(testPath);
        
        if (testDir.getParentFile() != null) {
            RSocketRequest request = new RSocketRequest();
            request.setMethod("fs.list");
            request.setParams(Map.of("path", testPath));
            
            StepVerifier.create(handler.handle(request))
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) response.getResult();
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> files = (List<Map<String, Object>>) result.get("files");
                    
                    // First entry should be ".."
                    if (!files.isEmpty()) {
                        Map<String, Object> first = files.get(0);
                        assertEquals("..", first.get("name"));
                        assertTrue((Boolean) first.get("isDirectory"));
                    }
                })
                .verifyComplete();
        }
    }
    
    @Test
    @DisplayName("Should return error for unknown method")
    void testUnknownMethod() {
        RSocketRequest request = new RSocketRequest();
        request.setMethod("fs.unknown");
        
        StepVerifier.create(handler.handle(request))
            .assertNext(response -> {
                assertTrue(response.isError());
                assertEquals(RSocketResponse.ErrorCode.METHOD_NOT_FOUND, response.getError().getCode());
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should use default path when not specified")
    void testDefaultPath() {
        RSocketRequest request = new RSocketRequest();
        request.setMethod("fs.list");
        // No path parameter
        
        StepVerifier.create(handler.handle(request))
            .assertNext(response -> {
                assertTrue(response.isSuccess());
                
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getResult();
                assertNotNull(result.get("current"));
            })
            .verifyComplete();
    }
}
