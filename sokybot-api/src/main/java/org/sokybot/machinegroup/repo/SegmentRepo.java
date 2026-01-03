package org.sokybot.machinegroup.repo;


import org.sokybot.machinegroup.gamemodel.navmesh.SectorRef;
import org.springframework.data.repository.CrudRepository;

public interface SegmentRepo extends CrudRepository<SectorRef, Short>{

}
