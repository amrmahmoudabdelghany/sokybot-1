package org.sokybot.app.mainframe;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.swing.*;

@Configuration
public class MainFrameComponentFactory {
    
	
	@PostConstruct
	private void init() { 
		System.out.println("On Main Frame Component Factroy init"); 
	}
    
    @Bean
    public JMenuBar menuBar() { 
        JSMenuBar menuBar = new JSMenuBar() ;

        return menuBar ;
     }

     @Bean
     public JMenu helpMenu() {
        JMenu menu = new JMenu("Help") ;
        return menu ;
     }

     @Bean
     public JToolBar toolBar() {
        JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
        return toolBar ;
     }


}
