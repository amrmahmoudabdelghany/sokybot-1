package org.sokybot.webview.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MachineActionResultDto {
    private final String status;
    private final String machineId;

    @JsonCreator
    public MachineActionResultDto(
            @JsonProperty("status") String status,
            @JsonProperty("machineId") String machineId) {
        this.status = status;
        this.machineId = machineId;
    }

    public String getStatus() { return status; }
    public String getMachineId() { return machineId; }
}
