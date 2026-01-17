import org.sokybot.commons.ItemAction;

import java.util.Map;

public class ItemSettings {

	private Map<Integer, ItemAction> itemSettings;

	public ItemAction getItemAction(int itemRef) {
		if (!itemSettings.containsKey(itemRef))
			return ItemAction.IGNORE;

		return itemSettings.get(itemRef);
	}

	public void setItemAction(int itemRef, ItemAction action) {
		this.itemSettings.put(itemRef, action);
	}

}
