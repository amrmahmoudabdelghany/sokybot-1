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

    @Override
    public String toString() {
        return "GroupInfo{id=" + id + ", name='" + name + "', gamePath='" + gamePath + "'}";
    }
}
