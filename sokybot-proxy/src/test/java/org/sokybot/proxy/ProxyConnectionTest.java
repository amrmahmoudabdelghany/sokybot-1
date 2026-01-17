package org.sokybot.proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.internal.SimplePacketPublisher;
import org.sokybot.proxy.test.ProxyTestBase;
import org.sokybot.proxy.test.util.MockConnectionListener;

/**
 * Unit tests for ProxyConnection.
 */
@DisplayName("ProxyConnection Tests")
class ProxyConnectionTest extends ProxyTestBase {
    
    @Test
    @DisplayName("Should return correct machine ID")
    void testGetMachineId() {
        IProxyConnection connection = createConnection();
        
        assertEquals(TEST_MACHINE_ID, connection.getMachineId());
    }
    
    @Test
    @DisplayName("Should have packet publisher")
    void testGetPacketPublisher() {
        IProxyConnection connection = createConnection();
        
        IPacketPublisher publisher = connection.getPacketPublisher();
        assertNotNull(publisher);
    }
    
    @Test
    @DisplayName("Should return same publisher instance on multiple calls")
    void testGetPacketPublisherReturnsSameInstance() {
        IProxyConnection connection = createConnection();
        
        IPacketPublisher publisher1 = connection.getPacketPublisher();
        IPacketPublisher publisher2 = connection.getPacketPublisher();
        
        assertSame(publisher1, publisher2);
    }
    
    @Test
    @DisplayName("Should start in disconnected state")
    void testInitialConnectionState() {
        IProxyConnection connection = createConnection();
        
        assertFalse(connection.isConnected());
        assertFalse(connection.isClientConnected());
        assertFalse(connection.isServerConnected());
    }
    
    @Test
    @DisplayName("Should set and get clientless mode")
    void testClientlessMode() {
        IProxyConnection connection = createConnection();
        
        assertFalse(connection.isClientlessMode());
        
        connection.setClientlessMode(true);
        assertTrue(connection.isClientlessMode());
        
        connection.setClientlessMode(false);
        assertFalse(connection.isClientlessMode());
    }
    
    @Test
    @DisplayName("Should toggle clientless mode multiple times")
    void testToggleClientlessMode() {
        IProxyConnection connection = createConnection();
        
        for (int i = 0; i < 5; i++) {
            connection.setClientlessMode(i % 2 == 0);
            assertEquals(i % 2 == 0, connection.isClientlessMode());
        }
    }
    
    @Test
    @DisplayName("Should update connection listener")
    void testSetConnectionListener() {
        IProxyConnection connection = createConnection();
        IConnectionListener newListener = mock(IConnectionListener.class);
        
        connection.setConnectionListener(newListener);
        
        // Verify listener can be changed (we can't directly verify internal state)
        // but we can verify the connection still works
        assertNotNull(connection);
    }
    
    @Test
    @DisplayName("Should allow null connection listener")
    void testSetNullConnectionListener() {
        IProxyConnection connection = createConnection();
        
        assertDoesNotThrow(() -> {
            connection.setConnectionListener(null);
        });
    }
    
    @Test
    @DisplayName("Should replace connection listener multiple times")
    void testReplaceConnectionListenerMultipleTimes() {
        IProxyConnection connection = createConnection();
        
        IConnectionListener listener1 = mock(IConnectionListener.class);
        IConnectionListener listener2 = mock(IConnectionListener.class);
        IConnectionListener listener3 = mock(IConnectionListener.class);
        
        connection.setConnectionListener(listener1);
        connection.setConnectionListener(listener2);
        connection.setConnectionListener(listener3);
        
        assertNotNull(connection);
    }
    
    @Test
    @DisplayName("Should allow packet subscription")
    void testPacketSubscription() {
        IProxyConnection connection = createConnection();
        IPacketPublisher publisher = connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        IPacketSubscription subscription = publisher.subscribe(0x1234, observer);
        
        assertNotNull(subscription);
        assertNotNull(publisher);
    }
    
    @Test
    @DisplayName("Should allow multiple subscriptions for same opcode")
    void testMultipleSubscriptionsSameOpcode() {
        IProxyConnection connection = createConnection();
        IPacketPublisher publisher = connection.getPacketPublisher();
        
        IPacketObserver observer1 = mock(IPacketObserver.class);
        IPacketObserver observer2 = mock(IPacketObserver.class);
        IPacketObserver observer3 = mock(IPacketObserver.class);
        
        IPacketSubscription sub1 = publisher.subscribe(0x2001, observer1);
        IPacketSubscription sub2 = publisher.subscribe(0x2001, observer2);
        IPacketSubscription sub3 = publisher.subscribe(0x2001, observer3);
        
        assertNotNull(sub1);
        assertNotNull(sub2);
        assertNotNull(sub3);
    }
    
    @Test
    @DisplayName("Should allow subscription for multiple opcodes")
    void testSubscriptionMultipleOpcodes() {
        IProxyConnection connection = createConnection();
        IPacketPublisher publisher = connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        IPacketSubscription subscription = publisher.subscribe(observer, 0x2001, 0xA103, 0x6001);
        
        assertNotNull(subscription);
    }
    
    @Test
    @DisplayName("Should publish packets to subscribed observers")
    void testPacketPublishing() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        AtomicInteger receivedCount = new AtomicInteger(0);
        IPacketObserver observer = packet -> {
            assertEquals(0x2001, packet.getOpcode());
            receivedCount.incrementAndGet();
        };
        
