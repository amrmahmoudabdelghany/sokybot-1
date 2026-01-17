package org.sokybot.gamemodel.model;

import java.util.List;
import java.util.Optional;

import org.sokybot.game.dto.Buff;
import org.sokybot.game.dto.HotKey;
import org.sokybot.game.dto.Mastery;
import org.sokybot.game.dto.Skill;
import org.sokybot.game.enums.FreePVP;
import org.sokybot.game.enums.JobType;
import org.sokybot.game.enums.PVPState;
import org.sokybot.persistence.entities.NPCEntity;

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
    
    List<Mastery> getMastryList(); // Using DTOs directly is fine if they are immutable enough or shared
    List<Skill> getSkills();
    
    List<Integer> getCompletedQuests();
    List<Object> getActiveQuests();
    
    int getServerTime();
    NPCEntity getEntity();
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
}
