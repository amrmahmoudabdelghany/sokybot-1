package org.sokybot.proxy.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.EventAdmin;
import org.sokybot.network.IPacketObserver;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.IConnectionListener;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.proxy.IProxyConnectionFactory;
import org.sokybot.proxy.ProxyConnectionFactory;
import org.sokybot.proxy.test.util.MockConnectionListener;
import org.sokybot.proxy.test.util.NettyTestUtils;
import org.sokybot.proxy.test.util.PacketTestUtils;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Base class for proxy bundle tests.
 * Provides common utilities, mocks, and test infrastructure for testing proxy
 * components.
 */
public abstract class ProxyTestBase {

    protected static final String TEST_MACHINE_ID = "test-machine";
    protected static final String TEST_HOST = "localhost";
    protected static final int TEST_PORT = 15779;
    protected static final int TEST_SERVER_PORT = 15780;

    @Mock
    protected EventAdmin mockEventAdmin;

    protected IProxyConnectionFactory factory;
    protected MockConnectionListener mockListener;
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;

    /**
     * Sets up test fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ProxyConnectionFactory impl = new ProxyConnectionFactory();
        impl.setEventAdmin(mockEventAdmin);
        impl.activate();
        factory = impl;
        mockListener = new MockConnectionListener();
    }

    /**
     * Tears down test fixtures after each test.
     */
    @AfterEach
    void tearDown() throws Exception {
        if (factory != null) {
            factory.shutdown();
        }

        if (bossGroup != null && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully().await(1, TimeUnit.SECONDS);
        }

        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully().await(1, TimeUnit.SECONDS);
        }
    }

    /**
     * Creates a new proxy connection for testing.
     */
    protected IProxyConnection createConnection(String machineId, IConnectionListener listener) {
        return factory.createConnection(machineId, listener);
    }

    /**
     * Creates a new proxy connection with the default test machine ID.
     */
    protected IProxyConnection createConnection() {
        return createConnection(TEST_MACHINE_ID, mockListener);
    }

    /**
     * Creates a new proxy connection with a mock listener.
     */
    protected IProxyConnection createConnectionWithMockListener() {
        IConnectionListener listener = mock(IConnectionListener.class);
        return createConnection(TEST_MACHINE_ID, listener);
    }

    /**
     * Waits for a condition with a timeout.
     */
    protected void waitForCondition(long timeoutMs, Runnable conditionCheck) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                conditionCheck.run();
                return;
            } catch (AssertionError e) {
                Thread.sleep(10);
            }
        }
        conditionCheck.run(); // Final check - will throw if still false
    }

    /**
     * Waits for a countdown latch with a timeout.
     */
    protected void waitForLatch(CountDownLatch latch, long timeoutMs) throws InterruptedException {
        boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Latch did not complete within " + timeoutMs + "ms");
        }
    }

    /**
     * Gets the Netty test utilities for mocking channels.
     */
    protected NettyTestUtils nettyUtils() {
        return new NettyTestUtils(workerGroup);
    }

    /**
     * Gets the packet test utilities for creating test packets.
     */
    protected PacketTestUtils packetUtils() {
        return new PacketTestUtils();
    }

    /**
     * Creates a test packet with the specified opcode.
     */
    protected ImmutablePacket createTestPacket(int opcode, byte[] data) {
        return packetUtils().createPacket(opcode, data);
    }

    /**
     * Creates a mutable packet for sending.
     */
    protected MutablePacket createMutablePacket(int opcode, byte[] data) {
        return packetUtils().createMutablePacket(opcode, data);
    }

    /**
     * Waits for a specific number of packets to be published.
     */
    protected void waitForPackets(IPacketPublisher publisher, int opcode, IPacketObserver observer,
            int expectedCount, long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(expectedCount);
        IPacketObserver countingObserver = packet -> {
            if (packet.getOpcode() == opcode) {
                observer.onPacket(packet);
                latch.countDown();
            }
        };

        publisher.subscribe(opcode, countingObserver);
        waitForLatch(latch, timeoutMs);
    }
}
