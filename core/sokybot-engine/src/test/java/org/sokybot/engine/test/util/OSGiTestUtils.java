package org.sokybot.engine.test.util;

import org.osgi.framework.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for mocking OSGi services in tests.
 */
public class OSGiTestUtils {
    
    /**
     * Creates a mock BundleContext with service registry support.
     * Services can be registered and retrieved like a real OSGi registry.
     */
    public static MockBundleContext createMockBundleContext() {
        return new MockBundleContext();
    }
    
    /**
     * Mock BundleContext implementation for testing.
     * Provides service registration and lookup without real OSGi framework.
     */
    public static class MockBundleContext implements BundleContext {
        private final Map<Class<?>, List<ServiceEntry>> services = new HashMap<>();
        private long serviceIdCounter = 1;
        
        private static class ServiceEntry {
            final Object service;
            final ServiceReference<?> reference;
            final Map<String, Object> properties;
            
            ServiceEntry(Object service, ServiceReference<?> reference, Map<String, Object> properties) {
                this.service = service;
                this.reference = reference;
                this.properties = properties;
            }
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
            List<ServiceEntry> entries = services.get(clazz);
            if (entries == null || entries.isEmpty()) {
                return null;
            }
            // Return the first registered service
            return (ServiceReference<S>) entries.get(0).reference;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) {
            List<ServiceEntry> entries = services.get(clazz);
            if (entries == null || entries.isEmpty()) {
                return new ArrayList<>();
            }
            List<ServiceReference<S>> refs = new ArrayList<>();
            for (ServiceEntry entry : entries) {
                refs.add((ServiceReference<S>) entry.reference);
            }
            return refs;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <S> S getService(ServiceReference<S> reference) {
            if (reference == null) {
                return null;
            }
            // Find the service by searching all registered services
            for (List<ServiceEntry> entries : services.values()) {
                for (ServiceEntry entry : entries) {
                    if (entry.reference == reference) {
                        return (S) entry.service;
                    }
                }
            }
            return null;
        }
        
        @Override
        public boolean ungetService(ServiceReference<?> reference) {
            if (reference == null) {
                return false;
            }
            // Remove the service entry
            for (List<ServiceEntry> entries : services.values()) {
                if (entries.removeIf(e -> e.reference == reference)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Registers a mock service in this mock bundle context.
         * 
         * @param clazz The service interface class
         * @param service The service instance
         * @param properties Optional service properties
         * @return The service reference
         */
        @SuppressWarnings("unchecked")
        public <S> ServiceReference<S> registerMockService(Class<S> clazz, S service, Map<String, Object> properties) {
            MockServiceReference<S> ref = new MockServiceReference<>(clazz, serviceIdCounter++, properties);
            ServiceEntry entry = new ServiceEntry(service, ref, properties != null ? properties : new HashMap<>());
            
            services.computeIfAbsent(clazz, k -> new ArrayList<>()).add(entry);
            return ref;
        }
        
        /**
         * Unregisters a service.
         */
        public void unregisterService(ServiceReference<?> reference) {
            ungetService(reference);
        }
        
        /**
         * Unregisters all services.
         */
        public void clearServices() {
            services.clear();
        }
        
        // Stub implementations for other BundleContext methods (not used in tests)
        @Override
        public Bundle installBundle(String location) throws BundleException { return null; }
        @Override
        public Bundle installBundle(String location, java.io.InputStream input) throws BundleException { return null; }

        public <S> S getService(ServiceReference<S> reference, ServiceObjects<S> serviceObjects) {
            return getService(reference);
        }
        
        @Override
        public Bundle getBundle() { return null; }
        @Override
        public Bundle getBundle(long id) { return null; }
        @Override
        public Bundle getBundle(String location) { return null; }
        @Override
        public ServiceReference<?> getServiceReference(String clazz) { return null; }
        @Override
        public ServiceReference<?>[] getServiceReferences(String clazz, String filter) { return null; }
        @Override
        public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) { return null; }
        @Override
        public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) { return null; }
        @Override
        public Bundle[] getBundles() { return new Bundle[0]; }
        @Override
        public String getProperty(String key) { return null; }
        @Override
        public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
            Map<String, Object> props = new HashMap<>();
            if (properties != null) {
                for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
                    String key = e.nextElement();
                    props.put(key, properties.get(key));
                }
            }
            return new MockServiceRegistration<>(registerMockService(clazz, service, props));
        }
        @Override
        public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
             return registerService(new String[]{clazz}, service, properties);
        }
        @Override
        public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
            throw new UnsupportedOperationException("Use registerService(Class, Object) instead");
        }
        @Override
        public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory, Dictionary<String, ?> properties) {
            throw new UnsupportedOperationException("ServiceFactory not supported in mock");
        }
        @Override
        public void addServiceListener(ServiceListener listener, String filter) {}
        @Override
        public void addServiceListener(ServiceListener listener) {}
        @Override
        public void removeServiceListener(ServiceListener listener) {}
        @Override
        public void addBundleListener(BundleListener listener) {}
        @Override
        public void removeBundleListener(BundleListener listener) {}
        @Override
        public void addFrameworkListener(FrameworkListener listener) {}
        @Override
        public void removeFrameworkListener(FrameworkListener listener) {}
        @Override
        public File getDataFile(String filename) { return null; }
        @Override
        public Filter createFilter(String filter) { return null; }
    }
    
    /**
     * Mock ServiceReference implementation.
     */
    private static class MockServiceReference<S> implements ServiceReference<S> {
        private final Class<S> serviceClass;
        private final long id;
        private final Map<String, Object> properties;
        
        MockServiceReference(Class<S> serviceClass, long id, Map<String, Object> properties) {
            this.serviceClass = serviceClass;
            this.id = id;
            this.properties = properties != null ? properties : new HashMap<>();
        }
        
        @Override
        public Object getProperty(String key) {
            return properties.get(key);
        }
        
        @Override
        public String[] getPropertyKeys() {
            return properties.keySet().toArray(new String[0]);
        }
        
        @Override
        public Bundle getBundle() { return null; }
        @Override
        public Bundle[] getUsingBundles() { return new Bundle[0]; }
        @Override
        public boolean isAssignableTo(Bundle bundle, String className) { return false; }
        @Override
        public int compareTo(Object reference) { return 0; }

        @Override
        public <A> A adapt(Class<A> type) { return null; }

        @Override
        public java.util.Dictionary<String, Object> getProperties() {
            return new java.util.Hashtable<>(properties);
        }
        
        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }
    }
    
    /**
     * Mock ServiceRegistration implementation.
     */
    private static class MockServiceRegistration<S> implements ServiceRegistration<S> {
        private final ServiceReference<S> reference;
        
        MockServiceRegistration(ServiceReference<S> reference) {
            this.reference = reference;
        }
        
        @Override
        public ServiceReference<S> getReference() {
            return reference;
        }
        
        @Override
        public void setProperties(Dictionary<String, ?> properties) {}
        @Override
        public void unregister() {}
    }
}
