package org.sokybot.common.astar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootContextLoader;



public class HeadlessSpringbootContextLoader extends SpringBootContextLoader{

	
	
	
	@Override
	protected SpringApplication getSpringApplication() {
		SpringApplication app =  super.getSpringApplication();
		
		app.setHeadless(false);
		
		return app ; 
	}
}
