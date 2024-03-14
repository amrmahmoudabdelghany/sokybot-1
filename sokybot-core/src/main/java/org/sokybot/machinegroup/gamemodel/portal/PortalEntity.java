package org.sokybot.machinegroup.gamemodel.portal;

import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.sokybot.machinegroup.gamemodel.SilkroadEntity;

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
public class PortalEntity extends SilkroadEntity {

	
	@OneToOne
	@PrimaryKeyJoinColumn(name = "portalId")
	private TeleportEntity teleport ;

	
	
	public Optional<TeleportEntity> getTeleport() { 
		return Optional.ofNullable(teleport) ; 
	}
}
