package org.sokybot.webview.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MachineInfoDto {
    private final String machineId;
    private final String name;
    private final String groupName;
    private final boolean isRunning;

    @JsonCreator
    public MachineInfoDto(
            @JsonProperty("machineId") String machineId,
            @JsonProperty("name") String name,
            @JsonProperty("groupName") String groupName,
            @JsonProperty("isRunning") boolean isRunning) {
        this.machineId = machineId;
        this.name = name;
        this.groupName = groupName;
        this.isRunning = isRunning;
    }

    public String getMachineId() { return machineId; }
    public String getName() { return name; }
    public String getGroupName() { return groupName; }
    public boolean isRunning() { return isRunning; }
}
