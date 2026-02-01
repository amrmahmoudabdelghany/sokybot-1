package org.sokybot.engine.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Manifest for actuator plugins.
 * Loaded from actuator.yaml in JAR files.
 */
public class ActuatorManifest {

    private String name;
    private String displayName;
    private String description;
    private String version;
    private String author;
    private String mainClass;
    private List<String> dependencies;
    private List<String> tags;

    public ActuatorManifest() {
        this.dependencies = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    /**
     * Validate the manifest has required fields.
     */
    public boolean isValid() {
        return name != null && !name.isEmpty()
                && mainClass != null && !mainClass.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ActuatorManifest{name='%s', version='%s', mainClass='%s'}",
                name, version, mainClass);
    }
}
