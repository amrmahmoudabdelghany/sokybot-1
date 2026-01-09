package org.sokybot.pk2extractor.dto.gameinfo;

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
