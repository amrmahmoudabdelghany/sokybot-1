package org.sokybot.service;

import java.util.List;
import org.sokybot.model.geo.Vector2D;

public interface INavigationService {

    List<Vector2D> findPath(Vector2D start, Vector2D end);
    
    // Optional: Load NavMesh manually if needed, or handled internally by the implementation.
    // void loadNavMesh(ISroMaterialDAO dao); 
}
