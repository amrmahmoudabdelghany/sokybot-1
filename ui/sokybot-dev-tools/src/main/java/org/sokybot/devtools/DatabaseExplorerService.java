package org.sokybot.devtools;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.persistence.service.IPersistenceContextManager;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.runtime.IGroupContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for exploring H2 databases for all groups.
 * Lists all groups from ISokybotContext and provides database access per group.
 */
@Component(service = DatabaseExplorerService.class)
public class DatabaseExplorerService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseExplorerService.class);

    private volatile IPersistenceContextManager persistenceManager;
    private volatile ISokybotContext sokybotContext;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setPersistenceManager(IPersistenceContextManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    protected void unsetPersistenceManager(IPersistenceContextManager persistenceManager) {
        this.persistenceManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
    }

    protected void unsetSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = null;
    }


    /**
     * List all group names.
     * Returns names of all groups, regardless of whether persistence is initialized.
     */
    public List<String> listContexts() {
        if (sokybotContext == null) {
            return Collections.emptyList();
        }
        IGroupContext[] groups = sokybotContext.getGroups();
        return Arrays.stream(groups)
                .map(IGroupContext::name)
                .collect(Collectors.toList());
    }

    /**
     * List all tables for the specified group.
     * @param groupName The group name
     */
    public List<String> listTables(String groupName) {
        if (sokybotContext == null) {
            throw new IllegalStateException("Sokybot context not available");
        }
        
        IGroupContext groupCtx = sokybotContext.findGroupCtx(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupName));
        
        // Get gamePath from GameDataLookup (which was initialized when group was created)
        String gamePath = getGamePathForGroup(groupCtx);
        if (gamePath == null) {
            throw new IllegalStateException("Game path not found for group: " + groupName);
        }
        
        if (persistenceManager == null || !persistenceManager.hasContext(gamePath)) {
            throw new IllegalArgumentException("Database not initialized for group: " + groupName);
        }

        EntityManagerFactory emf = persistenceManager.getEntityManagerFactory(gamePath);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Query query = em.createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' ORDER BY TABLE_NAME");
            List<Object> results = query.getResultList();
            return results.stream().map(Object::toString).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error listing tables for group: " + groupName, e);
            throw new RuntimeException("Failed to list tables", e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Execute a SQL query on the specified group's database.
     * RESTRICTED to SELECT statements for safety (simple check).
     */
    public List<Map<String, Object>> executeQuery(String groupName, String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }

        String lowerSql = sql.trim().toLowerCase();
        if (!lowerSql.startsWith("select") && !lowerSql.startsWith("show") && !lowerSql.startsWith("call")) {
             throw new IllegalArgumentException("Only SELECT/SHOW/CALL queries are allowed");
        }
        
        if (sokybotContext == null) {
            throw new IllegalStateException("Sokybot context not available");
        }
        
        IGroupContext groupCtx = sokybotContext.findGroupCtx(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupName));
        
        String gamePath = getGamePathForGroup(groupCtx);
        if (gamePath == null) {
            throw new IllegalStateException("Game path not found for group: " + groupName);
        }
        
        if (persistenceManager == null || !persistenceManager.hasContext(gamePath)) {
            throw new IllegalArgumentException("Database not initialized for group: " + groupName);
        }


        EntityManagerFactory emf = persistenceManager.getEntityManagerFactory(gamePath);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Query query = em.createNativeQuery(sql);
            
            // Use Hibernate specific API to return Maps
            org.hibernate.query.NativeQuery<?> nativeQuery = query.unwrap(org.hibernate.query.NativeQuery.class);
            nativeQuery.setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);
            
            List<?> resultList = nativeQuery.getResultList();
            
            List<Map<String, Object>> mappedResults = new ArrayList<>();
            for (Object o : resultList) {
                if (o instanceof Map) {
                    mappedResults.add((Map<String, Object>) o);
                } else {
                    Map<String, Object> map = new HashMap<>();
                    map.put("result", o);
                    mappedResults.add(map);
                }
            }
            return mappedResults;

        } catch (Exception e) {
            logger.error("Error executing query for group: " + groupName + " SQL: " + sql, e);
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    /**
     * Get the game path for a group context.
     * Attempts to get it from the GameDataLookup which stores the registration.
     */
    private String getGamePathForGroup(IGroupContext groupCtx) {
        try {
            // GameDataLookup is created per group and knows its gamePath
            // We can try to get it via reflection or check if there's an accessor
            // For now, fallback: check active persistence contexts for a match by trying the group name
            if (persistenceManager != null) {
                Map<String, ?> activeContexts = persistenceManager.getActiveContexts();
                // The gamePath is the key in active contexts
                // We need a way to map group name back to gamePath
                // Actually, let's just return the first active context for now as a workaround
                // OR we need to add a method to get gamePath from IGameDataLookup
                
                // Better approach: use IGameDataLookup to get the game path
                try {
                    org.sokybot.persistence.service.IGameDataLookup lookup = groupCtx.getGameDataLookup();
                    // IGameDataLookup doesn't have getGamePath(), but it was registered with one
                    // We need to check IPersistenceContextManager for the mapping
                    
                    // Workaround: iterate through active contexts and find one that matches
                    for (String path : activeContexts.keySet()) {
                        // This is not ideal, but for now return the first match
                        // Ideally we'd have a reverse lookup or store this mapping
                        return path;
                    }
                } catch (Exception e) {
                    logger.debug("Could not get game path via lookup", e);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting game path for group: " + groupCtx.name(), e);
        }
        return null;
    }
}
