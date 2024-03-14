package org.sokybot;

import com.formdev.flatlaf.FlatDarkLaf;

import lombok.extern.slf4j.Slf4j;

import org.sokybot.app.AppConfig;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.swing.*;

import java.awt.*;


@Slf4j
@ComponentScan({ "org.sokybot.app" })
@Configuration
public class Driver {

    private static void initUIManager() {

   	   log.info("initialize swing enverionment");
   	   
     	FlatDarkLaf.setup();
    	System.out.println("LAF Name : " + UIManager.getLookAndFeel().getName()) ; 
        UIManager.put("toolWindowManager.mainContainer.background" , Color.DARK_GRAY) ;
       
    }

    
    public static void main(String args[]) {
    	
    	
    	initUIManager();
  
   	new SpringApplicationBuilder(Driver.class)
    	   .headless(false).run(args);
     
    
    	
    	//SpringApplication.run(Driver.class, args ) ; 
    }


}
