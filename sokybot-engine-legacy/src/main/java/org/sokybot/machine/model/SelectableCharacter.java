package org.sokybot.machine.model;

import org.sokybot.game.enums.AcademyMemberClass;
import org.sokybot.game.enums.GuildMemberClass;
import lombok.Data;

@Data
public class SelectableCharacter {
    private int charId;
    private String name;
    private byte charScale;
    private byte level;
    private long charEXPOffset;
    private short charSTR;
    private short charINT;
    private short charStatPoint;
    private int charHP;
    private int charMP;
    private byte isDeleting;
    private int deleteTime;
    private GuildMemberClass GMC;
    private String currentGuildName;
    private AcademyMemberClass AMC;

    // Manual getters/setters as backup/convenience
    public int getCharId() { return charId; }
    public void setCharId(int charId) { this.charId = charId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public byte getCharScale() { return charScale; }
    public void setCharScale(byte charScale) { this.charScale = charScale; }
    public byte getLevel() { return level; }
    public void setLevel(byte level) { this.level = level; }
    public long getCharEXPOffset() { return charEXPOffset; }
    public void setCharEXPOffset(long charEXPOffset) { this.charEXPOffset = charEXPOffset; }
    public short getCharSTR() { return charSTR; }
    public void setCharSTR(short charSTR) { this.charSTR = charSTR; }
    public short getCharINT() { return charINT; }
    public void setCharINT(short charINT) { this.charINT = charINT; }
    public short getCharStatPoint() { return charStatPoint; }
    public void setCharStatPoint(short charStatPoint) { this.charStatPoint = charStatPoint; }
    public int getCharHP() { return charHP; }
    public void setCharHP(int charHP) { this.charHP = charHP; }
    public int getCharMP() { return charMP; }
    public void setCharMP(int charMP) { this.charMP = charMP; }
    public byte getIsDeleting() { return isDeleting; }
    public void setIsDeleting(byte isDeleting) { this.isDeleting = isDeleting; }
    public int getDeleteTime() { return deleteTime; }
    public void setDeleteTime(int deleteTime) { this.deleteTime = deleteTime; }
    public GuildMemberClass getGMC() { return GMC; }
    public void setGMC(GuildMemberClass GMC) { this.GMC = GMC; }
    public String getCurrentGuildName() { return currentGuildName; }
    public void setCurrentGuildName(String name) { this.currentGuildName = name; }
    public AcademyMemberClass getAMC() { return AMC; }
    public void setAMC(AcademyMemberClass AMC) { this.AMC = AMC; }
}
