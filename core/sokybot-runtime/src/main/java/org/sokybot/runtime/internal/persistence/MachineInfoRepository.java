package org.sokybot.runtime.internal.persistence;

import org.sokybot.runtime.internal.domain.MachineInfo;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MachineInfo persistence operations.
 * Internal use only within sokybot-runtime.
 */
public interface MachineInfoRepository {

    /**
     * Saves a MachineInfo entity (insert or update).
     */
    MachineInfo save(MachineInfo entity);

    /**
     * Finds a MachineInfo by its ID.
     */
    Optional<MachineInfo> findById(int id);

    /**
     * Returns all MachineInfo entities.
     */
    List<MachineInfo> findAll();

    /**
     * Deletes a MachineInfo by its ID.
     */
    void deleteById(int id);

    /**
     * Deletes a MachineInfo entity.
     */
    void delete(MachineInfo entity);

    /**
     * Checks if a MachineInfo exists by ID.
     */
    boolean existsById(int id);

    /**
     * Returns the total count of MachineInfo entities.
     */
    long count();

    /**
     * Finds all machines belonging to a specific group.
     */
    List<MachineInfo> findByGroupId(int groupId);

    /**
     * Finds a MachineInfo by unique machine name.
     */
    Optional<MachineInfo> findByMachineName(String machineName);
}
