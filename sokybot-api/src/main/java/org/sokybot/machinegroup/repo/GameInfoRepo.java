package org.sokybot.machinegroup.repo;

import org.sokybot.machinegroup.gamemodel.GameInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameInfoRepo extends CrudRepository<GameInfo, String> {
 
	
	

}
