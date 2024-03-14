package org.sokybot.machinegroup.service;

import java.awt.Image;
import java.util.List;
import java.util.Optional;

import org.sokybot.machinegroup.gamemodel.geo.Vector2D;
import org.sokybot.machinegroup.gamemodel.navmesh.Position;
import org.sokybot.machinegroup.gamemodel.npc.NPCEntity;
import org.sokybot.service.ISroDAO;

public interface ISroMaterialDAO extends ISroDAO , IMediaPk2  , IDataPk2{

	public Optional<Image> findSectorMinimap(short x, short y);
	Optional<Image> findCharacterIcon(int charId);
	
	List<Vector2D> findPath(Position start , Position end) ; 
	
	List<Vector2D> findPath(int x1 , int y1 , int x2 , int y2) ; 
	
	List<NPCEntity> findAllMonsterLike(String filter) ; 
	
}
