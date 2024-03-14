package org.sokybot.gameloader;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.InvalidPathException;

import org.junit.jupiter.api.Test;

public class GameLoaderTest {

	
	
	@Test
	public void testInvalidPath() { 
		GameLoader gameLoader = new GameLoader() ; 
		assertThrows(NullPointerException.class ,()->{
			gameLoader.launch(null) ;
		});
		
		assertThrows(InvalidPathException.class, ()->{
			
			gameLoader.launch("  ") ; 
		});
		
		assertThrows(IllegalArgumentException.class , ()->{
			gameLoader.launch("C://notexistsfile123.txt") ; 
		});
		
	}
	

}
