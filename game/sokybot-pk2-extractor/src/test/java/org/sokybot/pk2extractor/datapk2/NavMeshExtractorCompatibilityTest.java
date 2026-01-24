package org.sokybot.pk2extractor.datapk2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.dto.region.SectorRefData;
import org.sokybot.pk2extractor.test.AbstractExtractorCompatibilityTest;

class NavMeshExtractorCompatibilityTest extends AbstractExtractorCompatibilityTest<SectorRefData> {

    @Override
    protected IExtractor<SectorRefData> createExtractor() {
        return new NavMeshExtractor();
    }

    @Override
    protected IPk2Driver getPk2Driver() {
        return fixture.getDataPk2().orElseThrow(() -> new IllegalStateException("Data.pk2 not found"));
    }

    @Override
    protected int getMinimumExpectedItemCount() {
        return 50;
    }

    @Override
    protected void validateItem(SectorRefData item, int index) {
        super.validateItem(item, index);
        // Basic valid check
        // Sector ID is usually non-zero, but let's check structure mostly.
        
        // At least one list might be populated or if all empty, the sector ID checks out.
        // We trust the parsing logic unit test/code, here we check standard compliance.
        // Checking for negative values where positive expected
        if (!item.getNavObjects().isEmpty()) {
             item.getNavObjects().forEach(obj -> {
                 assertTrue(obj.getObjectId() > 0, "Nav object ID > 0");
             });
        }
    }
}
