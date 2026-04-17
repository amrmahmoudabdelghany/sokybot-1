import type { CharacterState, MachineStatusEvent } from '../RSocketClient';

declare module '../RSocketClient' {
    interface CharacterState {
        lastFailureReason?: string | null;
        retryCount?: number | null;
        maxRetries?: number | null;
        configuredClientVersion?: number | null;
    }

    interface MachineStatusEvent {
        lastFailureReason?: string | null;
        retryCount?: number | null;
        maxRetries?: number | null;
        configuredClientVersion?: number | null;
    }
}

export type _CharacterStateAugmented = CharacterState;
export type _MachineStatusEventAugmented = MachineStatusEvent;

