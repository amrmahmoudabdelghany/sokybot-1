export const queryKeys = {
    sokybot: {
        all: ['sokybot'] as const,
        /** Shared key: one RSocket round-trip for machines + groups */
        workspaceSummary: () => [...queryKeys.sokybot.all, 'workspaceSummary'] as const,
        machines: () => [...queryKeys.sokybot.all, 'machines'] as const,
        groups: () => [...queryKeys.sokybot.all, 'groups'] as const,
        extensionRegistry: () => [...queryKeys.sokybot.all, 'extensionRegistry'] as const,
        extensionSchema: (pageId: string, machineId: string | undefined) =>
            [...queryKeys.sokybot.all, 'extensionSchema', pageId, machineId ?? ''] as const,
    },
};
