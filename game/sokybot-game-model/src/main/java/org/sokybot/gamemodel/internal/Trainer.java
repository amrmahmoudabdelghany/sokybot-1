package org.sokybot.gamemodel.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sokybot.gameevents.dto.Buff;
import org.sokybot.gameevents.dto.HotKey;
import org.sokybot.gameevents.dto.Mastery;
import org.sokybot.gameevents.dto.Skill;
import org.sokybot.gameevents.dto.SpawnData;
import org.sokybot.gameevents.enums.FreePVP;
import org.sokybot.gameevents.enums.JobType;
import org.sokybot.gameevents.enums.PVPState;
import org.sokybot.gamemodel.model.ITrainer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Trainer extends Player implements ITrainer {

    // Basic Stats
    private byte level;
    private byte maxLvl;
    private long charEXPOffset;
    private int sExpOffset;
    private long gold;
    private int skillPoint;
    private short charStatPoint;
    private byte zerkCount;
    private int gatheredExpPoint;
    private byte autoInverstExp;

    // PK / PVP
    private byte dailyPK;
    private short totalPK;
    private int pkPenaltyPoint;
    private byte zerkLvl;
    private FreePVP freePVP;
    private PVPState pvpState;
    private byte pvpFlag;

    // Job
    private String jobName;
    private JobType jobType;
    private byte jobLvl;
    private int jobExp;
    private int jobContribution;
    private int jobReward;

    // Inventory
    private byte itemInventorySize;
    private byte itemCount;
    private List<Item> inventory = new ArrayList<>();

    private byte avaterInventorySize;
    private byte avaterItemCount;
    private List<Item> avatarInventory = new ArrayList<>();

    private boolean hasMask;

    // Masteries / Skills
    private MasteryList mastryList = new MasteryList();
    private List<Skill> skills = new ArrayList<>();

    // Quests
    private List<Integer> completedQuests = new ArrayList<>();
    private List<Object> activeQuests = new ArrayList<>();

    // Other
    private int serverTime;
    // Removed NPCEntity reference to decouple from persistence
    // private NPCEntity entity;
    private byte charScale;

    // Flags
    private long guideFlag;
    private int accountId;
    private byte gmFlag;
    private byte activationFlag;
    private boolean hasTransport;
    private boolean inCombat;
    private int transportId;

    // Settings / Hotkeys / Blocked
    private List<HotKey> hotKeys = new ArrayList<>();
    private byte hpSlot;
    private byte hpValue;
    private byte mpSlot;
    private byte mpValue;
    private byte universalSlot;
    private byte universalValue;
    private byte potionDelay;

    private List<String> blockedPlayers = new ArrayList<>();

    private int unk13;
    private byte unk14;

    // Buffs
    private List<Buff> buffs = new ArrayList<>();

    // Stats (from CharacterInfoEvent)
    private int phyAtkMin;
    private int phyAtkMax;
    private int magAtkMin;
    private int magAtkMax;
    private short phyDef;
    private short magDef;
    private short hitRate;
    private short parryRate;
    private short charSTR;
    private short charINT;

    private org.sokybot.gameevents.enums.SkillCastErrorType lastError = org.sokybot.gameevents.enums.SkillCastErrorType.UNKNOWN;
    private int lastErrorTargetId;

    public Trainer() {
        super(org.sokybot.gameevents.dto.PlayerData.builder()
                .uniqueId(0).refId(0).name("None")
                .xSector(0).ySector(0).xOffset(0).yOffset(0).zOffset(0).angle((short) 0)
                .build());
    }

    public Trainer(SpawnData data) {
        super(data);
    }

    // Helper methods
    public void addItem(Item item) {
        inventory.add(item);
    }

    public void addAvaterItem(Item item) {
        avatarInventory.add(item);
    }

    public void addMastery(Mastery m) {
        mastryList.add(m);
    }

    public void addSkill(Skill s) {
        skills.add(s);
    }

    public void addBuff(Buff b) {
        buffs.add(b);
    }

    public void addCompletedQuest(int id) {
        completedQuests.add(id);
    }

    public void addActiveQuest(Object q) {
        activeQuests.add(q);
    }

    public void addHotKey(HotKey hk) {
        hotKeys.add(hk);
    }

    public void setCharHP(int hp) {
        setCurrentHP(hp);
    }

    public void setCharMP(int mp) {
        setCurrentMP(mp);
    }

    public static class MasteryList extends ArrayList<Mastery> {
        public Optional<Mastery> findMastry(int id) {
            return this.stream().filter(m -> m.getId() == id).findFirst();
        }
    }

    @Override
    public boolean hasSkill(int skillId) {
        return skills.stream().anyMatch(s -> s.getRefId() == skillId);
    }

    @Override
    public Optional<Skill> findSkill(String name) {
        return skills.stream()
                .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(name))
                .findFirst();
    }
}
