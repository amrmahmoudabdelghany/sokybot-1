package org.sokybot.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SokybotIOUtils {

	private SokybotIOUtils() {}
	
	public static Map<String, String> readProperties(InputStream in) throws IOException { 
		
		Properties prop = new Properties();
		prop.load(in);

		Map<String, String> _prop = new HashMap<>();
		prop.forEach((k, v) -> {
			_prop.put(String.valueOf(k), String.valueOf(v));
		});
		
		return _prop ; 

	}
}
