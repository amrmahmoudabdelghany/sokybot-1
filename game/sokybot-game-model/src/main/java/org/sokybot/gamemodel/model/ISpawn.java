package org.sokybot.gamemodel.model;

import org.sokybot.persistence.entities.navmesh.Position;

public interface ISpawn {

    int getUniqueId();
    int getRefId();
    String getName();
    
    int getXSector();
    int getYSector();
    
    float getXOffset();
    float getYOffset();
    float getZOffset();
    
    short getAngle();
    
    Position getPosition();
    
    // Derived/Calculated
    int getX();
    int getY();
    
    double distance(int x, int y);
    double distance(float x, float y);
}
