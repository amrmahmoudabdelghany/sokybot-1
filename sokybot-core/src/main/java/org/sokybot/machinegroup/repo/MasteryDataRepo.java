package org.sokybot.machinegroup.repo;

import org.sokybot.machinegroup.gamemodel.MasteryData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MasteryDataRepo extends CrudRepository<MasteryData, Integer> {

}
