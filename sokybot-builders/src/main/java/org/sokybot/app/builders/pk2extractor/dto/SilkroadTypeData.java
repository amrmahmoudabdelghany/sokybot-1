import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for Silkroad Type data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SilkroadTypeData {
    private Map<String, String> properties;
}




