package org.sokybot.runtime.internal.domain;

import java.io.Serializable;

/**
 * Represents a machine (bot) configuration within a group.
 * Simple POJO without JPA annotations.
 */
public class MachineInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int groupId;
    private String machineName = "";

    public MachineInfo() {
    }

    public MachineInfo(int groupId, String machineName) {
        this.groupId = groupId;
        this.machineName = machineName;
    }

    public MachineInfo(int id, int groupId, String machineName) {
        this.id = id;
        this.groupId = groupId;
        this.machineName = machineName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    @Override
    public String toString() {
        return "MachineInfo{id=" + id + ", groupId=" + groupId + ", machineName='" + machineName + "'}";
    }
}
