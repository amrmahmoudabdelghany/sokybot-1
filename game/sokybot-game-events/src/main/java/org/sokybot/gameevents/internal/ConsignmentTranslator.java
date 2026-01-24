package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.consignment.ConsignmentEvent;
import org.sokybot.gameevents.events.consignment.ConsignmentEvent.ConsignmentEventType;
import org.sokybot.gameevents.events.consignment.ConsignmentEvent.ConsignmentItem;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConsignmentTranslator extends AbstractTranslator {

    public ConsignmentTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public ConsignmentTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0; // Unified translator for multiple opcodes
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        int opcode = packet.getOpcode();
        long timestamp = System.currentTimeMillis();

        switch (opcode) {
            case 0xB50E:
                return handleListResponse(machineId, timestamp, reader);
            case 0xB508:
                return handleRegisterResponse(machineId, timestamp, reader);
            case 0xB50A:
                return handleBuyResponse(machineId, timestamp, reader);
            case 0x350D:
                return handleUpdateNotification(machineId, timestamp, reader);
            default:
                return Collections.emptyList();
        }
    }

    private List<IGameEvent> handleListResponse(String machineId, long timestamp, IStreamReader reader) {
        byte result = reader.getByte();
        if (result == 1) {
            byte itemCount = reader.getByte();
            List<ConsignmentItem> items = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                items.add(ConsignmentItem.builder()
                        .personalId(reader.getInt())
                        .saleStatus(reader.getByte())
                        .refItemId(reader.getInt())
                        .sellCount(reader.getInt())
                        .price(reader.getLong())
                        .deposit(reader.getLong())
                        .sellFee(reader.getLong())
                        .endDate(reader.getInt())
                        .build());
            }
            return List.of(ConsignmentEvent.builder()
                    .fullName(machineId)
                    .timestamp(timestamp)
                    .type(ConsignmentEventType.LIST)
                    .items(items)
                    .build());
        } else {
            return List.of(ConsignmentEvent.builder()
                    .fullName(machineId)
                    .timestamp(timestamp)
                    .type(ConsignmentEventType.LIST)
                    .errorCode((int) reader.getShort())
                    .build());
        }
    }

    private List<IGameEvent> handleRegisterResponse(String machineId, long timestamp, IStreamReader reader) {
        byte result = reader.getByte();
        if (result == 1) {
            byte itemCount = reader.getByte();
            List<ConsignmentItem> items = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                items.add(ConsignmentItem.builder()
                        .sourceSlot(reader.getByte())
                        .saleStatus(reader.getByte())
                        .personalId(reader.getInt())
                        .refItemId(reader.getInt())
                        .deposit(reader.getLong())
                        .sellFee(reader.getLong())
                        .endDate(reader.getInt())
                        .build());
            }
            return List.of(ConsignmentEvent.builder()
                    .fullName(machineId)
                    .timestamp(timestamp)
                    .type(ConsignmentEventType.REGISTERED)
                    .items(items)
                    .build());
        } else {
            return List.of(ConsignmentEvent.builder()
                    .fullName(machineId)
                    .timestamp(timestamp)
                    .type(ConsignmentEventType.REGISTERED)
                    .errorCode((int) reader.getShort())
                    .build());
        }
    }

    private List<IGameEvent> handleBuyResponse(String machineId, long timestamp, IStreamReader reader) {
        byte result = reader.getByte();
        if (result == 1) {
            return List.of(ConsignmentEvent.builder()
                    .fullName(machineId)
                    .timestamp(timestamp)
                    .type(ConsignmentEventType.BOUGHT)
                    .personalId(reader.getInt())
                    .slot(reader.getByte())
                    .build());
        } else {
            return List.of(ConsignmentEvent.builder()
                    .fullName(machineId)
                    .timestamp(timestamp)
                    .type(ConsignmentEventType.BOUGHT)
                    .errorCode((int) reader.getShort())
                    .build());
        }
    }

    private List<IGameEvent> handleUpdateNotification(String machineId, long timestamp, IStreamReader reader) {
        byte itemCount = reader.getByte();
        List<ConsignmentItem> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(ConsignmentItem.builder()
                    .personalId(reader.getInt())
                    .refItemId(reader.getInt())
                    .saleStatus(reader.getByte())
                    .endDate(reader.getInt())
                    .build());
        }
        return List.of(ConsignmentEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .type(ConsignmentEventType.UPDATED)
                .items(items)
                .build());
    }
}
