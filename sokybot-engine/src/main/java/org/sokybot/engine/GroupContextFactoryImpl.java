package org.sokybot.engine;

import org.osgi.service.component.annotations.Component;
import org.sokybot.IGroupContext;
import org.sokybot.app.domain.GroupInfo;
import org.sokybot.context.IGroupContextFactory;
import org.sokybot.machinegroup.MachineGroupConfig;
import org.sokybot.runtime.GroupContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Implementation of IGroupContextFactory that creates group contexts with Spring.
 * 
 * This factory creates Spring application contexts using MachineGroupConfig.
 * It's located in the engine bundle since it depends on Spring Boot.
 * 
 * It creates a Spring context and passes it to GroupContextImpl (from runtime bundle)
 * which wraps it and provides OSGi-based component access.
 */
@Component(service = IGroupContextFactory.class)
public class GroupContextFactoryImpl implements IGroupContextFactory {
    
    private static final Logger log = LoggerFactory.getLogger(GroupContextFactoryImpl.class);
    
    @Override
    public IGroupContext createGroupContext(GroupInfo groupInfo, 
                                            org.osgi.framework.BundleContext bundleContext) {
        log.info("Creating group context for: {} (Spring context will be created)", groupInfo.getName());
        
        try {
            // Create Spring context with MachineGroupConfig
            // Note: No parent context - groups are independent
            ConfigurableApplicationContext springContext = new SpringApplicationBuilder(MachineGroupConfig.class)
                    .properties(
                            "groupName:" + groupInfo.getName(),
                            "gamePath:" + groupInfo.getGamePath(),
                            "spring.config.location:classpath:machine-group-ctx.properties")
                    .initializers((ctx) -> {
                        ctx.setId(groupInfo.getName());
                        ctx.getBeanFactory().registerSingleton("groupInfo", groupInfo);
                    })
                    .run();
            
            // Create wrapper to provide Spring context to runtime bundle
            SpringGroupContextWrapper springWrapper = new SpringGroupContextWrapper(
                    groupInfo, springContext, bundleContext);
            
            // Create runtime GroupContextImpl which will use OSGi services
            // Pass the spring wrapper so it can access Spring beans when needed
            return new GroupContextImpl(groupInfo, springWrapper, bundleContext);
            
        } catch (Exception e) {
            log.error("Failed to create group context for: {}", groupInfo.getName(), e);
            throw new RuntimeException("Failed to create group context: " + groupInfo.getName(), e);
        }
    }
    
    @Override
    public void destroyGroupContext(IGroupContext groupContext) {
        if (groupContext instanceof GroupContextImpl) {
            ((GroupContextImpl) groupContext).destroy();
        }
    }
}
