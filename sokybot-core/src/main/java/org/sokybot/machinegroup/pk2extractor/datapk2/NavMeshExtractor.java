package org.sokybot.machinegroup.pk2extractor.datapk2;

import static org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils.toByteArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


import org.sokybot.machinegroup.gamemodel.navmesh.NavBorderRef;

import org.sokybot.machinegroup.gamemodel.navmesh.NavCellLinkRef;
import org.sokybot.machinegroup.gamemodel.navmesh.NavCellRef;
import org.sokybot.machinegroup.gamemodel.navmesh.NavObjectRef;
import org.sokybot.machinegroup.gamemodel.navmesh.Position;
import org.sokybot.machinegroup.gamemodel.navmesh.SectorRef;
import org.sokybot.machinegroup.pk2extractor.IExtractor;
import org.sokybot.machinegroup.repo.SegmentRepo;
import org.sokybot.pk2.IPk2Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

import lombok.extern.slf4j.Slf4j;




@Slf4j
@Order(2)
//@Component
@Scope("prototype")
@Qualifier("datapk2")
public class NavMeshExtractor implements IExtractor{

	@Autowired
	private SegmentRepo repo ; 
	
	
	
	@Override
	public void extract(IPk2Driver driver) {
		log.info("Extract NavMesh data");

		//
		driver.find("(^nv_([0-9a-fA-F]{2})([0-9a-fA-F]{2})\\.nvm$)") // 1 for test
				.forEach((jmx) -> {

					ByteBuffer buffer;
					buffer = ByteBuffer.wrap(toByteArray(jmx));

					buffer.order(ByteOrder.LITTLE_ENDIAN);

					byte[] header = new byte[12];
					buffer.get(header);
					String fileName = jmx.getName();

					String strSectorYX = jmx.getName().substring(3, fileName.lastIndexOf("."));
					short sectorYX = Short.parseShort(strSectorYX, 16);

					log.info("Extract Segment {}  data ", strSectorYX);
					// Short.pas
					List<NavObjectRef> navObjects = extractNavObjects(buffer);

					//for (int i = 0; i < navObjects.size(); i++) {
						//this.cacheStorage.store(strSectorYX + "-" + i, navObjects.get(i));
					//}

					List<NavCellRef> navCells = extractNavCells(buffer);

					//this.cacheStorage.store(strSectorYX + "-navCell-count", navCells.size());

					List<NavBorderRef> navBorders = extractNavBorders(buffer, navCells, sectorYX);

					//for (int i = 0; i < navBorders.size(); i++) {
					//	this.cacheStorage.store(strSectorYX + "-" + i, navBorders.get(i));
					//}

					List<NavCellLinkRef> navCellLinks = extractNavCellLinks(buffer, navCells);

					//for (int i = 0; i < navCellLinks.size(); i++) {
					//	this.cacheStorage.store(strSectorYX + "-" + i, navCellLinks.get(i));
					//}

					//for (int i = 0; i < navCells.size(); i++) {
					//	this.cacheStorage.store(strSectorYX + "-" + i, navCells.get(i));
					//}

					buffer.position(buffer.position() + 0x12000);

					SectorRef segment = SectorRef.builder()
							.sectorYX(sectorYX)
							 .navObjects(navObjects)
							 .navCells(navCells)
							 .navBorders(navBorders)
							 .navCellLinks(navCellLinks)
							.build();

					
					repo.save(segment) ; 
					//this.cacheStorage.store(strSectorYX, segment);

				});


		
	}
	
	private List<NavCellLinkRef> extractNavCellLinks(ByteBuffer buffer, List<NavCellRef> navCells) {

		log.info("Extract NavCellLinks ");
		List<NavCellLinkRef> res = new ArrayList<>();

		float x, y;

		Position min, max;

		byte lineFlag, lineSource, lineDestination;

		short cellSource, cellDestination;

		int cellLinkCount = buffer.getInt();

		for (short linkIndex = 0; linkIndex < cellLinkCount; linkIndex++) {

			x = buffer.getInt() / 10;
			y = 192 - buffer.getFloat() / 10;

			min = new Position(x, 0, y);

			x = buffer.getInt() / 10;
			y = 192 - buffer.getFloat() / 10;

			max = new Position(x, 0, y);

			lineFlag = buffer.get();
			lineSource = buffer.get();
			lineDestination = buffer.get();

			cellSource = buffer.getShort();
			cellDestination = buffer.getShort();

			NavCellLinkRef cellLink = NavCellLinkRef.builder()
					.min(min)
					.max(max)
					.lineFlag(lineFlag)
					.lineSource(lineSource)
					.lineDestination(lineDestination)
					.cellSourceIndex(cellSource)
					.cellDestinationIndex(cellDestination)
					.build();

			res.add(cellLink);

			if (cellSource != 0xFFFFFFFF) {
				navCells.get(cellSource).getNavCellLinkIndex().add(linkIndex);
			}

			if (cellDestination != 0xFFFFFFFF) {
				navCells.get(cellDestination).getNavCellLinkIndex().add(linkIndex);
			}
		}

		return res;
	}
	

