package org.sokybot.devtools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for inspecting OSGi services.
 */
public class ServiceInspectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceInspectionService.class);
    
    private final BundleContext bundleContext;
    
    public ServiceInspectionService(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    /**
     * List all OSGi services.
     */
    public List<Map<String, Object>> listServices() {
        List<Map<String, Object>> services = new ArrayList<>();
        
        try {
            ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(null, null);
            if (refs != null) {
                for (ServiceReference<?> ref : refs) {
                    services.add(serviceReferenceToMap(ref));
                }
            }
        } catch (Exception e) {
            logger.error("Error listing services", e);
        }
        
        return services;
    }
    
    /**
     * Find services by interface name.
     */
    public List<Map<String, Object>> findServicesByInterface(String interfaceName) {
        List<Map<String, Object>> services = new ArrayList<>();
        
        try {
            ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(interfaceName, null);
            if (refs != null) {
                for (ServiceReference<?> ref : refs) {
                    services.add(serviceReferenceToMap(ref));
                }
            }
        } catch (Exception e) {
            logger.error("Error finding services for interface: " + interfaceName, e);
        }
        
        return services;
    }
    
    /**
     * Get service properties.
     */
    public Map<String, Object> getServiceProperties(long serviceId) {
        try {
            ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(null, 
                "(service.id=" + serviceId + ")");
            if (refs != null && refs.length > 0) {
                return serviceReferenceToMap(refs[0]);
            }
        } catch (Exception e) {
            logger.error("Error getting service properties for ID: " + serviceId, e);
        }
        return null;
    }
    
    /**
     * Convert ServiceReference to map for JSON serialization.
     */
    private Map<String, Object> serviceReferenceToMap(ServiceReference<?> ref) {
        Map<String, Object> service = new HashMap<>();
        
        // Get object class (interfaces)
        Object objectClass = ref.getProperty("objectClass");
        if (objectClass instanceof String[]) {
            List<String> interfaces = new ArrayList<>();
            for (String iface : (String[]) objectClass) {
                interfaces.add(iface);
            }
            service.put("interfaces", interfaces);
        } else if (objectClass instanceof String) {
            service.put("interfaces", List.of((String) objectClass));
        }
        
        service.put("serviceId", ref.getProperty("service.id"));
        service.put("bundleId", ref.getBundle().getBundleId());
        service.put("bundleSymbolicName", ref.getBundle().getSymbolicName());
        
        // Add all service properties
        Map<String, Object> properties = new HashMap<>();
        String[] propertyKeys = ref.getPropertyKeys();
        if (propertyKeys != null) {
            for (String key : propertyKeys) {
                Object value = ref.getProperty(key);
                // Convert to serializable format
                if (value instanceof String[] || value instanceof Object[]) {
                    List<Object> list = new ArrayList<>();
                    for (Object item : (Object[]) value) {
                        list.add(item);
                    }
                    properties.put(key, list);
                } else {
                    properties.put(key, value);
                }
            }
        }
        service.put("properties", properties);
        
        return service;
    }
}
