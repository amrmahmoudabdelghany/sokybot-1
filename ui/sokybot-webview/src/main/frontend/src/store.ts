import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';
import type { CharacterState } from './RSocketClient';

interface SokybotState {
    selectedMachineId: string | null;
    selectedPageId: string | null;

    characterStates: Record<string, CharacterState>;

    extensionRegistry: {
        pages: Record<string, unknown>;
        toolbarActions: Record<string, unknown>;
    };

    isSidebarOpen: boolean;
    theme: string;
    machineTabs: Record<string, string>;

    healthStatus: string;

    setSelectedMachineId: (id: string | null) => void;
    setSelectedPageId: (id: string | null) => void;
    setExtensionRegistry: (registry: { pages: Record<string, unknown>; toolbarActions: Record<string, unknown> }) => void;
    addExtensionPage: (page: Record<string, unknown> & { pageId: string }) => void;
    removeExtensionPage: (pageId: string) => void;
    updateCharacterState: (machineId: string, state: CharacterState) => void;
    setActiveTab: (machineId: string, tabId: string) => void;
    setHealthStatus: (status: string) => void;
}

export const useSokybotStore = create<SokybotState>()(
    immer((set) => ({
        selectedMachineId: null,
        selectedPageId: null,
        extensionRegistry: { pages: {}, toolbarActions: {} },
        characterStates: {},
        isSidebarOpen: true,
        theme: 'light',
        machineTabs: {},
        healthStatus: 'unknown',

        setSelectedMachineId: (id) =>
            set((state) => {
                state.selectedMachineId = id;
            }),
        setSelectedPageId: (id) =>
            set((state) => {
                state.selectedPageId = id;
            }),

        setExtensionRegistry: (extensionRegistry) =>
            set((state) => {
                state.extensionRegistry = extensionRegistry;
            }),

        addExtensionPage: (page) =>
            set((state) => {
                state.extensionRegistry.pages[page.pageId] = page;
            }),

        removeExtensionPage: (pageId) =>
            set((state) => {
                delete state.extensionRegistry.pages[pageId];
            }),

        updateCharacterState: (machineId, characterState) =>
            set((state) => {
                state.characterStates[machineId] = characterState;
            }),
        setActiveTab: (machineId, tabId) =>
            set((state) => {
                state.machineTabs[machineId] = tabId;
            }),
        setHealthStatus: (status) =>
            set((state) => {
                state.healthStatus = status;
            }),
    }))
);
