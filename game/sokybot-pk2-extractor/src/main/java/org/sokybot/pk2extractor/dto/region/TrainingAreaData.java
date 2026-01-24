package org.sokybot.pk2extractor.dto.region;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Training Area data extracted from pk2 files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingAreaData {
    private Integer id;
    private String name;
    private String description;
}
