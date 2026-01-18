package org.sokybot.game.navigation.internal;

import static org.sokybot.commons.SilkroadUtils.getSectorOffset;
import static org.sokybot.commons.SilkroadUtils.getSectorYX;

import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.game.navigation.internal.triangulation.CellSplit;
import org.sokybot.game.navigation.internal.triangulation.Triangulator;
import org.sokybot.persistence.service.IGameDataLookup;

public class NavMesh {

	private IGameDataLookup gameDataLookup;

	private Triangulator ops = new Triangulator();

	public NavMesh(IGameDataLookup gameDataLookup) {
		this.gameDataLookup = gameDataLookup;
	}

	public Sector getSector(byte sectorX, byte sectorY) {
		return getSector((short) (((sectorY & 0xFF) << 8) | (sectorX & 0xFF)));
	}

	public Sector getSector(short sectorYX) {
		SectorRef ref = this.gameDataLookup.findSector(sectorYX).orElseThrow();
		return new Sector(this, ref);
	}

	public boolean containsSector(byte sectorX, byte sectorY) {
		return containsSector((short) (((sectorY & 0xFF) << 8) | (sectorX & 0xFF)));
	}

	public boolean containsSector(short sectorYX) {
		return this.gameDataLookup.findSector(sectorYX).map((s) -> true).orElse(false);
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
		return this.gameDataLookup.findObjectNavMesh(objectId).orElse(null);
	}

	private SectorRef getSectorRef(short sectorYX) {

		return this.gameDataLookup.findSector(sectorYX).orElse(null);

	}

}

