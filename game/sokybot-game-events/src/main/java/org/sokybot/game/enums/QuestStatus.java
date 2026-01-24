package org.sokybot.game.enums;

// (1 = Untouched, 7 = Started, 8 = Complete)
public enum QuestStatus {

	Untouched(1), Started(7), Complete(8), UNKNOWN(0);

	private byte status;

	private QuestStatus(int status) {
		this.status = (byte) status;
	}

	public static QuestStatus of(byte val) {
		for (QuestStatus status : values()) {
			if(status.status == val) return status ; 
		}
		return UNKNOWN;
	}
}
