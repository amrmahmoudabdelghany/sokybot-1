package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.consignment.ConsignmentDetailEvent;
import org.sokybot.gameevents.events.consignment.ConsignmentDetailEvent.ConsignmentItemDetail;
import org.sokybot.gameevents.events.consignment.ConsignmentDetailEvent.MagicOption;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.ArrayList;
import java.util.List;

public class ConsignmentDetailTranslator extends AbstractTranslator {

    public ConsignmentDetailTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public ConsignmentDetailTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB506;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte result = reader.getByte();
        byte detailType = 0;
        String sellerName = null;
        int personalId = 0;
        ConsignmentItemDetail item = null;
        Integer errorCode = null;

        if (result == 1) {
            detailType = reader.getByte();
            sellerName = reader.getString();
            personalId = reader.getInt();

            // Handle genericItemData
            // Based on Silkroad protocol genericItemData structure
            // RefID is usually read from the context or the first 4 bytes of generic data
            int refItemId = reader.getInt();
            byte plus = reader.getByte();
            long variance = reader.getLong();
            int quantity = reader.getInt();
            int durability = reader.getInt();

            byte magicOptionCount = reader.getByte();
            List<MagicOption> magicOptions = new ArrayList<>();
            for (int i = 0; i < magicOptionCount; i++) {
                magicOptions.add(MagicOption.builder()
                        .type(reader.getInt())
                        .value(reader.getInt())
                        .build());
            }

            item = ConsignmentItemDetail.builder()
                    .refItemId(refItemId)
                    .plus(plus)
                    .variance(variance)
                    .quantity(quantity)
                    .durability(durability)
                    .magicOptions(magicOptions)
                    .build();

        } else if (result == 2) {
            errorCode = (int) reader.getShort() & 0xFFFF;
        }

        return List.of(ConsignmentDetailEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .result(result)
                .detailType(detailType)
                .sellerName(sellerName)
                .personalId(personalId)
                .item(item)
                .errorCode(errorCode)
                .build());
    }
}
