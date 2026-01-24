package org.sokybot.proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.IPacketSubscription;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.proxy.internal.SimplePacketPublisher;
import org.sokybot.proxy.test.ProxyTestBase;

/**
 * Comprehensive unit tests for SimplePacketPublisher (IPacketPublisher implementation).
 */
@DisplayName("PacketPublisher Tests")
class PacketPublisherTest extends ProxyTestBase {
    
    @Test
    @DisplayName("Should subscribe single observer for single opcode")
    void testSingleSubscription() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        IPacketSubscription subscription = publisher.subscribe(0x2001, observer);
        
        assertNotNull(subscription);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        verify(observer, times(1)).onPacket(packet);
    }
    
    @Test
    @DisplayName("Should subscribe observer for multiple opcodes")
    void testMultiOpcodeSubscription() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        IPacketSubscription subscription = publisher.subscribe(observer, 0x2001, 0xA103, 0x6001);
        
        assertNotNull(subscription);
        
        ImmutablePacket packet1 = packetUtils().createPacket(0x2001, new byte[]{1});
        ImmutablePacket packet2 = packetUtils().createPacket(0xA103, new byte[]{2});
        ImmutablePacket packet3 = packetUtils().createPacket(0x6001, new byte[]{3});
        
        publisher.publish(packet1);
        publisher.publish(packet2);
        publisher.publish(packet3);
        
        verify(observer, times(1)).onPacket(packet1);
        verify(observer, times(1)).onPacket(packet2);
        verify(observer, times(1)).onPacket(packet3);
    }
    
    @Test
    @DisplayName("Should publish to all observers for same opcode")
    void testPublishToAllObservers() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer1 = mock(IPacketObserver.class);
        IPacketObserver observer2 = mock(IPacketObserver.class);
        IPacketObserver observer3 = mock(IPacketObserver.class);
        
        publisher.subscribe(0x2001, observer1);
        publisher.subscribe(0x2001, observer2);
        publisher.subscribe(0x2001, observer3);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        verify(observer1, times(1)).onPacket(packet);
        verify(observer2, times(1)).onPacket(packet);
        verify(observer3, times(1)).onPacket(packet);
    }
    
    @Test
    @DisplayName("Should not publish to observers for different opcode")
    void testOpcodeSpecificPublishing() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer1 = mock(IPacketObserver.class);
        IPacketObserver observer2 = mock(IPacketObserver.class);
        
        publisher.subscribe(0x2001, observer1);
        publisher.subscribe(0xA103, observer2);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        verify(observer1, times(1)).onPacket(packet);
        verify(observer2, never()).onPacket(any());
    }
    
    @Test
    @DisplayName("Should handle observer exceptions without breaking other observers")
    void testObserverExceptionHandling() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        AtomicInteger successCount = new AtomicInteger(0);
        
        IPacketObserver failingObserver = packet -> {
            throw new RuntimeException("Test exception");
        };
        
        IPacketObserver workingObserver1 = packet -> {
            successCount.incrementAndGet();
        };
        
        IPacketObserver workingObserver2 = packet -> {
            successCount.incrementAndGet();
        };
        
        publisher.subscribe(0x2001, failingObserver);
        publisher.subscribe(0x2001, workingObserver1);
        publisher.subscribe(0x2001, workingObserver2);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            publisher.publish(packet);
        });
        
        // Both working observers should have received the packet
        assertEquals(2, successCount.get());
    }
    
    @Test
    @DisplayName("Should unsubscribe single observer correctly")
    void testUnsubscribeSingleObserver() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        IPacketSubscription subscription = publisher.subscribe(0x2001, observer);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        verify(observer, times(1)).onPacket(packet);
        
        subscription.unsubscribe();
        
        publisher.publish(packet);
        verify(observer, times(1)).onPacket(packet); // Should not increment
    }
    
    @Test
    @DisplayName("Should unsubscribe only specified observer from multi-observer opcode")
    void testUnsubscribeOneFromMultiple() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer1 = mock(IPacketObserver.class);
        IPacketObserver observer2 = mock(IPacketObserver.class);
        IPacketObserver observer3 = mock(IPacketObserver.class);
        
        IPacketSubscription sub1 = publisher.subscribe(0x2001, observer1);
        IPacketSubscription sub2 = publisher.subscribe(0x2001, observer2);
        IPacketSubscription sub3 = publisher.subscribe(0x2001, observer3);
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        verify(observer1, times(1)).onPacket(packet);
        verify(observer2, times(1)).onPacket(packet);
        verify(observer3, times(1)).onPacket(packet);
        
        sub2.unsubscribe();
        
        publisher.publish(packet);
        
        verify(observer1, times(2)).onPacket(packet);
        verify(observer2, times(1)).onPacket(packet); // Should not increment
        verify(observer3, times(2)).onPacket(packet);
    }
    
    @Test
    @DisplayName("Should unsubscribe all opcodes for multi-opcode subscription")
    void testUnsubscribeMultiOpcodeSubscription() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        IPacketSubscription subscription = publisher.subscribe(observer, 0x2001, 0xA103, 0x6001);
        
        ImmutablePacket packet1 = packetUtils().createPacket(0x2001, new byte[]{1});
        ImmutablePacket packet2 = packetUtils().createPacket(0xA103, new byte[]{2});
        ImmutablePacket packet3 = packetUtils().createPacket(0x6001, new byte[]{3});
        
        publisher.publish(packet1);
        publisher.publish(packet2);
        publisher.publish(packet3);
        
        verify(observer, times(1)).onPacket(packet1);
        verify(observer, times(1)).onPacket(packet2);
        verify(observer, times(1)).onPacket(packet3);
        
        subscription.unsubscribe();
        
        publisher.publish(packet1);
        publisher.publish(packet2);
        publisher.publish(packet3);
        
        verify(observer, times(1)).onPacket(packet1); // Should not increment
        verify(observer, times(1)).onPacket(packet2); // Should not increment
        verify(observer, times(1)).onPacket(packet3); // Should not increment
    }
    
    @Test
    @DisplayName("Should handle multiple unsubscribe calls safely")
    void testMultipleUnsubscribeCalls() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        IPacketSubscription subscription = publisher.subscribe(0x2001, observer);
        
        assertDoesNotThrow(() -> {
            subscription.unsubscribe();
            subscription.unsubscribe();
            subscription.unsubscribe();
        });
    }
    
    @Test
    @DisplayName("Should handle empty opcode array in subscription")
    void testEmptyOpcodeArraySubscription() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        
        // Should not throw with empty array
        assertDoesNotThrow(() -> {
            IPacketSubscription subscription = publisher.subscribe(observer);
            assertNotNull(subscription);
        });
    }
    
    @Test
    @DisplayName("Should handle concurrent subscriptions")
    void testConcurrentSubscriptions() throws InterruptedException {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        int threadCount = 10;
        int subscriptionsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<IPacketSubscription> allSubscriptions = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    List<IPacketSubscription> subscriptions = new ArrayList<>();
                    for (int j = 0; j < subscriptionsPerThread; j++) {
                        int opcode = 0x2000 + threadId * subscriptionsPerThread + j;
                        IPacketObserver observer = mock(IPacketObserver.class);
                        IPacketSubscription sub = publisher.subscribe(opcode, observer);
                        subscriptions.add(sub);
                    }
                    synchronized (allSubscriptions) {
                        allSubscriptions.addAll(subscriptions);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount * subscriptionsPerThread, allSubscriptions.size());
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Should handle concurrent publish and subscribe")
    void testConcurrentPublishAndSubscribe() throws InterruptedException {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        AtomicInteger publishCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(100);
        
        // Start publishing
        for (int i = 0; i < 50; i++) {
            final int opcode = 0x2000 + (i % 10);
            executor.submit(() -> {
                try {
                    ImmutablePacket packet = packetUtils().createPacket(opcode, new byte[]{1});
                    publisher.publish(packet);
                    publishCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Start subscribing
        for (int i = 0; i < 50; i++) {
            final int opcode = 0x2000 + (i % 10);
            executor.submit(() -> {
                try {
                    IPacketObserver observer = mock(IPacketObserver.class);
                    publisher.subscribe(opcode, observer);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(publishCount.get() > 0);
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Should handle many observers for same opcode")
    void testManyObserversForSameOpcode() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        List<IPacketObserver> observers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            IPacketObserver observer = mock(IPacketObserver.class);
            observers.add(observer);
            publisher.subscribe(0x2001, observer);
        }
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        for (IPacketObserver observer : observers) {
            verify(observer, times(1)).onPacket(packet);
        }
    }
    
    @Test
    @DisplayName("Should handle packets with no subscribers")
    void testPublishWithNoSubscribers() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        
        // Should not throw when no subscribers
        assertDoesNotThrow(() -> {
            publisher.publish(packet);
        });
    }
    
    @Test
    @DisplayName("Should handle re-subscribing same observer")
    void testResubscribingSameObserver() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        
        IPacketSubscription sub1 = publisher.subscribe(0x2001, observer);
        IPacketSubscription sub2 = publisher.subscribe(0x2001, observer); // Same observer again
        
        ImmutablePacket packet = packetUtils().createPacket(0x2001, new byte[]{1, 2, 3});
        publisher.publish(packet);
        
        // Should be called twice (once for each subscription)
        verify(observer, times(2)).onPacket(packet);
    }
    
    @Test
    @DisplayName("Should handle different packet sizes")
    void testDifferentPacketSizes() {
        IProxyConnection connection = createConnection();
        SimplePacketPublisher publisher = (SimplePacketPublisher) connection.getPacketPublisher();
        
        IPacketObserver observer = mock(IPacketObserver.class);
        publisher.subscribe(0x2001, observer);
        
        // Empty packet
        ImmutablePacket emptyPacket = packetUtils().createEmptyPacket(0x2001);
        publisher.publish(emptyPacket);
        
        // Small packet
        ImmutablePacket smallPacket = packetUtils().createPacket(0x2001, new byte[]{1});
        publisher.publish(smallPacket);
        
        // Large packet
        byte[] largeData = new byte[1024];
        ImmutablePacket largePacket = packetUtils().createPacket(0x2001, largeData);
        publisher.publish(largePacket);
        
        verify(observer, times(1)).onPacket(emptyPacket);
        verify(observer, times(1)).onPacket(smallPacket);
        verify(observer, times(1)).onPacket(largePacket);
    }
}