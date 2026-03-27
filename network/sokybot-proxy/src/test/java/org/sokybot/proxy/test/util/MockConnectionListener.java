package org.sokybot.proxy.test.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.sokybot.proxy.IConnectionListener;

/**
 * Mock implementation of IConnectionListener for testing.
 * Tracks all connection events and provides assertions for testing.
 */
public class MockConnectionListener implements IConnectionListener {
    
    private final List<String> events;
    private final List<Throwable> disconnectionCauses;
    private String redirectHost;
    private int redirectPort;
    private int redirectLoginId;
    private String serverServiceName;
    private String handshakeFailureReason;
    
    private CountDownLatch clientConnectedLatch;
    private CountDownLatch serverConnectedLatch;
    private CountDownLatch securitySetupLatch;
    private CountDownLatch handshakeCompleteLatch;
    private CountDownLatch handshakeFailedLatch;
    private CountDownLatch redirectLatch;
    private CountDownLatch serverIdentifiedLatch;
    private CountDownLatch authenticatedLatch;
    private CountDownLatch disconnectedLatch;
    
    public MockConnectionListener() {
        this.events = new ArrayList<>();
        this.disconnectionCauses = new ArrayList<>();
    }
    
    @Override
    public void onClientConnected() {
        events.add("onClientConnected");
        if (clientConnectedLatch != null) {
            clientConnectedLatch.countDown();
        }
    }
    
    @Override
    public void onServerConnected() {
        events.add("onServerConnected");
        if (serverConnectedLatch != null) {
            serverConnectedLatch.countDown();
        }
    }
    
    @Override
    public void onSecuritySetupComplete() {
        events.add("onSecuritySetupComplete");
        if (securitySetupLatch != null) {
            securitySetupLatch.countDown();
        }
    }
    
    @Override
    public void onHandshakeComplete() {
        events.add("onHandshakeComplete");
        if (handshakeCompleteLatch != null) {
            handshakeCompleteLatch.countDown();
        }
    }
    
    @Override
    public void onHandshakeFailed(String reason) {
        events.add("onHandshakeFailed");
        this.handshakeFailureReason = reason;
        if (handshakeFailedLatch != null) {
            handshakeFailedLatch.countDown();
        }
    }
    
    @Override
    public void onRedirectRequired(String host, int port, int loginId) {
        events.add("onRedirectRequired");
        this.redirectHost = host;
        this.redirectPort = port;
        this.redirectLoginId = loginId;
        if (redirectLatch != null) {
            redirectLatch.countDown();
        }
    }
    
    @Override
    public void onServerIdentified(String serviceName) {
        events.add("onServerIdentified");
        this.serverServiceName = serviceName;
        if (serverIdentifiedLatch != null) {
            serverIdentifiedLatch.countDown();
        }
    }

    @Override
    public void onAuthenticated() {
        events.add("onAuthenticated");
        if (authenticatedLatch != null) {
            authenticatedLatch.countDown();
        }
    }
    
    @Override
    public void onDisconnected(Throwable cause) {
        events.add("onDisconnected");
        if (cause != null) {
            disconnectionCauses.add(cause);
        }
        if (disconnectedLatch != null) {
            disconnectedLatch.countDown();
        }
    }
    
    // ========== Latch Setup Methods ==========
    
    public void expectClientConnected() {
        this.clientConnectedLatch = new CountDownLatch(1);
    }
    
    public void expectServerConnected() {
        this.serverConnectedLatch = new CountDownLatch(1);
    }
    
    public void expectSecuritySetupComplete() {
        this.securitySetupLatch = new CountDownLatch(1);
    }
    
    public void expectHandshakeComplete() {
        this.handshakeCompleteLatch = new CountDownLatch(1);
    }
    
    public void expectHandshakeFailed() {
        this.handshakeFailedLatch = new CountDownLatch(1);
    }
    
    public void expectRedirect() {
        this.redirectLatch = new CountDownLatch(1);
    }
    
    public void expectServerIdentified() {
        this.serverIdentifiedLatch = new CountDownLatch(1);
    }
    
