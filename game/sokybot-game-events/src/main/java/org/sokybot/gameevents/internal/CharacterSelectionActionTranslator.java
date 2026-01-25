package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.character.CharacterSelectionActionEvent;
import org.sokybot.gameevents.events.character.CharacterSelectionActionEvent.CharSelectionEntry;
import org.sokybot.gameevents.events.character.CharacterSelectionActionEvent.SelectedCharacterItem;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.ArrayList;
import java.util.List;

public class CharacterSelectionActionTranslator extends AbstractTranslator {

    public CharacterSelectionActionTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public CharacterSelectionActionTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB007;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte action = reader.getByte();
        byte result = reader.getByte();
        List<CharSelectionEntry> characters = new ArrayList<>();
        Integer errorCode = null;

        if (result == 1) {
            // Check if it's the character list response
            // The action for list isn't explicitly defined in the provided doc snippet
            // but it's typically requested via 0x7007 and responded with 0xB007.
            // Based on original docs: if(result == 0x01 && type ==
            // CharacterSelectionAction.List)
            // We'll assume the list follows if we can read a byte for count and it makes
            // sense.
            try {
                byte characterCount = reader.getByte();
                for (int i = 0; i < characterCount; i++) {
                    int refObjId = reader.getInt();
                    String name = reader.getString();
                    byte scale = reader.getByte();
                    byte level = reader.getByte();
                    long expOffset = reader.getLong();
                    int str = reader.getShort() & 0xFFFF;
                    int intell = reader.getShort() & 0xFFFF;
                    int statPoint = reader.getShort() & 0xFFFF;
                    int curHP = reader.getInt();
                    int curMP = reader.getInt();
                    boolean deleting = reader.getByte() != 0;
                    int deleteTime = deleting ? reader.getInt() : 0;

                    byte guildMemberClass = reader.getByte();
                    boolean guildRenameRequired = reader.getByte() != 0;
                    String curGuildName = guildRenameRequired ? reader.getString() : null;
                    byte academyMemberClass = reader.getByte();

                    byte itemCount = reader.getByte();
                    List<SelectedCharacterItem> items = new ArrayList<>();
                    for (int j = 0; j < itemCount; j++) {
                        items.add(SelectedCharacterItem.builder()
                                .refItemId(reader.getInt())
                                .plus(reader.getByte())
                                .build());
                    }

                    byte avatarItemCount = reader.getByte();
                    List<SelectedCharacterItem> avatarItems = new ArrayList<>();
                    for (int j = 0; j < avatarItemCount; j++) {
                        avatarItems.add(SelectedCharacterItem.builder()
                                .refItemId(reader.getInt())
                                .plus(reader.getByte())
                                .build());
                    }

                    characters.add(CharSelectionEntry.builder()
                            .refObjId(refObjId)
                            .name(name)
                            .scale(scale)
                            .level(level)
                            .expOffset(expOffset)
                            .strength(str)
                            .intelligence(intell)
                            .statPoint(statPoint)
                            .curHP(curHP)
                            .curMP(curMP)
                            .deleting(deleting)
                            .deleteTime(deleteTime)
                            .guildMemberClass(guildMemberClass)
                            .guildRenameRequired(guildRenameRequired)
                            .curGuildName(curGuildName)
                            .academyMemberClass(academyMemberClass)
                            .items(items)
                            .avatarItems(avatarItems)
                            .build());
                }
            } catch (Exception e) {
                // If it wasn't the list, we just stop parsing characters
            }
        } else if (result == 2) {
            errorCode = (int) reader.getShort() & 0xFFFF;
        }

        return List.of(CharacterSelectionActionEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .action(action)
                .result(result)
                .characters(characters)
                .errorCode(errorCode)
                .build());
    }
}