	private List<NavBorderRef> extractNavBorders(ByteBuffer buffer, List<NavCellRef> cells, short sectorYX) {

		log.info("Extract NavBorder for sector " + Integer.toHexString(sectorYX & 0xffff));
		List<NavBorderRef> res = new ArrayList<>();

		float x, y;
		Position min, max;
		byte lineFlag;
		byte lineSource;
		byte lineDestination;
		short cellSource;
		short cellDestination;
		short regionSource;
		short regionDestination;

		int regionLinkCount = buffer.getInt();
		for (short linkIndex = 0; linkIndex < regionLinkCount; linkIndex++) {

			x = buffer.getFloat() / 10;
			y = 192 - buffer.getFloat() / 10;
			min = new Position(x, 0, y);

			x = buffer.getFloat() / 10;
			y = 192 - buffer.getFloat() / 10;
			max = new Position(x, 0, y);

			lineFlag = buffer.get();
			lineSource = buffer.get();
			lineDestination = buffer.get();

			cellSource = buffer.getShort();
			cellDestination = buffer.getShort();
			regionSource = buffer.getShort();
			regionDestination = buffer.getShort();

			if (sectorYX == regionSource) {

				cells.get(cellSource).getNavBorderIndex().add(linkIndex);
			} else {
				cells.get(cellDestination).getNavBorderIndex().add(linkIndex);

			}

			NavBorderRef border = NavBorderRef.builder()
					.min(min)
					.max(max)
					.lineFlag(lineFlag)
					.lineSource(lineSource)
					.lineDestination(lineDestination)
					.cellSourceIndex(cellSource)
					.cellDestinationIndex(cellDestination)
					.regionSourceYX(regionSource)
					.regionDestionationYX(regionDestination)
					.build();

			res.add(border);

		}

		return res;

	}

	private List<NavCellRef> extractNavCells(ByteBuffer buffer) {
		log.info("Extract NavCells");
		List<NavCellRef> res = new ArrayList<>();

		float x, y;

		int cellCount = buffer.getInt();

		int cellExtraCount = buffer.getInt();

		Position min, max;

		for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {

			x = buffer.getFloat() / 10;
			y = 192 - buffer.getFloat() / 10;

			min = new Position(x, 0, y);

			x = buffer.getFloat() / 10;
			y = 192 - buffer.getFloat() / 10;

			max = new Position(x, 0, y);

			// System.out.println("Min :" + min + " Max : " + max) ;

			Vector<Short> navObjectIndex = new Vector<>();
			Vector<Short> navBorderIndex = new Vector<>();
			Vector<Short> navCellLinkIndex = new Vector<>();

			byte entryCount = buffer.get();

			for (int i = 0; i < entryCount; i++) {
				navObjectIndex.add(buffer.getShort());
			}

			res.add(new NavCellRef(min, max, navObjectIndex, navBorderIndex, navCellLinkIndex));

		}

		return res;
	}


	private List<NavObjectRef> extractNavObjects(ByteBuffer buffer) {

		log.info("Extract NavObjects");
		List<NavObjectRef> res = new ArrayList<>();

		short count = buffer.getShort();

		for (int i = 0; i < count; i++) {
			NavObjectRef.NavObjectRefBuilder builder = NavObjectRef.builder();

			int objectId = buffer.getInt();

			Position objectPosition = new Position(buffer.getFloat() / 10, buffer.getFloat() / 10,
					192 - buffer.getFloat() / 10);
			short collisionFlag = buffer.getShort();
			float yaw = buffer.getFloat();
			short uniqueId = buffer.getShort();
			short scale = buffer.getShort();
			short eventZoneFlag = buffer.getShort();
			short regionId = buffer.getShort();

			NavObjectRef obj = builder.id(objectId)
					.position(objectPosition)
					.collisionFlag(collisionFlag)
					.yaw(yaw)
					.uniqueId(uniqueId)
					.scale(scale)
					.eventZoneFlag(eventZoneFlag)
					.regionID(regionId)
					.build();

			res.add(obj);

			/*
			 * test if(!objLoaded.containsKey(uniqueId)) { objLoaded.put(uniqueId , obj) ;
			 * }else { System.out.println("Object " + uniqueId + " already loadded") ;
			 * System.out.println("New Object : " + obj) ;
			 * System.out.println("Old Object : " + objLoaded.get(uniqueId)) ; throw new
			 * IllegalStateException() ; }
			 * 
			 */

			// System.out.println(res) ;

			short mountCount = buffer.getShort();
			for (int ii = 0; ii < mountCount; ii++)
				buffer.position(buffer.position() + 6);

		}

		return res;

	}



}
