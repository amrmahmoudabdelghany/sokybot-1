package org.sokybot.webview.handler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * Handler for file system operations.
 * Used by the frontend for directory picker dialogs.
 * 
 * Methods:
 *   - fs.list: List files in a directory
 *   - fs.roots: List filesystem roots
 */
@Component(
    service = IRSocketHandler.class,
    property = {
        IRSocketHandler.METHOD_PROPERTY + "=fs.list",
        IRSocketHandler.METHOD_PROPERTY + "=fs.roots"
    }
)
public class FileSystemHandler implements IRSocketHandler {
    
    @Override
    public String[] getMethods() {
        return new String[] { "fs.list", "fs.roots" };
    }
    
    @Override
    public String getDescription() {
        return "File system operations (list, roots)";
    }
    
    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();
        
        switch (method) {
            case "fs.list":
                return handleList(request);
            case "fs.roots":
                return handleRoots(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }
    
    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        String path = request.getString("path", ".");
        
        File dir = new File(path);
        try {
            dir = dir.getCanonicalFile();
        } catch (IOException e) {
            dir = dir.getAbsoluteFile();
        }
        
        List<Map<String, Object>> files = new ArrayList<>();
        
        // Add parent directory entry
        if (dir.getParentFile() != null) {
            Map<String, Object> parent = new HashMap<>();
            parent.put("name", "..");
            parent.put("path", dir.getParentFile().getAbsolutePath());
            parent.put("isDirectory", true);
            files.add(parent);
        }
        
        // List directory contents
        if (dir.exists() && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File f : children) {
                    if (f.isHidden()) continue;
                    
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", f.getName());
                    fileInfo.put("path", f.getAbsolutePath());
                    fileInfo.put("isDirectory", f.isDirectory());
                    files.add(fileInfo);
                }
            }
        }
        
        // Sort: directories first, then alphabetically
        files.sort((a, b) -> {
            boolean aDir = (Boolean) a.get("isDirectory");
            boolean bDir = (Boolean) b.get("isDirectory");
            if (aDir && !bDir) return -1;
            if (!aDir && bDir) return 1;
            return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
        });
        
        Map<String, Object> result = new HashMap<>();
        result.put("current", dir.getAbsolutePath());
        result.put("files", files);
        
        return Mono.just(RSocketResponse.success(result));
    }
    
    private Mono<RSocketResponse> handleRoots(RSocketRequest request) {
        File[] roots = File.listRoots();
        List<Map<String, Object>> rootList = new ArrayList<>();
        
        if (roots != null) {
            for (File f : roots) {
                Map<String, Object> root = new HashMap<>();
                root.put("name", f.getPath());
                root.put("path", f.getPath());
                root.put("isDirectory", true);
                rootList.add(root);
            }
        }
        
        return Mono.just(RSocketResponse.success(rootList));
    }
}
