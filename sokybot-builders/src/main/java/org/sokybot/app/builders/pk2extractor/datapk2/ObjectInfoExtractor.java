import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.datapk2;

import static org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils.toByteArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.machinegroup.gamemodel.navmesh.ObjectNavMesh;
import org.sokybot.machinegroup.gamemodel.navmesh.ObjectNavMesh.ObjectNavmeshBuilder;
import org.sokybot.app.builders.pk2extractor.IExtractor;
import org.sokybot.app.builders.pk2extractor.mediapk2.EntityNameExtractor;
import org.sokybot.persistence.service.ObjectNavMeshRepoRepository;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;






import lombok.extern.slf4j.Slf4j;




@Slf4j
@Order(1)
@Component(service = IExtractor.class)
@Qualifier("datapk2")
public class ObjectInfoExtractor implements IExtractor{

	
	@Reference
	private ObjectNavMeshRepository repo ; 
	
	@Override
	public void extract(IPk2Driver driver) {
		log.info("Extract Object Info ");

		driver.findFirst("\\navmesh\\object.ifo").ifPresent((jmx) -> {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(jmx.getInputStream()));
			reader.lines().filter((line) -> line.length() > 5 && !line.equals("JMXVOBJI1000")).forEach((line) -> {
				
				String id = String.valueOf(Integer.parseInt(line.substring(0, 5)));
				String bsrPath = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
				bsrPath = processPath(bsrPath) ; 
				if (bsrPath != null && !bsrPath.isBlank() && bsrPath.endsWith(".bsr")) {
					driver.findFirst(bsrPath)		
					.map(this::extractBMSPath)
							
							.flatMap(driver::findFirst)
							.ifPresent((bmsFile) -> {
								
								ObjectNavMesh objectNavMesh = extractObjectNavmesh(Integer.parseInt(id) , bmsFile );
								// cache this object
								if (objectNavMesh != null) {
									// System.out.println("ObjectNavMesh is : " + objectNavMesh) ;
									//this.cacheStorage.store(id, objectNavMesh);
									
									this.repo.save(objectNavMesh) ; 
								}

							});
				}
			});

		});
		
	}
	
	
	private  String processPath(String path) { 
		
		return "(?i)" +  String.join("\\(?i)", path.split("\\\\")) ; 
	}
	
	
	public static void main(String args[]) { 
		
		IPk2Driver driver = IPk2Driver.open("E:\\Amroo\\Silkroad Games\\LegionSRO_15_08_2019\\Data.pk2") ; 
		
		//System.out.println(processBsrPath("res\\etc\\x_mas\\x_mas_candle01.bsr")) ; 
		
	//	driver.findFirst("Res\\etc\\x_mas\\x_mas_wreath03.bsr")
	//	.ifPresent((jmx)->{
	//		System.out.println("File exists") ; 
	//	});
		
		ObjectInfoExtractor ex = new ObjectInfoExtractor() ; 
		ex.extract(driver);
		
	}
	
	private ObjectNavMesh extractObjectNavmesh(int id , JMXFile file) {

		log.info("Extract Object Navmesh");
		ByteBuffer buffer = ByteBuffer.wrap(toByteArray(file));
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		// BMS file header
		buffer.position(12); // "JMXVBMS 0110"
		buffer.getInt(); // vertexOffset
		buffer.getInt(); // skinOffset
		buffer.getInt(); // faceOffset
		buffer.getInt(); // clothVertexOffset
		buffer.getInt(); // clothEdgeOffset
		buffer.getInt(); // boundingBoxOffset
		buffer.getInt(); // occlusionPortals

		int navmeshOffset = buffer.getInt();
		buffer.getInt(); // SkinedNavMeshOffset
		buffer.getInt(); // Unknown9Offset
		buffer.getInt(); // unkUInt0
		int navFlag = buffer.getInt(); // 0 = None, 1 = Edge, 2 = Cell, 4 = Event

		if (navmeshOffset != 0) {

			ObjectNavmeshBuilder builder = new ObjectNavmeshBuilder();

			builder.id(id) ; 
			
			buffer.position(navmeshOffset);

			int counter = buffer.getInt(); // //NavVertices
			for (int i = 0; i < counter; i++) {
				double x = buffer.getFloat() / 10;
				double z = buffer.getFloat() / 10;
				double y = buffer.getFloat() / 10;
				// x = x * Math.cos(-rotation) - -y * Math.sin(-rotation);
				// y = x * Math.sin(-rotation) + -y * Math.cos(-rotation);

				// x += objectPosition.getX();
				// y += objectPosition.getY();

				builder.point((float) x, (float) z, (float) y);

				buffer.get(); // unk
			}

			counter = buffer.getInt(); // collisionCellCount
			for (int i = 0; i < counter; i++) {

				builder.objectGround(buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort());
				if ((navFlag & 2) != 0) {
					buffer.get(); // cell.EventZoneData

				}
			}

			counter = buffer.getInt(); // //NavOutlineEdges
			for (int i = 0; i < counter; i++) {

				builder.outLine(buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(),
						buffer.get());
				if ((navFlag & 1) != 0) {
					buffer.get();
					// throw new IllegalStateException("TEST") ;
				}
			}

			counter = buffer.getInt();

			for (int i = 0; i < counter; i++) {

				builder.inLine(buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(),
						buffer.get());
				if ((navFlag & 1) != 0) {
					buffer.get();
					// throw new IllegalStateException("TEST") ;
				}

			}
			return builder.build();
		} else {
			System.out.println("NavmeshOffset is " + navmeshOffset);
		}
		return null;

	}

	private String extractBMSPath(JMXFile bsrFile) {
		//log.info("Extract BMS Path");
		ByteBuffer buffer = ByteBuffer.wrap(toByteArray(bsrFile));
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(12); // file header "JMXVRES 109 "

		buffer.getInt(); // bmt
		buffer.getInt(); // bms1
		buffer.getInt(); // unk1
		buffer.getInt();// unk2
		buffer.getInt();// unk3
		buffer.getInt();// unk4
		buffer.getInt(); // unk5
		int bms2 = buffer.getInt();
		buffer.getInt(); // unk6

		buffer.position(bms2);

		byte[] b = new byte[buffer.getInt()];
		buffer.get(b);
		
		String res =  new String(b);
	//	System.out.println("BMS Path is : " + res) ; 
		return res.isBlank() ? "" : processPath(res) ; 
	}
}




