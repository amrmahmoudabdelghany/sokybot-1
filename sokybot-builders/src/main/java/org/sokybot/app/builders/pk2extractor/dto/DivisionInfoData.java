import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Division Info data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivisionInfoData {
    public byte local;
    private List<DivisionData> divisions = new ArrayList<>();
    
    public void addDivision(DivisionData division) {
        this.divisions.add(division);
    }
}




