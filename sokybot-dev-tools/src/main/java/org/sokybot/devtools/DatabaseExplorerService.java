package org.sokybot.devtools;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.persistence.service.IPersistenceContextManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for exploring H2 databases via PersistenceContextManager.
 */
@Component(service = DatabaseExplorerService.class)
public class DatabaseExplorerService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseExplorerService.class);

    @Reference
    private IPersistenceContextManager persistenceManager;

    /**
     * List all active database contexts (game paths).
     */
    public List<String> listContexts() {
        if (persistenceManager == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(persistenceManager.getActiveContexts().keySet());
    }

    /**
     * List all tables in the specified game context.
     */
    public List<String> listTables(String gamePath) {
        if (persistenceManager == null || !persistenceManager.hasContext(gamePath)) {
            throw new IllegalArgumentException("Context not found: " + gamePath);
        }

        EntityManagerFactory emf = persistenceManager.getEntityManagerFactory(gamePath);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            // Query H2 system table
            Query query = em.createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' ORDER BY TABLE_NAME");
            List<Object> results = query.getResultList();
            return results.stream().map(Object::toString).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error listing tables for context: " + gamePath, e);
            throw new RuntimeException("Failed to list tables", e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Execute a SQL query on the specified game context.
     * RESTRICTED to SELECT statements for safety (simple check).
     */
    public List<Map<String, Object>> executeQuery(String gamePath, String sql) {
        if (persistenceManager == null || !persistenceManager.hasContext(gamePath)) {
            throw new IllegalArgumentException("Context not found: " + gamePath);
        }

        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }

        String lowerSql = sql.trim().toLowerCase();
        if (!lowerSql.startsWith("select") && !lowerSql.startsWith("show") && !lowerSql.startsWith("call")) {
             throw new IllegalArgumentException("Only SELECT/SHOW/CALL queries are allowed");
        }

        EntityManagerFactory emf = persistenceManager.getEntityManagerFactory(gamePath);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            
            // Hibernate native query returning map is tricky without result set mapping
            // But we can use the unwrapped implementation or just Object[] result
            Query query = em.createNativeQuery(sql);
            
            // Set 'org.hibernate.transform.AliasToEntityMapResultTransformer' is deprecated/complex in JPA
            // We'll map manually from Object[] or use a specific hint if possible.
            // For generic SQL executing, we might get Object[] or single Object
            
             // Use Hibernate specific API to return Maps
            org.hibernate.query.NativeQuery<?> nativeQuery = query.unwrap(org.hibernate.query.NativeQuery.class);
            nativeQuery.setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);
            
            List<?> resultList = nativeQuery.getResultList();
            
            // Cast to List<Map<String, Object>>
            List<Map<String, Object>> mappedResults = new ArrayList<>();
            for (Object o : resultList) {
                if (o instanceof Map) {
                    mappedResults.add((Map<String, Object>) o);
                } else {
                    // unexpected, wrap it?
                    Map<String, Object> map = new HashMap<>();
                    map.put("result", o);
                    mappedResults.add(map);
                }
            }
            return mappedResults;

        } catch (Exception e) {
            logger.error("Error executing query for context: " + gamePath + " SQL: " + sql, e);
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
