package org.sokybot.engine.api.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Metadata descriptor for an actuator.
 * Provides information for UI display and dependency management.
 */
public class ActuatorDescriptor {

    private final String name;
    private final String displayName;
    private final String description;
    private final String version;
    private final String author;
    private final List<String> dependencies;
    private final List<String> tags;

    private ActuatorDescriptor(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name must not be null");
        this.displayName = builder.displayName != null ? builder.displayName : builder.name;
        this.description = builder.description != null ? builder.description : "";
        this.version = builder.version != null ? builder.version : "1.0.0";
        this.author = builder.author != null ? builder.author : "Unknown";
        this.dependencies = builder.dependencies != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.dependencies))
                : Collections.emptyList();
        this.tags = builder.tags != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.tags))
                : Collections.emptyList();
    }

    /**
     * Create a simple descriptor with just a name.
     */
    public static ActuatorDescriptor of(String name) {
        return builder(name).build();
    }

    /**
     * Create a descriptor with name and description.
     */
    public static ActuatorDescriptor of(String name, String description) {
        return builder(name).description(description).build();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    // Getters

    /**
     * Get the unique identifier for this actuator.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the human-readable display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of what this actuator does.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the version string (semantic versioning recommended).
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the author/creator name.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Get list of actuator names this actuator depends on.
     */
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * Get tags for categorization.
     */
    public List<String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return String.format("ActuatorDescriptor{name='%s', version='%s', author='%s'}",
                name, version, author);
    }

    /**
     * Builder for ActuatorDescriptor.
     */
    public static class Builder {
        private final String name;
        private String displayName;
        private String description;
        private String version;
        private String author;
        private List<String> dependencies;
        private List<String> tags;

        public Builder(String name) {
            this.name = name;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder addDependency(String dependency) {
            if (this.dependencies == null) {
                this.dependencies = new ArrayList<>();
            }
            this.dependencies.add(dependency);
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder addTag(String tag) {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }
            this.tags.add(tag);
            return this;
        }

        public ActuatorDescriptor build() {
            return new ActuatorDescriptor(this);
        }
    }
}