    public void expectDisconnected() {
        this.disconnectedLatch = new CountDownLatch(1);
    }

    public void expectAuthenticated() {
        this.authenticatedLatch = new CountDownLatch(1);
    }
    
    // ========== Wait Methods ==========
    
    public void waitForClientConnected(long timeoutMs) throws InterruptedException {
        if (clientConnectedLatch == null) {
            expectClientConnected();
        }
        boolean completed = clientConnectedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Client connection did not occur within " + timeoutMs + "ms");
        }
    }
    
    public void waitForServerConnected(long timeoutMs) throws InterruptedException {
        if (serverConnectedLatch == null) {
            expectServerConnected();
        }
        boolean completed = serverConnectedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Server connection did not occur within " + timeoutMs + "ms");
        }
    }
    
    public void waitForSecuritySetup(long timeoutMs) throws InterruptedException {
        if (securitySetupLatch == null) {
            expectSecuritySetupComplete();
        }
        boolean completed = securitySetupLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Security setup did not complete within " + timeoutMs + "ms");
        }
    }
    
    public void waitForHandshakeComplete(long timeoutMs) throws InterruptedException {
        if (handshakeCompleteLatch == null) {
            expectHandshakeComplete();
        }
        boolean completed = handshakeCompleteLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Handshake did not complete within " + timeoutMs + "ms");
        }
    }
    
    public void waitForHandshakeFailed(long timeoutMs) throws InterruptedException {
        if (handshakeFailedLatch == null) {
            expectHandshakeFailed();
        }
        boolean completed = handshakeFailedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Handshake failure did not occur within " + timeoutMs + "ms");
        }
    }
    
    public void waitForRedirect(long timeoutMs) throws InterruptedException {
        if (redirectLatch == null) {
            expectRedirect();
        }
        boolean completed = redirectLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Redirect did not occur within " + timeoutMs + "ms");
        }
    }
    
    public void waitForServerIdentified(long timeoutMs) throws InterruptedException {
        if (serverIdentifiedLatch == null) {
            expectServerIdentified();
        }
        boolean completed = serverIdentifiedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Server identification did not occur within " + timeoutMs + "ms");
        }
    }
    
    public void waitForDisconnected(long timeoutMs) throws InterruptedException {
        if (disconnectedLatch == null) {
            expectDisconnected();
        }
        boolean completed = disconnectedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Disconnection did not occur within " + timeoutMs + "ms");
        }
    }

    public void waitForAuthenticated(long timeoutMs) throws InterruptedException {
        if (authenticatedLatch == null) {
            expectAuthenticated();
        }
        boolean completed = authenticatedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Authentication did not occur within " + timeoutMs + "ms");
        }
    }
    
    // ========== Assertion Methods ==========
    
    public List<String> getEvents() {
        return new ArrayList<>(events);
    }
    
    public void clearEvents() {
        events.clear();
        disconnectionCauses.clear();
    }
    
    public boolean hasEvent(String eventName) {
        return events.contains(eventName);
    }
    
    public int getEventCount(String eventName) {
        return (int) events.stream().filter(e -> e.equals(eventName)).count();
    }
    
    public String getRedirectHost() {
        return redirectHost;
    }
    
    public int getRedirectPort() {
        return redirectPort;
    }
    
    public int getRedirectLoginId() {
        return redirectLoginId;
    }
    
    public String getServerServiceName() {
        return serverServiceName;
    }
    
    public String getHandshakeFailureReason() {
        return handshakeFailureReason;
    }
    
    public List<Throwable> getDisconnectionCauses() {
        return new ArrayList<>(disconnectionCauses);
    }
    
    public boolean wasDisconnected() {
        return hasEvent("onDisconnected");
    }
    
    public boolean wasClientConnected() {
        return hasEvent("onClientConnected");
    }
    
    public boolean wasServerConnected() {
        return hasEvent("onServerConnected");
    }
    
    public boolean wasHandshakeComplete() {
        return hasEvent("onHandshakeComplete");
    }
}
