package org.sokybot.machine.event;

import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
@AllArgsConstructor
public class SkillCastStartEvent {

	private int skillId  = -1; 
	private int casterId  = -1; 
	private int targetId  = -1; 
	
	

	
	public SkillCastStartEvent(ImmutablePacket packet) {
		IStreamReader reader = packet.getStreamReader() ; 
		
		if(reader.getBoolean()) { 
		reader.getShort() ; // unknow
		
		
		this.skillId = reader.getInt();  
		
		this.casterId = reader.getInt() ;
		reader.getInt() ; // unknow 
		this.targetId = reader.getInt() ; 
		
		// parsing damage
		}
		
		
	}
	
    public int getCasterId() {
        return this.casterId;
    }
    
    public int getSkillId() {
        return this.skillId;
    }
    
    public int getTargetId() {
        return this.targetId;
    }
	
	
}
