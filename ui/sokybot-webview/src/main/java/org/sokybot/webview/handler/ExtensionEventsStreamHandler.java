package org.sokybot.webview.handler;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.RSocketServerService;

import reactor.core.publisher.Flux;

/**
 * Stream handler for extension events.
 * 
 * Stream: extension.events
 * Provides a continuous stream of extension-related events 
 * (page added/removed, toolbar actions, etc.)
 */
@Component(
    service = IRSocketStreamHandler.class,
    property = IRSocketStreamHandler.STREAM_PROPERTY + "=extension.events"
)
public class ExtensionEventsStreamHandler implements IRSocketStreamHandler {
    
    private RSocketServerService rsocketService;
    
    @Reference
    protected void setRsocketService(RSocketServerService rsocketService) {
        this.rsocketService = rsocketService;
    }
    
    @Override
    public String getStreamName() {
        return "extension.events";
    }
    
    @Override
    public String getDescription() {
        return "Stream of extension events (page added/removed, updates)";
    }
    
    @Override
    public Flux<Object> handleStream(RSocketRequest request) {
        return rsocketService.getExtensionEventStream().map(event -> event);
    }
}
