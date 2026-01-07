import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ObjectNavMesh data extracted from pk2 files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectNavMeshData {
    private int refId;
    // Add additional fields as needed
}




