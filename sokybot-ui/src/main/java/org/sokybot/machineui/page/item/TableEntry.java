package org.sokybot.machineui.page.item;

import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.settings.ItemAction;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableEntry {
    private ItemEntity itemEntity;
    private ItemAction action;
    
    public TableEntry(ItemEntity itemEntity) {
        this.itemEntity = itemEntity;
        this.action = ItemAction.IGNORE;
    }
}
