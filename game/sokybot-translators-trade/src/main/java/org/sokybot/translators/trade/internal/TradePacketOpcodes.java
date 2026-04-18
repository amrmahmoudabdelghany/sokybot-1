package org.sokybot.translators.trade.internal;

/**
 * Agent exchange + stall opcodes (vSRO-style). Tune per-shard if layouts diverge.
 */
public final class TradePacketOpcodes {

    /** Player exchange started (server). */
    public static final int EXCHANGE_STARTED = 0x3085;
    /** Partner confirmed their side. */
    public static final int EXCHANGE_CONFIRMED = 0x3086;
    /** Exchange approved / completed by server. */
    public static final int EXCHANGE_APPROVED = 0x3087;
    /** Exchange cancelled. */
    public static final int EXCHANGE_CANCELLED = 0x3088;
    /** Item/gold slot update in exchange window. */
    public static final int EXCHANGE_UPDATE = 0x3089;

    /** Stall action (buy/sell/update). */
    public static final int STALL_ACTION = 0x30B7;
    /** Stall created / opened. */
    public static final int STALL_CREATED = 0x30B8;
    /** Stall destroyed. */
    public static final int STALL_DESTROYED = 0x30B9;
    /** Stall title changed. */
    public static final int STALL_NAME_CHANGED = 0x30BB;

    private TradePacketOpcodes() {
    }
}
