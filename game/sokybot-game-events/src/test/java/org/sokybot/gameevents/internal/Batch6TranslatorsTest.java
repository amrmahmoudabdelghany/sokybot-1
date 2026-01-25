package org.sokybot.gameevents.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.character.CharacterRenameAckEvent;
import org.sokybot.gameevents.events.community.FriendListInfoEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.entity.EntitySpawnEvent;
import org.sokybot.gameevents.events.entity.GroupSpawnBeginEvent;
import org.sokybot.gameevents.events.skill.MasteryLevelDownEvent;
import org.sokybot.gameevents.events.world.CelestialUpdateEvent;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.IGameDataLookup;

class Batch6TranslatorsTest {

    private IGameDataLookup lookup;
    private final String machineName = "Group.Machine";

    @BeforeEach
    void setUp() {
        lookup = mock(IGameDataLookup.class);
    }

    private ImmutablePacket createPacket(int opcode, byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(6 + payload.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) payload.length);
        buffer.putShort((short) opcode);
        buffer.putShort((short) 0); // Security
        buffer.put(payload);
        return new ImmutablePacket(buffer);
    }

    @Test
    void shouldTranslateFriendListInfo() {
        // Groups: 1 (Group1), 2 (Group2)
        // Friends: Char1 (Model1, Group1, Offline), Char2 (Model2, Group2, Online)
        byte[] payload = new byte[] {
                0x02, // Group count
                0x01, 0x00, 0x06, 0x00, 'G', 'r', 'o', 'u', 'p', '1',
                0x02, 0x00, 0x06, 0x00, 'G', 'r', 'o', 'u', 'p', '2',
                0x02, // Friend count
                0x01, 0x00, 0x00, 0x00, 0x07, 0x00, 'F', 'r', 'i', 'e', 'n', 'd', '1',
                0x0B, 0x00, 0x00, 0x00, // Model
                0x01, 0x00, // Group
                0x01, // Offline
                0x02, 0x00, 0x00, 0x00, 0x07, 0x00, 'F', 'r', 'i', 'e', 'n', 'd', '2',
                0x0C, 0x00, 0x00, 0x00, // Model
                0x02, 0x00, // Group
                0x00 // Online
        };
        ImmutablePacket packet = createPacket(0x3305, payload);
        FriendListInfoTranslator translator = new FriendListInfoTranslator(lookup);

        List<IGameEvent> events = translator.translate(machineName, packet);

        assertThat(events).hasSize(1);
        FriendListInfoEvent event = (FriendListInfoEvent) events.get(0);
        assertThat(event.getGroups()).hasSize(2);
        assertThat(event.getFriends()).hasSize(2);
        assertThat(event.getFriends().get(0).getName()).isEqualTo("Friend1");
        assertThat(event.getFriends().get(0).isOffline()).isTrue();
        assertThat(event.getFriends().get(1).getName()).isEqualTo("Friend2");
        assertThat(event.getFriends().get(1).isOffline()).isFalse();
    }

    @Test
    void shouldTranslateCelestialUpdate() {
        byte[] payload = new byte[] {
                0x0F, 0x00, // Moonphase (15)
                0x0C, // Hour (12)
                0x1E // Minute (30)
        };
        ImmutablePacket packet = createPacket(0x3027, payload);
        CelestialUpdateTranslator translator = new CelestialUpdateTranslator(lookup);

        List<IGameEvent> events = translator.translate(machineName, packet);

        assertThat(events).hasSize(1);
        CelestialUpdateEvent event = (CelestialUpdateEvent) events.get(0);
        assertThat(event.getMoonphase()).isEqualTo(15);
        assertThat(event.getHour()).isEqualTo(12);
        assertThat(event.getMinute()).isEqualTo(30);
    }

    @Test
    void shouldTranslateMasteryLevelDown() {
        byte[] payload = new byte[] {
                0x00, // Success (false)
                0x06, 0x00 // ErrorCode (6)
        };
        ImmutablePacket packet = createPacket(0xB203, payload);
        MasteryLevelDownTranslator translator = new MasteryLevelDownTranslator(lookup);

        List<IGameEvent> events = translator.translate(machineName, packet);

        assertThat(events).hasSize(1);
        MasteryLevelDownEvent event = (MasteryLevelDownEvent) events.get(0);
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getErrorCode()).isEqualTo(6);
    }

    @Test
    void shouldTranslateGroupSpawnBegin() {
        byte[] payload = new byte[] {
                0x01, // SpawnType (1)
                0x05, 0x00 // ExpectedCount (5)
        };
        ImmutablePacket packet = createPacket(0x3017, payload);
        GroupSpawnBeginTranslator translator = new GroupSpawnBeginTranslator(lookup);

        List<IGameEvent> events = translator.translate(machineName, packet);

        assertThat(events).hasSize(1);
        GroupSpawnBeginEvent event = (GroupSpawnBeginEvent) events.get(0);
        assertThat(event.getSpawnType()).isEqualTo((byte) 1);
        assertThat(event.getCount()).isEqualTo((short) 5);
    }

    @Test
    void shouldTranslateCharacterRenameAck() {
        byte[] payload = new byte[] {
                0x01, // Action (Character Rename)
                0x02, // Result (Error)
                0x07, 0x00 // ErrorCode (7 - Not allowed)
        };
        ImmutablePacket packet = createPacket(0xB450, payload);
        CharacterRenameAckTranslator translator = new CharacterRenameAckTranslator(lookup);

        List<IGameEvent> events = translator.translate(machineName, packet);

        assertThat(events).hasSize(1);
        CharacterRenameAckEvent event = (CharacterRenameAckEvent) events.get(0);
        assertThat(event.getRenameAction()).isEqualTo((byte) 1);
        assertThat(event.getResult()).isEqualTo((byte) 2);
        assertThat(event.getErrorCode()).isEqualTo(7);
    }

    @Test
    void shouldTranslateGroupSpawnData() {
        // 1 entity
        byte[] payload = new byte[] {
                0x01, 0x00, // Count (1)
                0x0A, 0x00, 0x00, 0x00, // RefID
                0x01, 0x00, 0x00, 0x00, // UniqueID
                0x1B, 0x1C, // Sector X, Y
                0x00, 0x00, (byte) 0x80, 0x3F, // X Offset (1.0f)
                0x00, 0x00, 0x00, 0x40, // Z Offset (2.0f)
                0x00, 0x00, 0x40, 0x40, // Y Offset (3.0f)
                0x00, 0x00, // Angle
                0x00, // HasDest (false)
                0x01, // MovementType
                0x00, // SkyClickFlag
                0x00, 0x00, // Angle2
                0x01, // LifeState
                0x00, // Debuff
                0x00, // Motion
                0x00, // Status
                0x00, 0x00, (byte) 0x80, 0x3F, // Walk speed
                0x00, 0x00, 0x00, 0x40, // Run speed
                0x00, 0x00, 0x40, 0x40, // Hwan speed
                0x00, // Buff count
                0x00, // Talk flag
                0x00, // Rarity
                0x00, // Appearance
                0x00 // Strength level
        };
        NPCEntity entity = NPCEntity.builder()
                .refId(10)
                .level(1)
                .HP(100)
                .build();
        // Since NPCEntity is abstract or we need to check if we can instantiate it.
        // @Entity usually has no-args constructor.
        // It has @SuperBuilder.
        when(lookup.findNPC(anyInt())).thenReturn(Optional.of(entity));

        ImmutablePacket packet = createPacket(0x3019, payload);
        GroupSpawnDataTranslator translator = new GroupSpawnDataTranslator(lookup);

        List<IGameEvent> events = translator.translate(machineName, packet);

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(EntitySpawnEvent.class);
    }
}
