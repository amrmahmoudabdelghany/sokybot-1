package org.sokybot.gamemodel.model;

import java.util.List;
import java.util.Optional;

import org.sokybot.gameevents.dto.Buff;
import org.sokybot.gameevents.dto.HotKey;
import org.sokybot.gameevents.dto.Mastery;
import org.sokybot.gameevents.dto.Skill;
import org.sokybot.gameevents.enums.FreePVP;
import org.sokybot.gameevents.enums.JobType;
import org.sokybot.gameevents.enums.PVPState;
// import org.sokybot.persistence.entities.NPCEntity; // Removed

public interface ITrainer extends IPlayer {

    byte getLevel();

    byte getMaxLvl();

    long getCharEXPOffset();

    int getSExpOffset();

    long getGold();

    int getSkillPoint();

    short getCharStatPoint();

    byte getZerkCount();

    byte getDailyPK();

    short getTotalPK();

    int getPkPenaltyPoint();

    byte getZerkLvl();

    FreePVP getFreePVP();

    PVPState getPvpState();

    byte getPvpFlag();

    String getJobName();

    JobType getJobType();

    byte getJobLvl();

    int getJobExp();

    int getJobContribution();

    int getJobReward();

    byte getItemInventorySize();

    byte getItemCount();

    List<? extends IItem> getInventory();

    byte getAvaterInventorySize();

    byte getAvaterItemCount();

    List<? extends IItem> getAvatarInventory();

    boolean isHasMask();

    List<Mastery> getMastryList();

    List<Skill> getSkills();

    List<Integer> getCompletedQuests();

    List<Object> getActiveQuests();

    int getServerTime();

    // NPCEntity getEntity(); // Removed to decouple from persistence
    byte getCharScale();

    List<HotKey> getHotKeys();

    List<Buff> getBuffs();

    int getPhyAtkMin();

    int getPhyAtkMax();

    int getMagAtkMin();

    int getMagAtkMax();

    short getPhyDef();

    short getMagDef();

    short getHitRate();

    short getParryRate();

    short getCharSTR();

    short getCharINT();

    boolean hasSkill(int skillId);

    Optional<Skill> findSkill(String name);

    org.sokybot.gameevents.enums.SkillCastErrorType getLastError();

    int getLastErrorTargetId();
}
