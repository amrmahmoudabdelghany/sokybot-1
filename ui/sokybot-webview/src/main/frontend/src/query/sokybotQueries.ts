import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { rsocketService } from '../RSocketClient';
import { normalizeMachines } from '../utils/machines';
import { queryKeys } from './queryKeys';

/** Response from `extension.schema` RSocket route (declarative page + initial state). */
export type ExtensionSchemaResponse = {
    schema?: unknown;
    data?: Record<string, unknown>;
    state?: Record<string, unknown>;
};

export async function fetchExtensionSchema(
    pageId: string,
    machineId?: string
): Promise<ExtensionSchemaResponse> {
    return rsocketService.request<ExtensionSchemaResponse>('extension.schema', {
        pageId,
        machineId,
    });
}

/**
 * Machines list (deduped). Shares one network request with {@link useGroupsQuery} via
 * {@code workspace.summary} (with startup retry and legacy {@code group.list}/{@code machine.list} fallback).
 */
export function useMachinesQuery(enabled: boolean) {
    return useQuery({
        queryKey: queryKeys.sokybot.workspaceSummary(),
        queryFn: () => rsocketService.getWorkspaceSummary(),
        enabled,
        select: (data) => normalizeMachines(data.machines),
    });
}

/**
 * Groups list. Shares one network request with {@link useMachinesQuery} via {@code workspace.summary}.
 */
export function useGroupsQuery(enabled: boolean) {
    return useQuery({
        queryKey: queryKeys.sokybot.workspaceSummary(),
        queryFn: () => rsocketService.getWorkspaceSummary(),
        enabled,
        select: (data) => data.groups,
    });
}

export function useExtensionRegistryQuery(enabled: boolean) {
    return useQuery({
        queryKey: queryKeys.sokybot.extensionRegistry(),
        queryFn: () => rsocketService.getExtensionRegistry(),
        enabled,
    });
}

export function useExtensionSchemaQuery(
    pageId: string,
    machineId: string | undefined,
    options?: { enabled?: boolean }
) {
    const enabled = options?.enabled ?? Boolean(pageId);
    return useQuery({
        queryKey: queryKeys.sokybot.extensionSchema(pageId, machineId),
        queryFn: () => fetchExtensionSchema(pageId, machineId),
        enabled,
    });
}

export function useInvalidateSokybotQueries() {
    const qc = useQueryClient();
    return {
        invalidateMachines: () =>
            qc.invalidateQueries({ queryKey: queryKeys.sokybot.workspaceSummary() }),
        invalidateGroups: () =>
            qc.invalidateQueries({ queryKey: queryKeys.sokybot.workspaceSummary() }),
        invalidateExtensionRegistry: () =>
            qc.invalidateQueries({ queryKey: queryKeys.sokybot.extensionRegistry() }),
        invalidateExtensionSchema: (pageId: string, machineId?: string) =>
            qc.invalidateQueries({ queryKey: queryKeys.sokybot.extensionSchema(pageId, machineId) }),
        invalidateAllLists: () =>
            qc.invalidateQueries({ queryKey: queryKeys.sokybot.all }),
    };
}

export function useBotLifecycleMutation() {
    const { invalidateMachines } = useInvalidateSokybotQueries();
    return useMutation({
        mutationFn: async ({ machineId, running }: { machineId: string; running: boolean }) => {
            if (running) {
                await rsocketService.stopBot(machineId);
            } else {
                await rsocketService.startBot(machineId);
            }
        },
        onSuccess: () => {
            void invalidateMachines();
        },
    });
}
