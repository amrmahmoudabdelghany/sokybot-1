package org.sokybot.machinegroup.repo;

import java.util.Optional;

import javax.transaction.Transactional;

import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface ItemEntityRepo extends JpaRepository<ItemEntity, Integer>{


	Optional<ItemEntity> findItemEntityByLongId(String longId) ; 
}
