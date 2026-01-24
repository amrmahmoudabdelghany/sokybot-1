package org.sokybot.runtime.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;
import org.sokybot.game.navigation.IRuteFinder;
import org.sokybot.game.navigation.IRuteFinderFactory;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.sokybot.runtime.internal.domain.GroupInfo;
import org.sokybot.runtime.internal.domain.MachineInfo;
import org.sokybot.runtime.test.util.OSGiTestUtils;
import org.sokybot.runtime.test.util.OSGiTestUtils.MockBundleContext;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Base class for runtime bundle tests.
 * Provides common test setup (mock services, test data creation).
 */
@ExtendWith(MockitoExtension.class)
public abstract class RuntimeTestBase {
    
    protected static final String TEST_GROUP_NAME = "test-group";
    protected static final String TEST_MACHINE_NAME = "test-machine";
    protected static final String TEST_GAME_PATH = "/path/to/test/game";
    protected static final String TEST_FULL_NAME = TEST_GROUP_NAME + "." + TEST_MACHINE_NAME;
    
    protected MockBundleContext mockBundleContext;
    
    @Mock
    protected EventAdmin mockEventAdmin;
    
    @Mock
    protected IRuteFinderFactory mockRuteFinderFactory;
    
    @Mock
    protected IRuteFinder mockRuteFinder;
    
    @Mock
    protected IGamePersistenceFactory mockGamePersistenceFactory;
    
    @Mock
    protected IGameDataLookup mockGameDataLookup;
    
    @Mock
    protected Bundle mockBundle;
    
    /**
     * Sets up test fixtures before each test.
     * Subclasses can override to add custom setup.
     */
    @BeforeEach
    protected void setUp() {
        // Create mock bundle context
        mockBundleContext = OSGiTestUtils.createMockBundleContext();
        
        // Setup mock bundle
        lenient().when(mockBundle.getState()).thenReturn(Bundle.ACTIVE);
        mockBundleContext.setBundle(mockBundle);
        
        // Register mock services in bundle context
        mockBundleContext.registerMockService(EventAdmin.class, mockEventAdmin, null);
        mockBundleContext.registerMockService(IRuteFinderFactory.class, mockRuteFinderFactory, null);
        mockBundleContext.registerMockService(IGamePersistenceFactory.class, mockGamePersistenceFactory, null);
        
        // Setup mock factory returns
        lenient().when(mockRuteFinderFactory.createRuteFinder(mockGameDataLookup)).thenReturn(mockRuteFinder);
        lenient().when(mockGamePersistenceFactory.getLookup(anyString())).thenReturn(mockGameDataLookup);
    }
    
    /**
     * Creates a test GroupInfo instance.
     * 
     * @param name The group name (defaults to TEST_GROUP_NAME if null)
     * @param gamePath The game path (defaults to TEST_GAME_PATH if null)
     * @return A new GroupInfo instance
     */
    protected GroupInfo createTestGroupInfo(String name, String gamePath) {
        GroupInfo groupInfo = new GroupInfo(
            name != null ? name : TEST_GROUP_NAME,
            gamePath != null ? gamePath : TEST_GAME_PATH
        );
        groupInfo.setId(1); // Set ID for repository operations
        return groupInfo;
    }
    
    /**
     * Creates a test GroupInfo instance with default values.
     * 
     * @return A new GroupInfo instance
     */
    protected GroupInfo createTestGroupInfo() {
        return createTestGroupInfo(null, null);
    }
    
    /**
     * Creates a test MachineInfo instance.
     * 
     * @param groupId The group ID (defaults to 1 if 0)
     * @param machineName The machine name (defaults to TEST_MACHINE_NAME if null)
     * @return A new MachineInfo instance
     */
    protected MachineInfo createTestMachineInfo(int groupId, String machineName) {
        MachineInfo machineInfo = new MachineInfo(
            groupId > 0 ? groupId : 1,
            machineName != null ? machineName : TEST_MACHINE_NAME
        );
        machineInfo.setId(1); // Set ID for repository operations
        return machineInfo;
    }
    
    /**
     * Creates a test MachineInfo instance with default values.
     * 
     * @return A new MachineInfo instance
     */
    protected MachineInfo createTestMachineInfo() {
        return createTestMachineInfo(1, null);
    }
    
    /**
     * Tears down test fixtures after each test.
     * Subclasses can override to add custom cleanup.
     */
    protected void tearDown() {
        if (mockBundleContext != null) {
            mockBundleContext.clearServices();
        }
    }
}
