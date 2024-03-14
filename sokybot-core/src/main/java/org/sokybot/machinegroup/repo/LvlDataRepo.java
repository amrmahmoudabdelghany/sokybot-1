package org.sokybot.machinegroup.repo;

import org.sokybot.machinegroup.gamemodel.LvlEXP;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface LvlDataRepo extends CrudRepository<LvlEXP, Integer> {

}
