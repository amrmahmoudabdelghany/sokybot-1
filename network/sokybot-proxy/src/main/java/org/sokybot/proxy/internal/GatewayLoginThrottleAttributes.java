package org.sokybot.proxy.internal;

import io.netty.util.AttributeKey;

/** Per game-server {@link io.netty.channel.Channel} timestamp for last gateway 0x6102 encode. */
public final class GatewayLoginThrottleAttributes {

    public static final AttributeKey<Long> GATEWAY_LOGIN_6102_LAST_SENT_AT_MS =
            AttributeKey.valueOf("sokybot.gatewayLogin6102LastSentAtMs");

    private GatewayLoginThrottleAttributes() {}
}
