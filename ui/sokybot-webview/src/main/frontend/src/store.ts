import { create } from 'zustand';
import { rsocketService } from './RSocketClient';
import type { MachineInfo, GroupInfo, CharacterState } from './RSocketClient';

interface SokybotState {
    // Machines & Groups
    machines: MachineInfo[];
    groups: GroupInfo[];
    selectedMachineId: string | null;
    selectedPageId: string | null;

    // Character Data (Cached by machineId)
    characterStates: Record<string, CharacterState>;

    // Extension Registry
    extensionRegistry: {
        pages: Record<string, any>;
        toolbarActions: Record<string, any>;
    };

    // UI State
    isSidebarOpen: boolean;
    theme: string;
    machineTabs: Record<string, string>;

    // Stats & Health
    healthStatus: string;
    lastError: string | null;

    // Actions
    setMachines: (machines: MachineInfo[]) => void;
    setGroups: (groups: GroupInfo[]) => void;
    setSelectedMachineId: (id: string | null) => void;
    setSelectedPageId: (id: string | null) => void;
    setExtensionRegistry: (registry: { pages: Record<string, any>; toolbarActions: Record<string, any> }) => void;
    addExtensionPage: (page: any) => void;
    removeExtensionPage: (pageId: string) => void;
    updateCharacterState: (machineId: string, state: CharacterState) => void;
    setActiveTab: (machineId: string, tabId: string) => void;
    setHealthStatus: (status: string) => void;
    setLastError: (error: string | null) => void;

    // Async Actions
    fetchInitialData: () => Promise<void>;
    fetchExtensionRegistry: () => Promise<void>;
    startBot: (machineId: string) => Promise<void>;
    stopBot: (machineId: string) => Promise<void>;
}

export const useSokybotStore = create<SokybotState>((set) => ({
    machines: [],
    groups: [],
    selectedMachineId: null,
    selectedPageId: null,
    extensionRegistry: { pages: {}, toolbarActions: {} },
    characterStates: {},
    isSidebarOpen: true,
    theme: 'light',
    machineTabs: {},
    healthStatus: 'unknown',
    lastError: null,

    setMachines: (machines) => set({ machines }),
    setGroups: (groups) => set({ groups }),
    setSelectedMachineId: (id: string | null) => set({ selectedMachineId: id }),
    setSelectedPageId: (id: string | null) => set({ selectedPageId: id }),

    setExtensionRegistry: (extensionRegistry) => set({ extensionRegistry }),

    addExtensionPage: (page) => set((prev) => ({
        extensionRegistry: {
            ...prev.extensionRegistry,
            pages: {
                ...prev.extensionRegistry.pages,
                [page.pageId]: page
            }
        }
    })),

    removeExtensionPage: (pageId) => set((prev) => {
        const pages = { ...prev.extensionRegistry.pages };
        delete pages[pageId];
        return {
            extensionRegistry: {
                ...prev.extensionRegistry,
                pages
            }
        };
    }),

    updateCharacterState: (machineId: string, state: CharacterState) => set((prev) => ({
        characterStates: { ...prev.characterStates, [machineId]: state }
    })),
    setActiveTab: (machineId: string, tabId: string) => set((prev) => ({
        machineTabs: { ...prev.machineTabs, [machineId]: tabId }
    })),
    setHealthStatus: (status: string) => set({ healthStatus: status }),
    setLastError: (error: string | null) => set({ lastError: error }),

    fetchInitialData: async () => {
        try {
            const [machines, groups] = await Promise.all([
                rsocketService.getMachines(),
                rsocketService.getGroups()
            ]);
            set({ machines, groups });
        } catch (err: any) {
            set({ lastError: err.message });
        }
    },

    fetchExtensionRegistry: async () => {
        try {
            const registry = await rsocketService.getExtensionRegistry();
            set({ extensionRegistry: registry });
        } catch (err: any) {
            console.error("Failed to fetch extension registry", err);
        }
    },

    startBot: async (machineId) => {
        try {
            await rsocketService.startBot(machineId);
            // Optimistic update or refresh
            const machines = await rsocketService.getMachines();
            set({ machines });
        } catch (err: any) {
            set({ lastError: err.message });
        }
    },

    stopBot: async (machineId) => {
        try {
            await rsocketService.stopBot(machineId);
            // Optimistic update or refresh
            const machines = await rsocketService.getMachines();
            set({ machines });
        } catch (err: any) {
            set({ lastError: err.message });
        }
    }
}));
