package org.sokybot.engine.internal.journal;

/**
 * Records recent network lifecycle transitions into workflow persistent data.
 */
public interface INetworkTransitionJournal {

    void append(String transition, String loginPhase, String reason, String endpoint);
}