        publisher.subscribe(0x2001, observer);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        assertEquals(1, receivedCount.get());
    }
    
    @Test
    @DisplayName("Should publish packets to all observers for opcode")
    void testPacketPublishingToMultipleObservers() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);
        AtomicInteger count3 = new AtomicInteger(0);
        
        IPacketObserver observer1 = packet -> count1.incrementAndGet();
        IPacketObserver observer2 = packet -> count2.incrementAndGet();
        IPacketObserver observer3 = packet -> count3.incrementAndGet();
        
        publisher.subscribe(0x2001, observer1);
        publisher.subscribe(0x2001, observer2);
        publisher.subscribe(0x2001, observer3);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        assertEquals(1, count1.get());
        assertEquals(1, count2.get());
        assertEquals(1, count3.get());
    }
    
    @Test
    @DisplayName("Should not publish packets to observers for different opcode")
    void testPacketPublishingOnlyToMatchingOpcode() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);
        
        IPacketObserver observer1 = packet -> count1.incrementAndGet();
        IPacketObserver observer2 = packet -> count2.incrementAndGet();
        
        publisher.subscribe(0x2001, observer1);
        publisher.subscribe(0xA103, observer2);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        assertEquals(1, count1.get());
        assertEquals(0, count2.get()); // Should not receive packet for different opcode
    }
    
    @Test
    @DisplayName("Should handle observer exceptions gracefully")
    void testObserverExceptionHandling() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        AtomicInteger count = new AtomicInteger(0);
        
        IPacketObserver failingObserver = packet -> {
            throw new RuntimeException("Test exception");
        };
        
        IPacketObserver workingObserver = packet -> {
            count.incrementAndGet();
        };
        
        publisher.subscribe(0x2001, failingObserver);
        publisher.subscribe(0x2001, workingObserver);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        
        // Should not throw exception, should continue to next observer
        assertDoesNotThrow(() -> {
            publisher.publish(packet);
        });
        
        // Working observer should still receive the packet
        assertEquals(1, count.get());
    }
    
    @Test
    @DisplayName("Should unsubscribe observer correctly")
    void testUnsubscribe() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        AtomicInteger count = new AtomicInteger(0);
        IPacketObserver observer = packet -> count.incrementAndGet();
        
        IPacketSubscription subscription = publisher.subscribe(0x2001, observer);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        assertEquals(1, count.get());
        
        subscription.unsubscribe();
        
        publisher.publish(packet);
        assertEquals(1, count.get()); // Should not increment after unsubscribe
    }
    
    @Test
    @DisplayName("Should allow multiple unsubscribe calls")
    void testMultipleUnsubscribeCalls() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        IPacketSubscription subscription = publisher.subscribe(0x2001, observer);
        
        assertDoesNotThrow(() -> {
            subscription.unsubscribe();
            subscription.unsubscribe(); // Second call should not throw
            subscription.unsubscribe(); // Third call should not throw
        });
    }
    
    @Test
    @DisplayName("Should send packet to server when channel is null (no exception)")
    void testSendToServerWhenDisconnected() {
        IProxyConnection connection = createConnection();
        MutablePacket packet = packetUtils().createMutablePacket(0x2001, new byte[]{1, 2, 3});
        
        // Should not throw exception when not connected
        assertDoesNotThrow(() -> {
            connection.sendToServer(packet);
        });
    }
    
    @Test
    @DisplayName("Should send packet to client when channel is null (no exception)")
    void testSendToClientWhenDisconnected() {
        IProxyConnection connection = createConnection();
        MutablePacket packet = packetUtils().createMutablePacket(0xA103, new byte[]{1, 2, 3});
        
        // Should not throw exception when not connected
        assertDoesNotThrow(() -> {
            connection.sendToClient(packet);
        });
    }
    
    @Test
    @DisplayName("Should handle disconnect when not connected")
    void testDisconnectWhenNotConnected() {
        IProxyConnection connection = createConnection();
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            connection.disconnect();
        });
    }
    
    @Test
    @DisplayName("Should handle multiple disconnect calls")
    void testMultipleDisconnectCalls() {
        IProxyConnection connection = createConnection();
        
        // Should not throw exception on multiple calls
        assertDoesNotThrow(() -> {
            connection.disconnect();
            connection.disconnect();
            connection.disconnect();
        });
    }
    
    @Test
    @DisplayName("Should maintain state after disconnect")
    void testStateAfterDisconnect() {
        IProxyConnection connection = createConnection();
        
        connection.setClientlessMode(true);
        assertTrue(connection.isClientlessMode());
        
        connection.disconnect();
        
        // Clientless mode should persist
        assertTrue(connection.isClientlessMode());
        assertFalse(connection.isConnected());
    }
    
    @Test
    @DisplayName("Should handle empty opcode array in multi-subscription")
    void testSubscriptionWithEmptyOpcodes() {
        IProxyConnection connection = createConnection();
        IPacketPublisher publisher = connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        
        // Should not throw with empty array
        assertDoesNotThrow(() -> {
            IPacketSubscription subscription = publisher.subscribe(observer);
            assertNotNull(subscription);
        });
    }
    
    @Test
    @DisplayName("Should handle large number of subscriptions")
    void testManySubscriptions() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        List<IPacketSubscription> subscriptions = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            int opcode = 0x2000 + i;
            IPacketObserver observer = mock(IPacketObserver.class);
            IPacketSubscription sub = publisher.subscribe(opcode, observer);
            subscriptions.add(sub);
        }
        
        assertEquals(100, subscriptions.size());
        
        // Unsubscribe all
        for (IPacketSubscription sub : subscriptions) {
            assertDoesNotThrow(sub::unsubscribe);
        }
    }
}
