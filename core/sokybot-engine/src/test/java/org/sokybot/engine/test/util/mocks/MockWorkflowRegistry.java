package org.sokybot.engine.test.util.mocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IOrthogonalState;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.engine.api.workflow.IWorkflowState;

/**
 * Mock IWorkflowRegistry for offline testing.
 * Captures registered cycles and states for assertion.
 */
public class MockWorkflowRegistry implements IWorkflowRegistry {

    private final Map<String, ICycleDefinition> cycles = new LinkedHashMap<>();
    private final Map<String, IOrthogonalState> states = new LinkedHashMap<>();

    @Override
    public int registerOrthogonalState(IOrthogonalState state) {
        states.put(state.getName(), state);
        return state.getDesiredPriority();
    }

    @Override
    public int registerAfter(IOrthogonalState state, String afterState, int fallbackPriority) {
        states.put(state.getName(), state);
        return fallbackPriority;
    }

    @Override
    public boolean unregisterOrthogonalState(String stateName) {
        return states.remove(stateName) != null;
    }

    @Override
    public int registerCycle(ICycleDefinition cycle) {
        cycles.put(cycle.getCycleId().asString(), cycle);
        return cycle.getDesiredPriority();
    }

    @Override
    public boolean unregisterCycle(String cycleName) {
        return cycles.remove(cycleName) != null;
    }

    @Override
    public void setCycleEnabled(String cycleName, boolean enabled) {
    }

    @Override
    public boolean isCycleEnabled(String cycleName) {
        return cycles.containsKey(cycleName);
    }

    @Override
    public List<String> getRegisteredCycles() {
        return new ArrayList<>(cycles.keySet());
    }

    @Override
    public ICycleDefinition getCycle(String cycleName) {
        return cycles.get(cycleName);
    }

    @Override
    public List<String> getOrderedWorkflowComponents() {
        List<String> result = new ArrayList<>(states.keySet());
        result.addAll(cycles.keySet());
        return result;
    }

    @Override
    public List<String> getAllStateNames() {
        return new ArrayList<>(states.keySet());
    }

    @Override
    public IWorkflowState getState(String fullyQualifiedStateName) {
        return null;
    }

    @Override
    public boolean isCycleActive() {
        return false;
    }

    public Map<String, ICycleDefinition> getRegisteredCycleMap() {
        return cycles;
    }
}
