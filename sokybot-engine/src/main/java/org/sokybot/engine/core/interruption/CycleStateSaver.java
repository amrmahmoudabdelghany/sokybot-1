package org.sokybot.engine.core.interruption;

import org.sokybot.engine.api.workflow.ICycleState;
import org.sokybot.engine.api.workflow.IWorkflowContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Saves and restores cycle state during interruption.
 * Manages state data persistence.
 */
public class CycleStateSaver {
    
    /**
     * Saved cycle state information.
     */
    public static class SavedState {
        private final String cycleName;
        private final String stateName;
        private final ICycleState state;
        private final Map<String, Object> stateDataSnapshot;
        private final Map<String, Object> persistentDataSnapshot;
        private final long saveTimestamp;
        
        public SavedState(String cycleName, String stateName, ICycleState state,
                         Map<String, Object> stateDataSnapshot,
                         Map<String, Object> persistentDataSnapshot) {
            this.cycleName = cycleName;
            this.stateName = stateName;
            this.state = state;
            this.stateDataSnapshot = new HashMap<>(stateDataSnapshot);
            this.persistentDataSnapshot = new HashMap<>(persistentDataSnapshot);
            this.saveTimestamp = System.currentTimeMillis();
        }
        
        public String getCycleName() {
            return cycleName;
        }
        
        public String getStateName() {
            return stateName;
        }
        
        public ICycleState getState() {
            return state;
        }
        
        public Map<String, Object> getStateDataSnapshot() {
            return new HashMap<>(stateDataSnapshot);
        }
        
        public Map<String, Object> getPersistentDataSnapshot() {
            return new HashMap<>(persistentDataSnapshot);
        }
        
        public long getSaveTimestamp() {
            return saveTimestamp;
        }
    }
    
    /**
     * Saves the current cycle state.
     * 
     * @param cycleName The cycle name
     * @param currentState The current state
     * @param context The workflow context
     * @return Saved state information
     */
    public SavedState saveState(String cycleName, ICycleState currentState, IWorkflowContext context) {
        if (currentState == null) {
            return null;
        }
        
        // Snapshot state data
        Map<String, Object> stateDataSnapshot = context.getStateData() != null 
                ? new HashMap<>(context.getStateData()) 
                : new HashMap<>();
        
        // Snapshot persistent data
        Map<String, Object> persistentDataSnapshot = context.getPersistentData() != null 
                ? new HashMap<>(context.getPersistentData()) 
                : new HashMap<>();
        
        return new SavedState(cycleName, currentState.getName(), currentState,
                             stateDataSnapshot, persistentDataSnapshot);
    }
    
    /**
     * Restores cycle state from saved state.
     * 
     * @param savedState The saved state
     * @param context The workflow context
     */
    public void restoreState(SavedState savedState, IWorkflowContext context) {
        if (savedState == null) {
            return;
        }
        
        // Restore state data
        Map<String, Object> stateData = context.getStateData();
        stateData.clear();
        stateData.putAll(savedState.getStateDataSnapshot());
        
        // Restore persistent data (merge, don't clear)
        Map<String, Object> persistentData = context.getPersistentData();
        persistentData.putAll(savedState.getPersistentDataSnapshot());
    }
    
    /**
     * Clears saved state (for cleanup).
     * 
     * @param savedState The saved state to clear
     */
    public void clearSavedState(SavedState savedState) {
        if (savedState != null) {
            savedState.getStateDataSnapshot().clear();
            savedState.getPersistentDataSnapshot().clear();
        }
    }
}
