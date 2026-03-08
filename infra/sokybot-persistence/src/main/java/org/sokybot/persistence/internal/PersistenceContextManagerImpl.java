package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.persistence.service.IPersistenceContextManager;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of IPersistenceContextManager.
 * Manages per-game EntityManagerFactory instances, each with its own database file.
 */
@Component(service = IPersistenceContextManager.class, immediate = true)
public class PersistenceContextManagerImpl implements IPersistenceContextManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PersistenceContextManagerImpl.class);
    private final Map<String, EntityManagerFactory> emfMap = new ConcurrentHashMap<>();
    private String dbBasePath;
    
    @Activate
    public void activate(Map<String, Object> config) {
        // Use configurable base path (OSGi ConfigAdmin or system property)
        Object pathObj = config != null ? config.get("db.base.path") : null;
        String rawPath = pathObj != null ? pathObj.toString() : 
            System.getProperty("sokybot.db.path", "./data/db");
        File dbDir = new File(rawPath);
        this.dbBasePath = dbDir.getAbsolutePath();
        
        if (!dbDir.exists() && !dbDir.mkdirs()) {
            throw new IllegalStateException("Failed to create database directory: " + dbBasePath + 
                ". Ensure the path is writable (e.g. set sokybot.db.path or db.base.path).");
        }
        
        logger.info("PersistenceContextManager activated with base path: {}", dbBasePath);
    }
    
    @Deactivate
    public void deactivate() {
        logger.info("Deactivating PersistenceContextManager, closing {} contexts", emfMap.size());
        for (Map.Entry<String, EntityManagerFactory> entry : emfMap.entrySet()) {
            try {
                EntityManagerFactory emf = entry.getValue();
                if (emf != null && emf.isOpen()) {
                    emf.close();
                }
            } catch (Exception e) {
                logger.error("Error closing EntityManagerFactory for game: {}", entry.getKey(), e);
            }
        }
        emfMap.clear();
    }
    
    @Override
    public EntityManagerFactory getEntityManagerFactory(String gamePath) {
        if (gamePath == null || gamePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Game path cannot be null or empty");
        }
        
        return emfMap.computeIfAbsent(gamePath, this::createEntityManagerFactory);
    }
    
    private EntityManagerFactory createEntityManagerFactory(String gamePath) {
        logger.info("Creating EntityManagerFactory for game: {}", gamePath);
        
        try {
            // Create game-specific database file name
            String dbFileName = sanitizeGamePath(gamePath) + ".db";
            String dbPath = dbBasePath + File.separator + dbFileName;
            
            Map<String, String> properties = new HashMap<>();
            properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
            properties.put("javax.persistence.jdbc.url", 
                "jdbc:h2:file:" + dbPath + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
            properties.put("javax.persistence.jdbc.user", "sa");
            properties.put("javax.persistence.jdbc.password", "");
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.show_sql", "false");
            properties.put("hibernate.format_sql", "true");
            properties.put("hibernate.use_sql_comments", "false");
            properties.put("hibernate.hikari.minimumIdle", "2");
            properties.put("hibernate.hikari.maximumPoolSize", "5");
            properties.put("hibernate.hikari.connectionTimeout", "30000");
            properties.put("hibernate.hikari.idleTimeout", "600000");
            
            // In OSGi, javax.persistence.Persistence.createEntityManagerFactory uses
            // ServiceLoader which cannot discover providers across bundle boundaries.
            // Instantiate HibernatePersistenceProvider directly.
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                
                PersistenceProvider provider = new org.hibernate.jpa.HibernatePersistenceProvider();
                EntityManagerFactory emf = provider.createEntityManagerFactory(
                    "sokybot-persistence-unit", properties);
                
                if (emf == null) {
                    throw new javax.persistence.PersistenceException(
                        "HibernatePersistenceProvider returned null for persistence unit 'sokybot-persistence-unit'. " +
                        "Check that META-INF/persistence.xml is on the classpath.");
                }
                
                logger.info("Successfully created EntityManagerFactory for game: {} at {}", 
                    gamePath, dbPath);
                return emf;
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
            
        } catch (Exception e) {
            logger.error("Failed to create EntityManagerFactory for game: {}", gamePath, e);
            String causeMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            String message = "Failed to create persistence context for game: " + gamePath +
                (causeMsg != null && !causeMsg.isEmpty() ? ". " + causeMsg : "");
            throw new RuntimeException(message, e);
        }
    }
    
    @Override
    public void closeEntityManagerFactory(String gamePath) {
        if (gamePath == null) {
            return;
        }
        
        EntityManagerFactory emf = emfMap.remove(gamePath);
        if (emf != null) {
            try {
                if (emf.isOpen()) {
                    emf.close();
                    logger.info("Closed EntityManagerFactory for game: {}", gamePath);
                }
            } catch (Exception e) {
                logger.error("Error closing EntityManagerFactory for game: {}", gamePath, e);
            }
        }
    }
    
    @Override
    public Map<String, EntityManagerFactory> getActiveContexts() {
        return new HashMap<>(emfMap);
    }
    
    @Override
    public boolean hasContext(String gamePath) {
        return gamePath != null && emfMap.containsKey(gamePath);
    }
    
    /**
     * Convert game path to a valid filename.
     */
    private String sanitizeGamePath(String gamePath) {
        if (gamePath == null) {
            return "unknown";
        }
        // Convert path to valid filename: replace invalid chars with underscore
        String sanitized = gamePath.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Limit length to avoid filesystem issues
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        return sanitized;
    }
}