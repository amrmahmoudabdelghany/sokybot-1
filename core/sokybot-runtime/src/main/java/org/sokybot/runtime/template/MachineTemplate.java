package org.sokybot.runtime.template;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A machine configuration template.
 * Stores settings and actuator configuration for reuse.
 */
public class MachineTemplate {

    private final String id;
    private String name;
    private String description;
    private String author;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> settings;
    private List<String> enabledActuators;
    private List<String> disabledActuators;
    private Map<String, Object> metadata;

    public MachineTemplate(String id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.settings = new HashMap<>();
        this.enabledActuators = Collections.emptyList();
        this.disabledActuators = Collections.emptyList();
        this.metadata = new HashMap<>();
    }

    // Static factory methods

    public static MachineTemplate create(String name) {
        String id = generateId(name);
        return new MachineTemplate(id, name);
    }

    private static String generateId(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "")
                + "-" + System.currentTimeMillis() % 10000;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Map<String, Object> getSettings() {
        return Collections.unmodifiableMap(settings);
    }

    public List<String> getEnabledActuators() {
        return enabledActuators;
    }

    public List<String> getDisabledActuators() {
        return disabledActuators;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    // Setters

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void setAuthor(String author) {
        this.author = author;
        this.updatedAt = Instant.now();
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings != null ? new HashMap<>(settings) : new HashMap<>();
        this.updatedAt = Instant.now();
    }

    public void setEnabledActuators(List<String> enabledActuators) {
        this.enabledActuators = enabledActuators != null ? List.copyOf(enabledActuators) : Collections.emptyList();
        this.updatedAt = Instant.now();
    }

    public void setDisabledActuators(List<String> disabledActuators) {
        this.disabledActuators = disabledActuators != null ? List.copyOf(disabledActuators) : Collections.emptyList();
        this.updatedAt = Instant.now();
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.updatedAt = Instant.now();
    }

    // For deserialization
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("MachineTemplate{id='%s', name='%s', author='%s'}", id, name, author);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MachineTemplate that = (MachineTemplate) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
