package org.sokybot.machine;

import javax.transaction.Transactional;

import org.sokybot.app.AppConstants;
import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.sokybot.machinegroup.gamemodel.setting.TrainingAreaSettings;
import org.sokybot.machinegroup.repo.SettingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MachineSettingFactory {

	@Autowired
	private ApplicationContext ctx;

	@Bean
	Settings settings() {
		SettingRepo settingRepo = this.ctx.getBean(SettingRepo.class);
		String groupName = this.ctx.getParent().getEnvironment().getProperty(AppConstants.GROUP_NAME);
		String trainerName = this.ctx.getEnvironment().getProperty(AppConstants.MACHINE_NAME);
		String id = groupName + "." + trainerName;

		 return  settingRepo.findById(id).orElseGet(()->{
			 
			 System.out.println("Create New Settings Object" ) ; 
			 return settingRepo.saveAndFlush(new Settings(id, groupName, trainerName)) ; 
		 })  ; 
		 
		
	}

	@Bean
	TrainingAreaSettings areaSettings() {
		return settings().getTrainingAreaSettings();
	}
}
