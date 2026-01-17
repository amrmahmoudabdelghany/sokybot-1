package org.sokybot.machine.config;

import javax.transaction.Transactional;

import org.sokybot.app.AppConstants;
import org.sokybot.settings.Settings;
import org.sokybot.settings.TrainingAreaSettings;
import org.sokybot.settings.ISettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MachineSettingFactory {

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private org.sokybot.settings.ISettingsManager settingsManager;
	
	@Bean
	Settings settings() {
		String groupName = this.ctx.getParent().getEnvironment().getProperty(AppConstants.GROUP_NAME);
		String trainerName = this.ctx.getEnvironment().getProperty(AppConstants.MACHINE_NAME);
		
		return settingsManager.loadSettings(groupName, trainerName, "settings", Settings.class);
	}

	@Bean
	TrainingAreaSettings areaSettings() {
		return settings().getTrainingAreaSettings();
	}
}
