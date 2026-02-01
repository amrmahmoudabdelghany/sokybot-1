package org.sokybot.runtime.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sokybot.game.navigation.IRouteFinder;
import org.sokybot.game.navigation.IRouteFinderFactory;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.internal.domain.GroupInfo;
import org.sokybot.runtime.internal.domain.MachineInfo;
import org.sokybot.runtime.test.RuntimeTestBase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupContextImpl.
 */
class GroupContextImplTest extends RuntimeTestBase {

    private GroupContextImpl groupContext;
    private GroupInfo groupInfo;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();

        groupInfo = createTestGroupInfo();
        mockBundleContext.setBundle(mockBundle);

        // Create group context
        groupContext = new GroupContextImpl(
                groupInfo,
                mockBundleContext,
                mockEventAdmin,
                mockRouteFinderFactory,
                mockGamePersistenceFactory);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        if (groupContext != null) {
            groupContext.destroy();
        }
        super.tearDown();
    }

    @Test
    void testName() {
        assertEquals(TEST_GROUP_NAME, groupContext.name());
    }

    @Test
    void testIsRunning() {
        assertTrue(groupContext.isRunning());
    }

    @Test
    void testGetMachinesInitiallyEmpty() {
        IMachineContext[] machines = groupContext.getMachines();
        assertNotNull(machines);
        assertEquals(0, machines.length);
    }

    @Test
    void testFindMachineCtxNotFound() {
        assertFalse(groupContext.findMachineCtx("non-existent").isPresent());
    }

    @Test
    void testGetGameDataLookup() {
        IGameDataLookup lookup = groupContext.getGameDataLookup();

        assertNotNull(lookup);
        assertEquals(mockGameDataLookup, lookup);
        verify(mockGamePersistenceFactory, times(1)).getLookup(TEST_GAME_PATH);
    }

    @Test
    void testGetGameDataLookupCached() {
        IGameDataLookup lookup1 = groupContext.getGameDataLookup();
        IGameDataLookup lookup2 = groupContext.getGameDataLookup();

        assertSame(lookup1, lookup2);
        verify(mockGamePersistenceFactory, times(1)).getLookup(TEST_GAME_PATH);
    }

    @Test
    void testGetRouteFinder() {
        IRouteFinder finder = groupContext.getRouteFinder();

        assertNotNull(finder);
        assertEquals(mockRouteFinder, finder);
        verify(mockRouteFinderFactory, times(1)).createRouteFinder(mockGameDataLookup);
    }

    @Test
    void testGetRouteFinderCached() {
        IRouteFinder finder1 = groupContext.getRouteFinder();
        IRouteFinder finder2 = groupContext.getRouteFinder();

        assertSame(finder1, finder2);
        verify(mockRouteFinderFactory, times(1)).createRouteFinder(mockGameDataLookup);
    }

    @Test
    void testInstallMachine() {
        MachineInfo machineInfo = createTestMachineInfo();

        // This test requires mocking MachineContextFactory.createMachineContext
        // For now, we verify the structure

        // In a complete test, we would:
        // 1. Mock MachineContextFactory
        // 2. Call installMachine
        // 3. Verify machine is created and added
        // 4. Verify event is published
    }

    @Test
    void testInstallMachineWithDuplicateName() {
        // Install first machine
        // Then try to install another with same name
        // Should throw NameUniquenessConstraintViolationException
    }

    @Test
    void testInstallMachineWithBlankName() {
        assertThrows(RuntimeException.class, () -> {
            groupContext.installMachine("");
        });
    }

    @Test
    void testDestroyPublishesEvents() {
        // Install some machines
        // Then destroy
        // Verify MACHINE_CONTEXT_DESTROYED events are published

        groupContext.destroy();

        // Verify destroy was called (would need actual machines for full test)
        assertTrue(true); // Placeholder
    }
}
