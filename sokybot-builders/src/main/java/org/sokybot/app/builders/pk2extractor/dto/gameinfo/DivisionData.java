import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Division data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivisionData {
    private String name;
    private List<String> hosts = new ArrayList<>();
    
    public void addHost(String host) {
        this.hosts.add(host);
    }
    
    public void setName(String name) {
        this.name = name;
    }
}




