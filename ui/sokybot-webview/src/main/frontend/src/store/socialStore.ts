import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';
import type { ChatLineDto, SocialAlertDto, SocialChannel } from '../RSocketClient';

const CHAT_CAP = 500;
const ALERT_CAP = 100;

export interface SocialFilters {
    channels: Set<SocialChannel>;
    includeSelf: boolean;
    includeGm: boolean;
}

export interface SocialState {
    chatByMachine: Record<string, ChatLineDto[]>;
    filters: SocialFilters;
    alerts: SocialAlertDto[];
    pushChat: (machineId: string, line: ChatLineDto) => void;
    pushAlert: (a: SocialAlertDto) => void;
    setFilters: (f: Partial<SocialFilters>) => void;
    toggleFilterChannel: (ch: SocialChannel) => void;
    clearMachine: (machineId: string) => void;
}

const defaultFilters: SocialFilters = {
    channels: new Set(),
    includeSelf: false,
    includeGm: true,
};

export const useSocialStore = create<SocialState>()(
    immer((set) => ({
        chatByMachine: {},
        filters: {
            channels: new Set(defaultFilters.channels),
            includeSelf: defaultFilters.includeSelf,
            includeGm: defaultFilters.includeGm,
        },
        alerts: [],

        pushChat: (machineId, line) =>
            set((draft) => {
                const bucket = draft.chatByMachine[machineId] ?? (draft.chatByMachine[machineId] = []);
                bucket.push(line);
                while (bucket.length > CHAT_CAP) {
                    bucket.shift();
                }
            }),

        pushAlert: (a) =>
            set((draft) => {
                draft.alerts.push(a);
                while (draft.alerts.length > ALERT_CAP) {
                    draft.alerts.shift();
                }
            }),

        setFilters: (f) =>
            set((draft) => {
                if (f.channels !== undefined) {
                    draft.filters.channels = new Set(f.channels);
                }
                if (f.includeSelf !== undefined) {
                    draft.filters.includeSelf = f.includeSelf;
                }
                if (f.includeGm !== undefined) {
                    draft.filters.includeGm = f.includeGm;
                }
            }),

        toggleFilterChannel: (ch) =>
            set((draft) => {
                if (draft.filters.channels.has(ch)) {
                    draft.filters.channels.delete(ch);
                } else {
                    draft.filters.channels.add(ch);
                }
            }),

        clearMachine: (machineId) =>
            set((draft) => {
                delete draft.chatByMachine[machineId];
            }),
    }))
);
