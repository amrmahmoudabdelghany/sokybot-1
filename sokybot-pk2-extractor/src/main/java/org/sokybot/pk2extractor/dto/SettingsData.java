package org.sokybot.pk2extractor.dto;

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
