package org.sokybot.machine.event.trainerevent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class TrainerReachDestinationEvent {
    private float x;
    private float y;
    
    public float getX() { return x; }
    public float getY() { return y; }
}
