package org.sokybot.proxy.test.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Utility class for creating Netty mocks and test infrastructure.
 */
public class NettyTestUtils {

    private final EventLoopGroup eventLoopGroup;

    public NettyTestUtils(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
    }

    /**
     * Creates a mock Channel that can be used in tests.
     */
    public Channel createMockChannel() {
        Channel channel = mock(Channel.class);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);

        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        when(channel.localAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 54321));
        when(channel.eventLoop()).thenReturn(eventLoopGroup.next());

        // Mock write operations
        ChannelFuture writeFuture = createMockSucceededFuture(channel);
        when(channel.write(any())).thenReturn(writeFuture);
        when(channel.writeAndFlush(any())).thenReturn(writeFuture);

        // Mock close operations
        ChannelFuture closeFuture = createMockSucceededFuture(channel);
        when(channel.close()).thenReturn(closeFuture);

        return channel;
    }

    /**
     * Creates a mock ChannelFuture that completes successfully.
     */
    public ChannelFuture createMockSucceededFuture(Channel channel) {
        ChannelFuture future = mock(ChannelFuture.class);
        when(future.channel()).thenReturn(channel);
        when(future.isSuccess()).thenReturn(true);
        when(future.isDone()).thenReturn(true);
        when(future.isCancelled()).thenReturn(false);
        when(future.cause()).thenReturn(null);

        // Mock addListener to immediately invoke the listener
        when(future.addListener(any(GenericFutureListener.class))).thenAnswer(new Answer<ChannelFuture>() {
            @Override
            public ChannelFuture answer(InvocationOnMock invocation) {
                GenericFutureListener listener = invocation.getArgument(0);
                try {
                    listener.operationComplete(future);
                } catch (Exception e) {
                    // Ignore exceptions in test
                }
                return future;
            }
        });

        return future;
    }

    /**
     * Creates a mock ChannelFuture that fails.
     */
    public ChannelFuture createMockFailedFuture(Channel channel, Throwable cause) {
        ChannelFuture future = mock(ChannelFuture.class);
        when(future.channel()).thenReturn(channel);
        when(future.isSuccess()).thenReturn(false);
        when(future.isDone()).thenReturn(true);
        when(future.isCancelled()).thenReturn(false);
        when(future.cause()).thenReturn(cause);

        return future;
    }

    /**
     * Creates a mock ChannelHandlerContext.
     */
    public ChannelHandlerContext createMockContext(Channel channel) {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);

        when(ctx.channel()).thenReturn(channel);
        when(ctx.pipeline()).thenReturn(pipeline);
        when(ctx.executor()).thenReturn(eventLoopGroup.next());

        ChannelFuture writeFuture = createMockSucceededFuture(channel);
        when(ctx.write(any())).thenReturn(writeFuture);
        when(ctx.writeAndFlush(any())).thenReturn(writeFuture);

        return ctx;
    }

    /**
     * Creates a mock ChannelPromise.
     */
    public ChannelPromise createMockPromise(Channel channel) {
        ChannelPromise promise = mock(ChannelPromise.class);
        when(promise.channel()).thenReturn(channel);
        when(promise.setSuccess()).thenReturn(promise);
        when(promise.setFailure(any(Throwable.class))).thenReturn(promise);

        return promise;
    }

    /**
     * Waits for a channel operation to complete.
     */
    public void waitForChannelOperation(ChannelFuture future, long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        future.addListener(f -> latch.countDown());

        boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new AssertionError("Channel operation did not complete within " + timeoutMs + "ms");
        }
    }

    /**
     * Creates an InetSocketAddress for testing.
     */
    public InetSocketAddress createSocketAddress(String host, int port) {
        return new InetSocketAddress(host, port);
    }
}
