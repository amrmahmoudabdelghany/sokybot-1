package org.sokybot.warroom.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Party roster result: each leader maps to the machine ids in that party (including the leader).
 */
public final class WarRoomPlan {

    private final Map<String, List<String>> partyAssignments;

    public WarRoomPlan(Map<String, List<String>> partyAssignments) {
        Objects.requireNonNull(partyAssignments, "partyAssignments");
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : partyAssignments.entrySet()) {
            String leaderId = Objects.requireNonNull(e.getKey(), "leaderId");
            List<String> members = e.getValue();
            if (members == null) {
                copy.put(leaderId, Collections.emptyList());
            } else {
                copy.put(leaderId, Collections.unmodifiableList(new ArrayList<>(members)));
            }
        }
        this.partyAssignments = Collections.unmodifiableMap(copy);
    }

    public Map<String, List<String>> getPartyAssignments() {
        return partyAssignments;
    }
}
