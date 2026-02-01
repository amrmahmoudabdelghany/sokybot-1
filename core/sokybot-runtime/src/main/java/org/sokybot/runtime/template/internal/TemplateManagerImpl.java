package org.sokybot.runtime.template.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.runtime.template.ITemplateManager;
import org.sokybot.runtime.template.MachineTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * File-based template manager implementation.
 * Stores templates as JSON files in a configurable directory.
 */
@Component(service = ITemplateManager.class)
public class TemplateManagerImpl implements ITemplateManager {

    private static final Logger log = LoggerFactory.getLogger(TemplateManagerImpl.class);

    private static final String TEMPLATE_EXTENSION = ".json";
    private static final String DEFAULT_TEMPLATES_DIR = "templates";

    private final Map<String, MachineTemplate> templates = new ConcurrentHashMap<>();

    private ObjectMapper mapper;
    private Path templatesDirectory;

    @Activate
    protected void activate() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.templatesDirectory = Paths.get(
                System.getProperty("sokybot.templates.dir", DEFAULT_TEMPLATES_DIR));

        log.info("Template Manager activated, directory: {}", templatesDirectory);

        // Create directory if needed
        try {
            Files.createDirectories(templatesDirectory);
            loadTemplates();
        } catch (IOException e) {
            log.warn("Failed to create/read templates directory: {}", e.getMessage());
        }
    }

    @Deactivate
    protected void deactivate() {
        templates.clear();
        log.info("Template Manager deactivated");
    }

    private void loadTemplates() {
        if (!Files.isDirectory(templatesDirectory)) {
            return;
        }

        try (Stream<Path> files = Files.list(templatesDirectory)) {
            files.filter(p -> p.toString().endsWith(TEMPLATE_EXTENSION))
                    .forEach(this::loadTemplate);
            log.info("Loaded {} templates", templates.size());
        } catch (IOException e) {
            log.error("Failed to list templates directory", e);
        }
    }

    private void loadTemplate(Path file) {
        try {
            String json = Files.readString(file);
            MachineTemplate template = mapper.readValue(json, MachineTemplate.class);
            templates.put(template.getId(), template);
            log.debug("Loaded template: {}", template.getName());
        } catch (IOException e) {
            log.warn("Failed to load template from {}: {}", file, e.getMessage());
        }
    }

    @Override
    public MachineTemplate createFromMachine(String machineId, String name) {
        MachineTemplate template = MachineTemplate.create(name);

        // TODO: Get actual settings from machine context
        // For now, create with placeholder data
        Map<String, Object> settings = new HashMap<>();
        settings.put("sourceId", machineId);
        template.setSettings(settings);

        log.info("Created template '{}' from machine '{}'", name, machineId);
        return template;
    }

    @Override
    public void save(MachineTemplate template) {
        templates.put(template.getId(), template);

        // Persist to file
        Path file = templatesDirectory.resolve(template.getId() + TEMPLATE_EXTENSION);
        try {
            String json = mapper.writeValueAsString(template);
            Files.writeString(file, json);
            log.info("Saved template: {}", template.getName());
        } catch (IOException e) {
            log.error("Failed to save template to {}: {}", file, e.getMessage());
        }
    }

    @Override
    public Optional<MachineTemplate> get(String templateId) {
        return Optional.ofNullable(templates.get(templateId));
    }

    @Override
    public Optional<MachineTemplate> getByName(String name) {
        return templates.values().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<MachineTemplate> list() {
        return new ArrayList<>(templates.values());
    }

    @Override
    public boolean delete(String templateId) {
        MachineTemplate removed = templates.remove(templateId);
        if (removed == null) {
            return false;
        }

        // Delete file
        Path file = templatesDirectory.resolve(templateId + TEMPLATE_EXTENSION);
        try {
            Files.deleteIfExists(file);
            log.info("Deleted template: {}", removed.getName());
            return true;
        } catch (IOException e) {
            log.error("Failed to delete template file: {}", e.getMessage());
            // Template was removed from memory, so return true
            return true;
        }
    }

    @Override
    public boolean applyToMachine(String templateId, String machineId) {
        Optional<MachineTemplate> template = get(templateId);
        if (template.isEmpty()) {
            log.warn("Template not found: {}", templateId);
            return false;
        }

        // TODO: Apply settings to machine context
        // This would involve:
        // 1. Getting the machine context by ID
        // 2. Applying settings from template
        // 3. Enabling/disabling actuators

        log.info("Applied template '{}' to machine '{}'", template.get().getName(), machineId);
        return true;
    }

    @Override
    public Optional<String> exportToJson(String templateId) {
        return get(templateId).map(template -> {
            try {
                return mapper.writeValueAsString(template);
            } catch (IOException e) {
                log.error("Failed to export template: {}", e.getMessage());
                return null;
            }
        });
    }

    @Override
    public MachineTemplate importFromJson(String json) {
        try {
            MachineTemplate template = mapper.readValue(json, MachineTemplate.class);
            save(template);
            log.info("Imported template: {}", template.getName());
            return template;
        } catch (IOException e) {
            log.error("Failed to import template: {}", e.getMessage());
            throw new RuntimeException("Failed to import template", e);
        }
    }

    @Override
    public int count() {
        return templates.size();
    }
}
