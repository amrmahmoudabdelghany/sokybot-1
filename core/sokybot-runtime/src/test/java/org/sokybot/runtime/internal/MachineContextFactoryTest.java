package org.sokybot.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.sokybot.engine.IEngineFactory;
import org.sokybot.gamemodel.IGameModelFactory;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.proxy.IProxyConnectionFactory;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.internal.domain.MachineInfo;
import org.sokybot.runtime.test.RuntimeTestBase;

import java.util.Collections;

class MachineContextFactoryTest extends RuntimeTestBase {

    @Mock
    private IProxyConnectionFactory proxyFactory;
    
    @Mock
    private IGameModelFactory gameModelFactory;
    
    @Mock
    private IEngineFactory engineFactory;
    
    @Mock
    private IGroupContext groupContext;
    
    @Mock
    private IProxyConnection proxyConnection;
    
    @Mock
    private GroupContextImpl groupContextImpl; // To access getTranslators

    private MachineInfo machineInfo;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        
        machineInfo = new MachineInfo(1, TEST_MACHINE_NAME);
        
        // Mock Group Context (needs to cast to GroupContextImpl for shared translators)
        // Or we mock GroupContextImpl directly if createMachineContext casts it
        // The factory does: GroupContextImpl groupContextImpl = (GroupContextImpl) groupContext;
        // So we must pass a mock that is instance of GroupContextImpl
        
        lenient().when(groupContextImpl.name()).thenReturn(TEST_GROUP_NAME);
        lenient().when(groupContextImpl.getTranslators()).thenReturn(Collections.emptyMap());
        
        // Mock Factory behaviors
        lenient().when(proxyFactory.createConnection(anyString())).thenReturn(proxyConnection);
        lenient().when(gameModelFactory.create(anyString())).thenReturn(mock(org.sokybot.gamemodel.IGameModel.class));
        
        // Register services
        mockBundleContext.registerMockService(IProxyConnectionFactory.class, proxyFactory, null);
        mockBundleContext.registerMockService(IGameModelFactory.class, gameModelFactory, null);
        mockBundleContext.registerMockService(IEngineFactory.class, engineFactory, null);
    }

    @Test
    void testCreateMachineContext() {
        // Must pass GroupContextImpl mock
        IMachineContext context = MachineContextFactory.createMachineContext(
                machineInfo,
                groupContextImpl,
                mockBundleContext
        );
        
        assertNotNull(context);
        verify(proxyFactory).createConnection(anyString());
        verify(gameModelFactory).create(eq(TEST_GROUP_NAME + "." + TEST_MACHINE_NAME));
    }
    
    @Test
    void testCreateMachineContextMissingProxyFactory() {
        // Unregister proxy factory
        mockBundleContext.clearServices();
        // Re-register others but skip proxy
        mockBundleContext.registerMockService(IGameModelFactory.class, gameModelFactory, null);
        
        assertThrows(IllegalStateException.class, () -> {
            MachineContextFactory.createMachineContext(
                    machineInfo,
                    groupContextImpl,
                    mockBundleContext
            );
        });
    }
}
