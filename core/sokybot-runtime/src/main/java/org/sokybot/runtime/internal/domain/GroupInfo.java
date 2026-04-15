package org.sokybot.runtime.internal.domain;

import java.io.Serializable;

/**
 * Represents a group (game server) configuration.
 * Simple POJO without JPA annotations.
 */
public class GroupInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name = "";
    private String gamePath = "";
    private boolean isManualOverride;
    private String manualHost = "";
    private String manualDivision = "";

    public GroupInfo() {
    }

    public GroupInfo(String name, String gamePath) {
        this.name = name;
        this.gamePath = gamePath;
    }

    public GroupInfo(int id, String name, String gamePath) {
        this.id = id;
        this.name = name;
        this.gamePath = gamePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGamePath() {
        return gamePath;
    }

    public void setGamePath(String gamePath) {
        this.gamePath = gamePath;
    }

    public boolean isManualOverride() {
        return isManualOverride;
    }

    public void setManualOverride(boolean manualOverride) {
        isManualOverride = manualOverride;
    }

    public String getManualHost() {
        return manualHost;
    }

    public void setManualHost(String manualHost) {
        this.manualHost = manualHost;
    }

    public String getManualDivision() {
        return manualDivision;
    }

    public void setManualDivision(String manualDivision) {
        this.manualDivision = manualDivision;
    }

    @Override
    public String toString() {
        return "GroupInfo{id=" + id + ", name='" + name + "', gamePath='" + gamePath + "', isManualOverride="
                + isManualOverride + ", manualHost='" + manualHost + "', manualDivision='" + manualDivision + "'}";
    }
}
