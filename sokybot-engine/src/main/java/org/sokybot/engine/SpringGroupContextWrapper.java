package org.sokybot.engine;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sokybot.IGroupContext;
import org.sokybot.IMachineContext;
import org.sokybot.IMachineListener;
import org.sokybot.ui.api.IPageViewer;
import org.sokybot.app.domain.GroupInfo;
import org.sokybot.persistence.service.MachineInfoRepository;
import org.sokybot.service.ISroDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Wrapper that adapts Spring-based group context to IGroupContext interface.
 * 
 * This wrapper is used by GroupContextFactoryImpl to provide access to Spring context
 * beans while keeping Spring dependencies in the engine bundle.
 * 
 * The actual IGroupContext implementation in runtime bundle will wrap this.
 */
public class SpringGroupContextWrapper {
    
    private static final Logger log = LoggerFactory.getLogger(SpringGroupContextWrapper.class);
    
    private final GroupInfo groupInfo;
    private final ConfigurableApplicationContext springContext;
    private final BundleContext bundleContext;
    
    public SpringGroupContextWrapper(GroupInfo groupInfo, 
                                     ConfigurableApplicationContext springContext,
                                     BundleContext bundleContext) {
        this.groupInfo = groupInfo;
        this.springContext = springContext;
        this.bundleContext = bundleContext;
    }
    
    public GroupInfo getGroupInfo() {
        return groupInfo;
    }
    
    public ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
    
    public BundleContext getBundleContext() {
        return bundleContext;
    }
    
    public ISroDAO getGameDAO() {
        return springContext.getBean(ISroDAO.class);
    }
    
    public IPageViewer getPageViewer() {
        // Try OSGi service first
        if (bundleContext != null) {
            try {
                ServiceReference<IPageViewer> ref = bundleContext.getServiceReference(IPageViewer.class);
                if (ref != null) {
                    return bundleContext.getService(ref);
                }
            } catch (Exception e) {
                log.debug("IPageViewer not available from OSGi, trying Spring", e);
            }
        }
        
        // Fallback to Spring context
        try {
            return springContext.getBean(IPageViewer.class);
        } catch (Exception e) {
            log.warn("IPageViewer not available", e);
            return null;
        }
    }
    
    public MachineInfoRepository getMachineInfoRepository() {
        return springContext.getBean(MachineInfoRepository.class);
    }
    
    public void destroy() {
        if (springContext != null && springContext.isRunning()) {
            springContext.stop();
            springContext.close();
        }
    }
    
    public boolean isRunning() {
        return springContext != null && springContext.isRunning();
    }
}
