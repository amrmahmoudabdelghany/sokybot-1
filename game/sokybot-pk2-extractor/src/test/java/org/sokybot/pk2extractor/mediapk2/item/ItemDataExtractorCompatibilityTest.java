package org.sokybot.pk2extractor.mediapk2.item;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.item.ItemData;
import org.sokybot.pk2extractor.test.AbstractExtractorCompatibilityTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compatibility tests for ItemDataExtractor.
 * Tests against real PK2 files from game installations.
 * 
 * @author sokybot
 */
@DisplayName("ItemDataExtractor Compatibility Tests")
class ItemDataExtractorCompatibilityTest extends AbstractExtractorCompatibilityTest<ItemData> {

    @Override
    protected IExtractor<ItemData> createExtractor() {
        return new ItemDataExtractor();
    }

    @Override
    protected int getMinimumExpectedItemCount() {
        // ItemData files typically contain thousands of items
        return 100;
    }

    @Override
    protected void validateItem(ItemData item, int index) {
        super.validateItem(item, index);
        
        // ItemData should have a longId
        assertNotNull(item.getLongId(), 
            String.format("Item %d should have a longId", index));
        assertFalse(item.getLongId().isEmpty(), 
            String.format("Item %d longId should not be empty", index));
    }

    @Test
    @DisplayName("Extracted items have valid item types")
    void testItemTypesAreValid() {
        extractor.extract(getPk2Driver(), listener, progressListener);

        long itemsWithTypes = listener.getExtractedItems().stream()
            .filter(item -> item.getItemType() != null)
            .count();

        assertTrue(itemsWithTypes > 0, 
            "At least some items should have item types");
    }
}
