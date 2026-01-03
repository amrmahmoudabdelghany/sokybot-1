package org.sokybot.machinegroup.gamemodel.portal;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;

import org.sokybot.machinegroup.gamemodel.SilkroadEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


@Entity
@Getter
@SuperBuilder
@ToString(callSuper = true)
@NoArgsConstructor
public class TeleportEntity extends SilkroadEntity{


	private int portalId ; 
	
	@Singular
	@ElementCollection
	private List<Integer> links ; 
	
	
}
