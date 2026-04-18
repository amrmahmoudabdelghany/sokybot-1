package org.sokybot.translators.trade.internal;

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.events.core.ITranslatorProvider;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Trade + stall translator provider (priority 50 = plugin tier).
 */
@Component(service = ITranslatorProvider.class, immediate = true, property = {
        "type=trade-stall",
        "service.ranking:Integer=50"
})
public class TradeTranslatorProvider implements ITranslatorProvider {

    @Override
    public boolean supports(int opcode, IGameDataLookup lookup) {
        switch (opcode) {
            case TradePacketOpcodes.EXCHANGE_STARTED:
            case TradePacketOpcodes.EXCHANGE_CONFIRMED:
            case TradePacketOpcodes.EXCHANGE_APPROVED:
            case TradePacketOpcodes.EXCHANGE_CANCELLED:
            case TradePacketOpcodes.EXCHANGE_UPDATE:
            case TradePacketOpcodes.STALL_ACTION:
            case TradePacketOpcodes.STALL_CREATED:
            case TradePacketOpcodes.STALL_DESTROYED:
            case TradePacketOpcodes.STALL_NAME_CHANGED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public IPacketTranslator createTranslator(int opcode, IGameDataLookup lookup) {
        switch (opcode) {
            case TradePacketOpcodes.EXCHANGE_STARTED:
                return ExchangeStartedTranslator.INSTANCE;
            case TradePacketOpcodes.EXCHANGE_CONFIRMED:
                return ExchangeConfirmedTranslator.INSTANCE;
            case TradePacketOpcodes.EXCHANGE_APPROVED:
                return ExchangeApprovedTranslator.INSTANCE;
            case TradePacketOpcodes.EXCHANGE_CANCELLED:
                return ExchangeCancelledTranslator.INSTANCE;
            case TradePacketOpcodes.EXCHANGE_UPDATE:
                return ExchangeUpdateTranslator.INSTANCE;
            case TradePacketOpcodes.STALL_ACTION:
                return StallActionTranslator.INSTANCE;
            case TradePacketOpcodes.STALL_CREATED:
                return StallCreatedTranslator.INSTANCE;
            case TradePacketOpcodes.STALL_DESTROYED:
                return StallDestroyedTranslator.INSTANCE;
            case TradePacketOpcodes.STALL_NAME_CHANGED:
                return StallNameChangedTranslator.INSTANCE;
            default:
                return null;
        }
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public Set<Integer> getSupportedOpcodes(IGameDataLookup lookup) {
        return Set.of(
                TradePacketOpcodes.EXCHANGE_STARTED,
                TradePacketOpcodes.EXCHANGE_CONFIRMED,
                TradePacketOpcodes.EXCHANGE_APPROVED,
                TradePacketOpcodes.EXCHANGE_CANCELLED,
                TradePacketOpcodes.EXCHANGE_UPDATE,
                TradePacketOpcodes.STALL_ACTION,
                TradePacketOpcodes.STALL_CREATED,
                TradePacketOpcodes.STALL_DESTROYED,
                TradePacketOpcodes.STALL_NAME_CHANGED);
    }
}
