package org.sokybot.app.repo;

import java.util.List;

import org.sokybot.app.domain.MachineInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

public interface MachineInfoRepo extends CrudRepository<MachineInfo, Integer> {

	
	List<MachineInfo> findMachineInfoByGroupId(int id) ; 
	
	
}
