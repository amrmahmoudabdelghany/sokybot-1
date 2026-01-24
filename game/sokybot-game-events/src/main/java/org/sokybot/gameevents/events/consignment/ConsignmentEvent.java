package org.sokybot.gameevents.events.consignment;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

/**
 * Event fired when consignment shop data is received or updated.
 */
@Getter
@Builder
@ToString
public class ConsignmentEvent implements IGameEvent {

    public enum ConsignmentEventType {
        LIST,
        REGISTERED,
        BOUGHT,
        UPDATED
    }

    private final String fullName;
    private final long timestamp;
    private final ConsignmentEventType type;
    private final List<ConsignmentItem> items;
    private final Integer personalId;
    private final Byte slot;
    private final Integer errorCode;

    @Getter
    @Builder
    @ToString
    public static class ConsignmentItem {
        private final Integer personalId;
        private final Byte saleStatus;
        private final Integer refItemId;
        private final Integer sellCount;
        private final Long price;
        private final Long deposit;
        private final Long sellFee;
        private final Integer endDate;
        private final Byte sourceSlot;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
