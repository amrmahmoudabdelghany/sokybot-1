package org.sokybot.runtime.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;
import org.sokybot.game.navigation.IRouteFinderFactory;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.internal.domain.GroupInfo;
import org.sokybot.runtime.test.RuntimeTestBase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupContextFactoryImpl.
 */
class GroupContextFactoryImplTest extends RuntimeTestBase {
    
    private GroupContextFactoryImpl factory;
    
    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        
        factory = new GroupContextFactoryImpl();
        // Note: In real OSGi, these would be injected via @Reference
        // For testing, we need to set them manually or use reflection
    }
    
    @Test
    void testCreateGroupContext() {
        GroupInfo groupInfo = createTestGroupInfo();
        
        // This test verifies that factory creates GroupContextImpl correctly
        // The actual creation requires proper OSGi service injection
        // which is handled by Declarative Services in real scenarios
        
        IGroupContext groupContext = factory.createGroupContext(
            groupInfo,
            mockBundleContext
        );
        
        assertNotNull(groupContext);
        assertEquals(TEST_GROUP_NAME, groupContext.name());
    }
    
    @Test
    void testCreateGroupContextWithNullGroupInfo() {
        assertThrows(NullPointerException.class, () -> {
            factory.createGroupContext(null, mockBundleContext);
        });
    }
    
    @Test
    void testDestroyGroupContext() {
        GroupInfo groupInfo = createTestGroupInfo();
        IGroupContext groupContext = factory.createGroupContext(groupInfo, mockBundleContext);
        
        assertNotNull(groupContext);
        
        // Destroy should not throw
        factory.destroyGroupContext(groupContext);
    }
    
    @Test
    void testDestroyGroupContextWithNull() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> {
            factory.destroyGroupContext(null);
        });
    }
}
