package org.sokybot.context;

/**
 * Event topics and property names for OSGi Event Admin context lifecycle events.
 * 
 * These events are published when machine group contexts and machine contexts
 * are created or destroyed at runtime.
 */
public class ContextLifecycleEvents {
    
    // Group Context Event Topics
    public static final String TOPIC_GROUP_CONTEXT_CREATED = "sokybot/context/group/CREATED";
    public static final String TOPIC_GROUP_CONTEXT_DESTROYED = "sokybot/context/group/DESTROYED";
    
    // Machine Context Event Topics
    public static final String TOPIC_MACHINE_CONTEXT_CREATED = "sokybot/context/machine/CREATED";
    public static final String TOPIC_MACHINE_CONTEXT_DESTROYED = "sokybot/context/machine/DESTROYED";
    
    // Event Properties
    /**
     * Group name property (String)
     */
    public static final String PROP_GROUP_NAME = "groupName";
    
    /**
     * Machine name property (String)
     */
    public static final String PROP_MACHINE_NAME = "machineName";
    
    /**
     * Full name property - format: "groupName.machineName" (String)
     */
    public static final String PROP_FULL_NAME = "fullName";
    
    /**
     * Context object property - IGroupContext or IMachineContext instance
     */
    public static final String PROP_CONTEXT = "context";
    
    /**
     * Timestamp when the event was created (Long - milliseconds since epoch)
     */
    public static final String PROP_TIMESTAMP = "timestamp";
    
    private ContextLifecycleEvents() {
        // Utility class - prevent instantiation
    }
}
