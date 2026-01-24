package org.sokybot.network;

import org.sokybot.network.packet.ImmutablePacket;

public interface IPacketObserver {

    void onPacket(ImmutablePacket packet);
}
