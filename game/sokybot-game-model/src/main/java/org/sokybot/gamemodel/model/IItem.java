package org.sokybot.gamemodel.model;

public interface IItem extends ISpawn {

    byte getSlot();
    int getRentType();
    short getStackCount();
    byte getAttributeAssimilationProbability();
    
    // Delegated from ItemEntity usually, adding common ones
    String getLongId();
    boolean isWeapon();
    boolean isShield();
    boolean isAccessory();
    byte getLevel();
    
    // boolean isMall(); // etc. if needed
}
