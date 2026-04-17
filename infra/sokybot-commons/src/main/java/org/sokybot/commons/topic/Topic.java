package org.sokybot.commons.topic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sokybot.commons.osgi.OsgiEventTopics;

public final class Topic {
    private final String canonical;
    private final List<String> segments;
    private final boolean pattern;

    private Topic(List<String> segments) {
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
        this.canonical = String.join(".", this.segments);
        this.pattern = this.segments.stream().anyMatch(s -> "*".equals(s) || "**".equals(s));
    }

    public static Topic of(String... inputSegments) {
        List<String> normalized = new ArrayList<>();
        if (inputSegments != null) {
            for (String segment : inputSegments) {
                if (segment == null || segment.trim().isEmpty()) {
                    continue;
                }
                String trimmed = segment.trim();
                if ("*".equals(trimmed) || "**".equals(trimmed)) {
                    normalized.add(trimmed);
                } else {
                    normalized.add(OsgiEventTopics.segment(trimmed));
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.add("**");
        }
        return new Topic(normalized);
    }

    public static Topic parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return of("**");
        }
        String normalized = input.trim().replace('/', '.');
        String[] parts = normalized.split("\\.");
        return of(parts);
    }

    public static Topic game(String machineId, String eventSimpleName) {
        return of("sokybot", "game", machineId, eventSimpleName);
    }

    public static Topic network(String machineId, String transition) {
        return of("sokybot", "network", machineId, transition);
    }

    public static Topic party(String groupName, String eventType) {
        return of("sokybot", "party", groupName, eventType);
    }

    public String toBridgeString() {
        return canonical;
    }

    public String toEventAdminString() {
        return String.join("/", segments);
    }

    public List<String> getSegments() {
        return segments;
    }

    public boolean isPattern() {
        return pattern;
    }

    public boolean matches(Topic other) {
        return TopicMatcher.DEFAULT.matches(this, other);
    }

    @Override
    public String toString() {
        return canonical;
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonical);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Topic)) {
            return false;
        }
        Topic other = (Topic) obj;
        return Objects.equals(canonical, other.canonical);
    }
}
