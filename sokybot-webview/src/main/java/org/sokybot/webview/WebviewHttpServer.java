package org.sokybot.webview;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple HTTP server to serve the React webview application.
 * Serves static files from target/frontend/dist directory or bundle resources.
 */
public class WebviewHttpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebviewHttpServer.class);
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final int port;
    private final Path webappPath;
    
    public WebviewHttpServer(int port, Path webappPath) {
        this.port = port;
        this.webappPath = webappPath;
    }
    
    /**
     * Start the HTTP server.
     */
    public void start() throws InterruptedException {
        logger.info("Starting Webview HTTP Server on port {}", port);
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new HttpServerCodec());
                     ch.pipeline().addLast(new HttpObjectAggregator(65536));
                     ch.pipeline().addLast(new WebviewHttpHandler(webappPath));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture f = b.bind(port).sync();
            if (f.isSuccess()) {
                serverChannel = f.channel();
                logger.info("Webview HTTP Server started on http://localhost:{}/", port);
            } else {
                logger.error("Failed to bind HTTP server to port {}", port);
                throw new RuntimeException("Failed to bind HTTP server to port " + port, f.cause());
            }
        } catch (Exception e) {
            logger.error("Failed to start HTTP server on port {}", port, e);
            // Clean up on failure
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            throw e;
        }
    }
    
    /**
     * Stop the HTTP server.
     */
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        logger.info("Webview HTTP Server stopped");
    }
    
    /**
     * HTTP handler for serving static files.
     */
    private static class WebviewHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        
        private final Path webappPath;
        
        public WebviewHttpHandler(Path webappPath) {
            this.webappPath = webappPath;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            String uri = request.uri();
            
            // Handle CORS
            FullHttpResponse response;
            
            // Default to index.html for SPA routing
            if (uri.equals("/") || !uri.contains(".")) {
                uri = "/index.html";
            }
            
            // Clean uri
            if (uri.contains("?")) {
                uri = uri.substring(0, uri.indexOf("?"));
            }
            
            Path filePath = webappPath.resolve(uri.startsWith("/") ? uri.substring(1) : uri);
            
            // Security check: ensure file is within webapp directory
            if (!filePath.normalize().startsWith(webappPath.normalize())) {
                response = createErrorResponse(HttpResponseStatus.FORBIDDEN, "Forbidden");
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                return;
            }
            
            File file = filePath.toFile();
            
            if (file.exists() && file.isFile()) {
                byte[] content = Files.readAllBytes(file.toPath());
                String contentType = getContentType(file.getName());
                
                ByteBuf contentBuf = Unpooled.copiedBuffer(content);
                response = new DefaultFullHttpResponse(request.protocolVersion(), 
                    HttpResponseStatus.OK, contentBuf);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentBuf.readableBytes());
                
                // CORS headers
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
            } else {
                // 404 - serve index.html for SPA routing
                if (uri.endsWith(".html") || uri.equals("/index.html")) {
                    response = createErrorResponse(HttpResponseStatus.NOT_FOUND, "Not Found");
                } else {
                    // For other resources, try index.html for SPA
                    filePath = webappPath.resolve("index.html");
                    if (filePath.toFile().exists()) {
                        byte[] content = Files.readAllBytes(filePath);
                        ByteBuf contentBuf = Unpooled.copiedBuffer(content);
                        response = new DefaultFullHttpResponse(request.protocolVersion(), 
                            HttpResponseStatus.OK, contentBuf);
                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentBuf.readableBytes());
                    } else {
                        response = createErrorResponse(HttpResponseStatus.NOT_FOUND, "Not Found");
                    }
                }
            }
            
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (!keepAlive) {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.writeAndFlush(response);
            }
        }
        
        private FullHttpResponse createErrorResponse(HttpResponseStatus status, String message) {
            ByteBuf content = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(
                io.netty.handler.codec.http.HttpVersion.HTTP_1_1, status, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            return response;
        }
        
        private String getContentType(String filename) {
            if (filename.endsWith(".html")) {
                return "text/html; charset=utf-8";
            } else if (filename.endsWith(".js")) {
                return "application/javascript; charset=utf-8";
            } else if (filename.endsWith(".css")) {
                return "text/css; charset=utf-8";
            } else if (filename.endsWith(".json")) {
                return "application/json; charset=utf-8";
            } else if (filename.endsWith(".png")) {
                return "image/png";
            } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (filename.endsWith(".svg")) {
                return "image/svg+xml";
            } else if (filename.endsWith(".ico")) {
                return "image/x-icon";
            } else {
                return "application/octet-stream";
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("HTTP handler error", cause);
            ctx.close();
        }
    }
}
