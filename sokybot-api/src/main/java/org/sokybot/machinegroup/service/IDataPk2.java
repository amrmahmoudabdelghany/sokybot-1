package org.sokybot.machinegroup.service;

import java.util.List;
import java.util.Optional;

import org.sokybot.machinegroup.gamemodel.navmesh.ObjectNavMesh;
import org.sokybot.machinegroup.gamemodel.navmesh.SectorRef;
import org.sokybot.machinegroup.mapnavigation.Sector;

public interface IDataPk2 {

	Optional<SectorRef> findSegment(byte sectorX, byte sectorY);

	Optional<SectorRef> findSegment(short sectorYX);

	Optional<ObjectNavMesh> findObjectNavMesh(int objectId);

	List<SectorRef>       findAllSectors() ; 
}
