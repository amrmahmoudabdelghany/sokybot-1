package org.sokybot.town.api;

/**
 * Ways to leave town / staging area and resume hunting.
 */
public enum ReturnActionKind {

    USE_RECALL_SCROLL,
    WALK_TO_GATE,
    TELEPORT_VIA_NPC,
    FOLLOW_RECORDED_ROUTE,
    CUSTOM
}
