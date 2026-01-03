package org.sokybot.app;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;

@Configuration
@Lazy
public class SokyBotIcons {

	private static  ResourceLoader resourceLoader = new DefaultResourceLoader();

	public static final  Icon DELETE_ICON = getIcon("icons/delete.svg" , 45 , 45) ; 
	
	
	public  static Icon getIcon(String name  , int w , int h ) {
	     
		
			FlatSVGIcon icon = new FlatSVGIcon(
					name , w , h , SokyBotIcons.class.getClassLoader());
			
			icon = icon.derive(0.40f) ; 
			ColorFilter filter = ColorFilter.getInstance();
			filter.add(Color.black, Color.DARK_GRAY, Color.LIGHT_GRAY) ;
			
			icon.setColorFilter(filter);
			return icon ; 
		
	}
	
	
	@Bean
	Icon feed() throws IOException {
		FlatSVGIcon icon = new FlatSVGIcon(
				resourceLoader.getResource("classpath:icons/feed.svg").getFile());
		icon = icon.derive(0.40f) ; 
		ColorFilter filter = ColorFilter.getInstance();
		filter.add(Color.black, Color.DARK_GRAY, Color.LIGHT_GRAY) ;
		
		icon.setColorFilter(filter);
		return icon;
	}
	

}
