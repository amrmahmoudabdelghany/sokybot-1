package org.sokybot.machinegroup.mapnavigation;

import static org.sokybot.utils.SilkroadUtils.getSectorOffset;
import static org.sokybot.utils.SilkroadUtils.getSectorYX;

import java.util.HashMap;
import java.util.Map;

import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.machinegroup.mapnavigation.triangulation.CellSplit;
import org.sokybot.machinegroup.mapnavigation.triangulation.CellSplitRef;
import org.sokybot.machinegroup.mapnavigation.triangulation.Triangulator;
import org.sokybot.machinegroup.service.ISroMaterialDAO;

public class NavMesh {

	private ISroMaterialDAO sroDao;

	private Triangulator ops = new Triangulator();

	public NavMesh(ISroMaterialDAO sroDao) {
		this.sroDao = sroDao;
	}

	public Sector getSector(byte sectorX, byte sectorY) {
		return getSector((short) (((sectorY & 0xFF) << 8) | (sectorX & 0xFF)));
	}

	public Sector getSector(short sectorYX) {
		SectorRef ref = this.sroDao.findSegment(sectorYX).orElseThrow();
		return new Sector(this, ref);
	}

	public boolean containsSector(byte sectorX, byte sectorY) {
		return containsSector((short) (((sectorY & 0xFF) << 8) | (sectorX & 0xFF)));
	}

	public boolean containsSector(short sectorYX) {
		return this.sroDao.findSegment(sectorYX).map((s) -> true).orElse(false);
	}

	public Sector getSectorAt(float x, float y) {

		return new Sector(this, getSectorRef(getSectorYX(y, x)));
	}

	public Cell getNavCellAt(float x, float y) {
		return getSectorAt(x, y).getCellAt(getSectorOffset(x), getSectorOffset(y));

	}

	public CellSplit getCellSplit(Cell cell) {
		return this.ops.createSplite(cell);
	}

	public ObjectNavMesh getObjectNavMesh(int objectId) {
		return this.sroDao.findObjectNavMesh(objectId).orElse(null);
	}

	private SectorRef getSectorRef(short sectorYX) {

		return this.sroDao.findSegment(sectorYX).orElse(null);

	}

}
