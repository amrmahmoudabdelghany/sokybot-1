package org.sokybot.persistence.service;

import org.sokybot.app.domain.MachineInfo;
import java.util.List;
import java.util.Optional;

/**
 * OSGi service interface for MachineInfo persistence operations.
 * Replaces Spring Data's CrudRepository pattern.
 */
public interface MachineInfoRepository {
    
    /**
     * Saves a MachineInfo entity (insert or update).
     */
    MachineInfo save(MachineInfo entity);
    
    /**
     * Finds a MachineInfo by its ID.
     */
    Optional<MachineInfo> findById(Integer id);
    
    /**
     * Returns all MachineInfo entities.
     */
    List<MachineInfo> findAll();
    
    /**
     * Deletes a MachineInfo by its ID.
     */
    void deleteById(Integer id);
    
    /**
     * Deletes a MachineInfo entity.
     */
    void delete(MachineInfo entity);
    
    /**
     * Checks if a MachineInfo exists by ID.
     */
    boolean existsById(Integer id);
    
    /**
     * Returns the total count of MachineInfo entities.
     */
    long count();
    
    // Custom query methods
    
    /**
     * Finds all machines belonging to a specific group.
     * Replaces Spring Data's findMachineInfoByGroupId method.
     */
    List<MachineInfo> findByGroupId(int groupId);
    
    /**
     * Finds a MachineInfo by unique machine name.
     */
    Optional<MachineInfo> findByMachineName(String machineName);
}
