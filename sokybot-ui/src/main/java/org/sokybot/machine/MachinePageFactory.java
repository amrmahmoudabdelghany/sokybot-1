package org.sokybot.machine;

import java.io.IOException;

import javax.swing.Icon;

import org.sokybot.common.ANSITextPane;
import org.sokybot.machine.page.environmentpage.EnvTab;
import org.sokybot.machine.page.environmentpage.MonsterTable;
import org.sokybot.machine.page.skillpage.SkillTab;
import org.sokybot.machine.page.trainingpage.AreaTab;
import org.sokybot.machine.page.trainingpage.monsterpreference.MonsterPreferenceTab;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;

@Configuration
public class MachinePageFactory {

	public static final String LOG_PAGE_NAME = "Logs";
	public static final String TRAINING_PAGE_NAME = "Training";

	public static final String ENVIRONMENT_PAGE_NAME = "Environment" ; 
	
	public static final int TRAINING_PAGE_ORDER = 0;
	public static final int ENVIRONMENT_PAGE_ORDER = 1 ; 
	public static final int LOG_PAGE_ORDER = 2;
	private static final int SKILL_PAGE_ORDER = 3;
	private static final String HUNTING_PAGE_NAME = "Hunting";

	@Autowired
	private ResourceLoader resourceLoader;

	
	@Bean
	@Order(LOG_PAGE_ORDER)
	public IMachinePage logPage() { 
		
		
		return new IMachinePage() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public String getName() {
				return LOG_PAGE_NAME ; 
			}
			
			@Override
			public Icon getIcon() {
				return findIconOrGetDefaule("classpath:icons/feed.svg") ; 
			}
		} ; 
	}

	@Bean
	@Order(TRAINING_PAGE_ORDER)
	public IMachinePage trainingPage(AreaTab areaTab ) { 
		
		IMachinePage trainingPage =  new IMachinePage() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getName() {
				return TRAINING_PAGE_NAME ; 
			}
			
			@Override
			public Icon getIcon() {
				return findIconOrGetDefaule("classpath:icons/feed.svg") ; 
			}
		}; 
		
		trainingPage.addTab("Area", areaTab) ; 	
		
		return trainingPage ;
	}
	
 
	@Bean
	@Order(ENVIRONMENT_PAGE_ORDER)
	public IMachinePage environmentPage(EnvTab envTab) { 
		
		IMachinePage envPage = new IMachinePage() {
			
			@Override
			public String getName() {
				return ENVIRONMENT_PAGE_NAME ; 
			}
			
			@Override
			public Icon getIcon() {
				
				return findIconOrGetDefaule("classpath:icons/feed.svg");
			}
		};
		
		envPage.addTab("Monsters", envTab);
		
		return envPage ; 
	}
	
	@Bean
	@Order(SKILL_PAGE_ORDER)
	public IMachinePage huntingPage(SkillTab skillTab, MonsterPreferenceTab monsterPreferenceTab) { 
		
		IMachinePage page = new IMachinePage() {
			
			@Override
			public String getName() {
				return HUNTING_PAGE_NAME ; 
			}
			
			@Override
			public Icon getIcon() {
				
				return findIconOrGetDefaule("classpath:icons/feed.svg");
			}
		};
		
		page.addTab("Skills", skillTab);
		page.addTab("Monster Targeting", monsterPreferenceTab) ;
		return page ; 
	}
	
	private  Icon findIconOrGetDefaule(String iconClasspath) {
		Resource r = this.resourceLoader.getResource(iconClasspath);
		try {
			return new FlatSVGIcon(r.getInputStream()).derive(30, 30);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new FlatSearchIcon();
	}
}
