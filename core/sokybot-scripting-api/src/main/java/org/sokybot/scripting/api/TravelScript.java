package org.sokybot.scripting.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Default immutable {@link ITravelScript}.
 */
public final class TravelScript implements ITravelScript {

    private final String id;
    private final String description;
    private final List<TravelCommand> commands;

    private TravelScript(String id, String description, List<TravelCommand> commands) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = description != null ? description : "";
        this.commands = Collections.unmodifiableList(new ArrayList<>(commands));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<TravelCommand> getCommands() {
        return commands;
    }

    public static TravelScript of(String id, String description, List<TravelCommand> commands) {
        return new TravelScript(id, description, commands);
    }
}
