package org.sokybot.gameevents.dto;

public class AgentInfo {

    private final short id;
    private final String name;
    private final short onlineCount;
    private final short capacity;
    private final byte status;

    public AgentInfo(short id, String name, short onlineCount, short capacity, byte status) {
        this.id = id;
        this.name = name;
        this.onlineCount = onlineCount;
        this.capacity = capacity;
        this.status = status;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public short getOnlineCount() {
        return onlineCount;
    }

    public short getCapacity() {
        return capacity;
    }

    public byte getStatus() {
        return status;
    }
}
