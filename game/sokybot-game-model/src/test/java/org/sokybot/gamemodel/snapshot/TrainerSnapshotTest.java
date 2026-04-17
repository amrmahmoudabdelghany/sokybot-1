package org.sokybot.gamemodel.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.dto.ItemData;
import org.sokybot.gamemodel.internal.Item;
import org.sokybot.gamemodel.internal.Trainer;
import org.sokybot.gamemodel.internal.snapshot.TrainerSnapshot;

class TrainerSnapshotTest {

    @Test
    void shouldExposeUnmodifiableCollectionGetters() {
        Trainer trainer = new Trainer();
        trainer.addItem(new Item(ItemData.builder().uniqueId(1).refId(2).name("Potion")
                .xSector(0).ySector(0).xOffset(0).yOffset(0).zOffset(0).angle((short) 0)
                .amount(10).ownerExist(false).ownerJID(0).plus((byte) 0).rarity(null).build(), (byte) 1));

        TrainerSnapshot snapshot = TrainerSnapshot.of(trainer);

        assertEquals(1, snapshot.getInventory().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getInventory().add(null));
    }
}
