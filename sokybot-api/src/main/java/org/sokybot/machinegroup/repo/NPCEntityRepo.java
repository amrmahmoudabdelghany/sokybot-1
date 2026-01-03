package org.sokybot.machinegroup.repo;

import java.util.List;

import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;


public interface NPCEntityRepo extends CrudRepository<NPCEntity, Integer> {

	@Query("SELECT e FROM NPCEntity e WHERE e.longId Like '%MOB_%' AND e.level LIKE %:filter% OR e.name LIKE %:filter% ")
    List<NPCEntity> findAllMonsterLike(@Param("filter")  String filter) ; 
	
}
