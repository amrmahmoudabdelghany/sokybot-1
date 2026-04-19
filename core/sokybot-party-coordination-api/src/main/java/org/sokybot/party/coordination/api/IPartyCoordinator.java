package org.sokybot.party.coordination.api;

/**
 * Process-wide broker for multi-bot party invites and matching advertisements (same JVM).
 */
public interface IPartyCoordinator {

    void requestInvite(String inviterMachine, String inviteeMachine);

    void registerMatchingPost(String leaderMachine, String title, int minLevel, int maxLevel);

    /**
     * Invoked when {@code inviteeMachine} receives an invite from {@code inviterEntityId}.
     *
     * @return {@code true} to accept, {@code false} to decline or ignore.
     */
    boolean onInviteReceived(String inviteeMachine, int inviterEntityId);
}
