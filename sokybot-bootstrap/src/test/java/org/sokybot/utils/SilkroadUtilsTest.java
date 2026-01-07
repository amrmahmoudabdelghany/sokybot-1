package org.sokybot.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;

public class SilkroadUtilsTest {

	@Test
	public void testIsSilkroadDirectoryMethod() {
		

		File dir = Paths.get("src" ,"test" , "resources" , "FakeSilkroadDirectory").toFile(); 
	
		assertTrue(SilkroadUtils.isSilkraodDirectory(dir));
	}

	@Test
	public void testIsSilkroadDirctoryWithInvalidDir() { 
		File dir = Paths.get("src" ,"test" , "resources" , "InvalidFakeSilkroadDirectory").toFile(); 
		
		assertFalse(SilkroadUtils.isSilkraodDirectory(dir));

	}
}
