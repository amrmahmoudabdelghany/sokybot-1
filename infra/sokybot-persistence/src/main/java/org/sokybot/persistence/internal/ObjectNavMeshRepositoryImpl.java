package org.sokybot.persistence.internal;

import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.service.ObjectNavMeshRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of ObjectNavMeshRepository using AbstractRepository base class.
 */
public class ObjectNavMeshRepositoryImpl extends AbstractRepository<ObjectNavMesh, Integer> 
        implements ObjectNavMeshRepository {
    
    /**
     */
    public ObjectNavMeshRepositoryImpl() {
        super(ObjectNavMesh.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public ObjectNavMeshRepositoryImpl(EntityManagerFactory emf) {
        super(emf, ObjectNavMesh.class);
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "id";
    }
}