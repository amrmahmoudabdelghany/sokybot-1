package org.sokybot.common.astar;

import org.junit.jupiter.api.Test;
import org.sokybot.app.AppConfig;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;




@SpringBootTest(classes = {AppConfig.class} , webEnvironment = WebEnvironment.NONE  )
@ContextConfiguration(loader = HeadlessSpringbootContextLoader.class)
@TestPropertySource(locations = {"/application.properties"})
class RuteFinderTest {


	@Autowired
	ISroMaterialDAO sroDao ; 
	
	
	
	
	@Test
	public void test() { 
		sroDao.findDivisionInfo()
		.ifPresent((d)->System.out.println(d));
	}
	
	
}
