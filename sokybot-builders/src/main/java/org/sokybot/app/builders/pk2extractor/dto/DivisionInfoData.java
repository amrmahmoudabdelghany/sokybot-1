package org.sokybot.app.builders.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Division Info data extracted from pk2 files.
 * Contains locale info and list of divisions with their hosts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivisionInfoData {
    private byte local;
    private List<DivisionData> divisions = new ArrayList<>();
    
    public void addDivision(DivisionData division) {
        this.divisions.add(division);
    }
}
