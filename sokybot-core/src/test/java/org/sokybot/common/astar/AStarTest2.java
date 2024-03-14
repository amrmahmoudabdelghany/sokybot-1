package org.sokybot.common.astar;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sokybot.machinegroup.MachineGroupConfig;
import org.sokybot.machinegroup.gamemodel.geo.Vector2D;
import org.sokybot.machinegroup.gamemodel.navmesh.Position;
import org.sokybot.machinegroup.mapnavigation.RuteFinder;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = "gamePath=E:/Amroo/Silkroad Games/LegionSRO_15_08_2019" )
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(classes = {MachineGroupConfig.class , TestConfig.class})
@TestPropertySource(locations = {"/application.properties"})
@ActiveProfiles(value = {"test" , "dev"} )
//@Profile("test")
public class AstarTest2 {


	
	@Autowired
	private ISroMaterialDAO dao ; 
	
	
	
	@Test
	public void test() { 
	
		dao.findObjectNavMesh(30).ifPresentOrElse((o)->{
			o.inLines().forEach((line)->{
				System.out.println(line) ; 
			});
		} , ()->System.out.println("id not extists"));
	}
	
	 
	@Test
	public void testAstar() { 
		RuteFinder s = new RuteFinder(dao) ; 
	 List<Vector2D> l = 	
			 s.findPath(new Position(3534,0f , 2057.333f), new Position(3530f,0f , 1988.667f), 1) ;
		l.forEach((v)->System.out.println(v));
	}
	
	
	
}
