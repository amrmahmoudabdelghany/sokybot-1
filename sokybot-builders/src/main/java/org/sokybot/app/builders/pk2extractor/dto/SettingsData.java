import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Settings data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsData {
    private String key;
    private String value;
}




