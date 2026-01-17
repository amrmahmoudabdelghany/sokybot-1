package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.service.ObjectNavMeshRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of ObjectNavMeshRepository using AbstractRepository base class.
 */
@Component(service = ObjectNavMeshRepository.class)
public class ObjectNavMeshRepositoryImpl extends AbstractRepository<ObjectNavMesh, Integer> 
        implements ObjectNavMeshRepository {
    
    /**
     * OSGi DS default constructor - will be injected via @Reference.
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
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "id";
    }
}