package org.sokybot.machinegroup.repo;

import org.sokybot.machinegroup.gamemodel.setting.Settings;
import org.springframework.data.repository.CrudRepository;

public interface SettingRepo extends CrudRepository<Settings, String> {

	
	public Settings saveAndFlush(Settings s) ; 
	
}
