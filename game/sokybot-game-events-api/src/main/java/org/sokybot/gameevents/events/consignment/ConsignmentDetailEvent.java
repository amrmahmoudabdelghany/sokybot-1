package org.sokybot.gameevents.events.consignment;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

@Getter
@Builder
@ToString
public class ConsignmentDetailEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;

    private final byte result;
    private final byte detailType;
    private final String sellerName;
    private final int personalId;
    private final ConsignmentItemDetail item;
    private final Integer errorCode;

    @Getter
    @Builder
    @ToString
    public static class ConsignmentItemDetail {
        private final int refItemId;
        private final byte plus;
        private final long variance;
        private final int quantity;
        private final int durability;
        private final List<MagicOption> magicOptions;
    }

    @Getter
    @Builder
    @ToString
    public static class MagicOption {
        private final int type;
        private final int value;
    }
}
