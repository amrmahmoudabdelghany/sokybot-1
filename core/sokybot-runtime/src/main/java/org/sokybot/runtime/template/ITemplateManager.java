package org.sokybot.runtime.template;

import java.util.List;
import java.util.Optional;

/**
 * Manager for machine configuration templates.
 */
public interface ITemplateManager {

    /**
     * Create a new template from the current machine configuration.
     * 
     * @param machineId the machine to capture configuration from
     * @param name      the template name
     * @return the created template
     */
    MachineTemplate createFromMachine(String machineId, String name);

    /**
     * Save a template.
     * 
     * @param template the template to save
     */
    void save(MachineTemplate template);

    /**
     * Get a template by ID.
     * 
     * @param templateId the template ID
     * @return the template, or empty if not found
     */
    Optional<MachineTemplate> get(String templateId);

    /**
     * Get a template by name.
     * 
     * @param name the template name
     * @return the template, or empty if not found
     */
    Optional<MachineTemplate> getByName(String name);

    /**
     * List all templates.
     * 
     * @return list of templates
     */
    List<MachineTemplate> list();

    /**
     * Delete a template.
     * 
     * @param templateId the template ID
     * @return true if deleted
     */
    boolean delete(String templateId);

    /**
     * Apply a template to a machine.
     * 
     * @param templateId the template ID
     * @param machineId  the target machine
     * @return true if applied successfully
     */
    boolean applyToMachine(String templateId, String machineId);

    /**
     * Export a template to JSON string.
     * 
     * @param templateId the template ID
     * @return the JSON string, or empty if not found
     */
    Optional<String> exportToJson(String templateId);

    /**
     * Import a template from JSON string.
     * 
     * @param json the JSON string
     * @return the imported template
     */
    MachineTemplate importFromJson(String json);

    /**
     * Get the number of templates.
     */
    int count();
}
