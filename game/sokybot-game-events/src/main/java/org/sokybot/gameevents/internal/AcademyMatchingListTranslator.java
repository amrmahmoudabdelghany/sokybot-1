package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.academy.AcademyMatchingListEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates academy matching list response (opcode 0xB47D).
 */
public class AcademyMatchingListTranslator extends AbstractTranslator {

    public AcademyMatchingListTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public AcademyMatchingListTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB47D;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            byte result = reader.getByte();
            if (result != 1)
                return noEvents();

            byte pageCount = reader.getByte();
            byte pageIndex = reader.getByte();
            byte matchCount = reader.getByte();

            List<AcademyMatchingListEvent.AcademyMatch> matches = new ArrayList<>();
            for (int i = 0; i < matchCount; i++) {
                var builder = AcademyMatchingListEvent.AcademyMatch.builder();

                builder.number(reader.getInt());
                reader.getInt(); // unkUInt01
                builder.countryType(reader.getByte());

                int titleLen = reader.getShort() & 0xFFFF;
                builder.title(new String(reader.getBytes(titleLen)));

                reader.getInt(); // unkUInt02
                reader.getInt(); // unkUInt03
                builder.displayLevel(reader.getByte());
                builder.leaderLevel(reader.getByte());
                builder.leaderId(reader.getInt());

                int nameLen = reader.getShort() & 0xFFFF;
                builder.leaderName(new String(reader.getBytes(nameLen)));

                builder.graduatedCount(reader.getInt());
                builder.honorRank(reader.getByte());
                reader.getLong(); // unkULong04

                matches.add(builder.build());
            }

            return singleEvent(AcademyMatchingListEvent.builder()
                    .fullName(machineId)
                    .pageCount(pageCount)
                    .pageIndex(pageIndex)
                    .matches(matches)
                    .build());

        } catch (Exception e) {
            return noEvents();
        }
    }
}
