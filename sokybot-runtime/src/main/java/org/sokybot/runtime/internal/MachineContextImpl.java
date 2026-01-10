package org.sokybot.runtime.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.IMachinePageViewer;
import org.sokybot.app.domain.MachineInfo;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.IEngineFactory;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.proxy.IConnectionListener;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.proxy.IProxyConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of IMachineContext that manages a single machine.
 * 
 * This implementation:
 * - Uses IEngineFactory to get engine (creates Spring context internally)
 * - Uses IProxyConnectionFactory to get proxy connection
 * - Provides access to packet publisher and page viewer via OSGi services
 * - No Spring dependencies - pure OSGi
 */
public class MachineContextImpl implements IMachineContext {
    
    private static final Logger log = LoggerFactory.getLogger(MachineContextImpl.class);
    
    private final MachineInfo machineInfo;
    private final IGroupContext groupContext;
    private final BundleContext bundleContext;
    
    private IEngine engine;
    private IProxyConnection proxyConnection;
    
    public MachineContextImpl(MachineInfo machineInfo, IGroupContext groupContext, BundleContext bundleContext) {
        this.machineInfo = machineInfo;
        this.groupContext = groupContext;
        this.bundleContext = bundleContext;
        initializeMachineComponents();
    }
    
    private void initializeMachineComponents() {
        String machineId = fullName();
        log.info("Initializing machine context for: {}", machineId);
        
        try {
            // Get factories from OSGi service registry
            IProxyConnectionFactory proxyFactory = getService(IProxyConnectionFactory.class);
            IEngineFactory engineFactory = getService(IEngineFactory.class);
            
            if (proxyFactory == null) {
                throw new IllegalStateException("IProxyConnectionFactory not available");
            }
            if (engineFactory == null) {
                throw new IllegalStateException("IEngineFactory not available");
            }
            
            // 1. Create proxy connection without listener initially
            // The listener (ConnectionHandler) will be set later by the engine
            proxyConnection = proxyFactory.createConnection(machineId);
            
            // 2. Create engine instance (engine factory creates Spring context internally)
            // The engine will create ConnectionHandler as a Spring bean and wire it to the proxy
            engine = engineFactory.createEngine(
                    machineId,
                    proxyConnection,
                    machineInfo.getGroup().getName(),
                    machineInfo.getMachineName()
            );
            
            log.info("Machine context initialized: {}", machineId);
            
        } catch (Exception e) {
            log.error("Failed to initialize machine components for: {}", machineId, e);
            throw new RuntimeException("Failed to initialize machine: " + machineId, e);
        }
    }
    
    private <T> T getService(Class<T> serviceClass) {
        if (bundleContext == null) {
            return null;
        }
        try {
            ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);
            if (ref != null) {
                return bundleContext.getService(ref);
            }
        } catch (Exception e) {
            log.debug("Service {} not available", serviceClass.getName(), e);
        }
        return null;
    }
    
    @Override
    public IMachinePageViewer machinePageViewer() {
        // Get from OSGi service registry
        return getService(IMachinePageViewer.class);
    }
    
    @Override
    public IPacketPublisher packetPublisher() {
        // Get from proxy connection
        if (proxyConnection != null) {
            return proxyConnection.getPacketPublisher();
        }
        return null;
    }
    
    @Override
    public String name() {
        return machineInfo.getMachineName();
    }
    
    @Override
    public String fullName() {
        return machineInfo.getGroup().getName() + "." + name();
    }
    
    @Override
    public boolean isRunning() {
        return engine != null && engine.isRunning();
    }
    
    @Override
    public IEngine getEngine() {
        return this.engine;
    }

    @Override
    public org.sokybot.settings.Settings getSettings() {
        if (this.engine != null) {
            return this.engine.getSettings();
        }
        return null; // Or throw generic exception? Returning null for now to be safe.
    }
    
    public void destroy() {
        log.info("Destroying machine context: {}", fullName());
        
        try {
            // Stop engine (destroys Spring context internally)
            if (engine != null) {
                IEngineFactory engineFactory = getService(IEngineFactory.class);
                if (engineFactory != null) {
                    engineFactory.destroyEngine(fullName());
                }
                engine = null;
            }
            
            // Note: Proxy connection cleanup is handled by proxy bundle
            proxyConnection = null;
            
            log.info("Machine context destroyed: {}", fullName());
        } catch (Exception e) {
            log.error("Error destroying machine context: {}", fullName(), e);
        }
    }
}
