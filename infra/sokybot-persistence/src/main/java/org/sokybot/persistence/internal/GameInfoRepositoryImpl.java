package org.sokybot.persistence.internal;

import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.persistence.service.GameInfoRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of GameInfoRepository using AbstractRepository base class.
 */
public class GameInfoRepositoryImpl extends AbstractRepository<GameInfo, String> 
        implements GameInfoRepository {
    
    /**
     */
    public GameInfoRepositoryImpl() {
        super(GameInfo.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public GameInfoRepositoryImpl(EntityManagerFactory emf) {
        super(emf, GameInfo.class);
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "gamePath";
    }
}