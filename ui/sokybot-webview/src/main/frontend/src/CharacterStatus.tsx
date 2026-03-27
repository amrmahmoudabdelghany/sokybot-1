import React, { useCallback, useEffect, useRef, useState } from 'react';
import { rsocketService } from './RSocketClient';
import type { CharacterState } from './RSocketClient';
import type { MachineStatusEvent } from './RSocketClient';
import { Button, cn } from '@sokybot/frontend-shared';
import { useBotLifecycleMutation } from './query/sokybotQueries';

interface CharacterStatusProps {
    machineId?: string; // Optional machine ID to filter/request
    onConnectionStateChange?: (state: {
        machineId: string;
        connected: boolean;
        authenticated: boolean;
        loginPhase: string;
        agentsDiscovered: number;
    }) => void;
}

export const CharacterStatus: React.FC<CharacterStatusProps> = ({ machineId, onConnectionStateChange }) => {
    const botLifecycle = useBotLifecycleMutation();
    const [state, setState] = useState<CharacterState | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [saveAllMessage, setSaveAllMessage] = useState<string | null>(null);
    const [runtimeState, setRuntimeState] = useState({
        isRunning: false,
        connected: false,
        agentsDiscovered: 0
    });

    // Initial Fetch using new typed API
    const refreshTimeoutRef = useRef<number | null>(null);
    const lastRefreshAtRef = useRef<number>(0);
    const machineIdRef = useRef<string | undefined>(machineId);
    const agentsDiscoveredRef = useRef<number>(0);

    useEffect(() => {
        machineIdRef.current = machineId;
    }, [machineId]);

    useEffect(() => {
        agentsDiscoveredRef.current = runtimeState.agentsDiscovered ?? 0;
    }, [runtimeState.agentsDiscovered]);

    const fetchState = useCallback(async (options?: { silent?: boolean }) => {
        try {
            if (!options?.silent) {
                setLoading(true);
            }
            const requestMachineId = machineId;
            const data = await rsocketService.getCharacterState(requestMachineId);
            if (!requestMachineId || machineIdRef.current !== requestMachineId) {
                // Ignore stale async updates from a previously selected machine.
                return;
            }
            setRuntimeState({
                isRunning: Boolean(data?.isRunning),
                connected: Boolean(data?.connected),
                agentsDiscovered: data?.agentsDiscovered ?? 0
            });
            if (requestMachineId) {
                onConnectionStateChange?.({
                    machineId: requestMachineId,
                    connected: Boolean(data?.connected),
                    authenticated: Boolean(data?.authenticated),
                    loginPhase: data?.loginPhase || "DISCONNECTED",
                    agentsDiscovered: data?.agentsDiscovered ?? 0
                });
            }
            if (data && data.characterName) {
                setState(data);
            } else {
                // No character loaded yet
                setState(null);
            }
        } catch (err) {
            console.error("Failed to fetch character state", err);
            setError("Failed to load character data");
        } finally {
            if (!options?.silent) {
                setLoading(false);
            }
        }
    }, [machineId, onConnectionStateChange]);

    const scheduleRefresh = useCallback(() => {
        const now = Date.now();
        const minIntervalMs = 400;
        const elapsed = now - lastRefreshAtRef.current;
        const waitMs = elapsed >= minIntervalMs ? 0 : (minIntervalMs - elapsed);

        if (refreshTimeoutRef.current != null) {
            window.clearTimeout(refreshTimeoutRef.current);
        }

        refreshTimeoutRef.current = window.setTimeout(() => {
            lastRefreshAtRef.current = Date.now();
            void fetchState({ silent: true });
        }, waitMs);
    }, [fetchState]);

    useEffect(() => {
        void fetchState();
        return () => {
            if (refreshTimeoutRef.current != null) {
                window.clearTimeout(refreshTimeoutRef.current);
            }
        };
    }, [fetchState]);

    const handleToggleBot = async () => {
        if (!machineId) return;
        try {
            await botLifecycle.mutateAsync({
                machineId,
                running: runtimeState.isRunning,
            });
            void fetchState();
        } catch (err) {
            console.error("Failed to toggle bot", err);
        }
    };

    const isMachinePage = (pageId: string, currentMachineId: string): boolean => {
        return pageId.endsWith(`_${currentMachineId}`) || pageId.endsWith(`.${currentMachineId}`);
    };

    const isSettingsPage = (pageId: string): boolean => {
        const lowered = pageId.toLowerCase();
        return lowered.startsWith("connection_")
            || lowered.startsWith("training_")
            || lowered.startsWith("inventory_")
            || lowered.startsWith("skills_")
            || lowered.startsWith("healing_")
            || lowered.startsWith("navigation_")
            || lowered.startsWith("environment_");
    };

    const handleSaveAllSettings = async () => {
        if (!machineId) return;
        setSaveAllMessage("Saving all settings...");
        try {
            const registry = await rsocketService.getExtensionRegistry();
            const pageIds = Object.keys(registry?.pages || {})
                .filter((pageId) => isMachinePage(pageId, machineId) && isSettingsPage(pageId));

            if (pageIds.length === 0) {
                setSaveAllMessage("No settings pages found for this machine.");
                return;
            }

            let successCount = 0;
            const failedPages: string[] = [];

            for (const pageId of pageIds) {
                try {
                    await rsocketService.triggerExtensionAction(pageId, "save");
                    successCount += 1;
                } catch {
                    failedPages.push(pageId);
                }
            }

            if (failedPages.length === 0) {
                setSaveAllMessage(`Saved ${successCount}/${pageIds.length} settings pages.`);
            } else {
                setSaveAllMessage(
                    `Saved ${successCount}/${pageIds.length}. Failed: ${failedPages.join(", ")}`
                );
            }
        } catch (err) {
            console.error("Failed to save all settings", err);
            setSaveAllMessage("Unable to save all settings.");
        }
    };

    // Event Subscription using new typed API (game payload updates)
    useEffect(() => {
        const machineMarker = machineId || "";
        const sub = rsocketService.subscribeToGameEvents((event) => {
            if (!event) return;

            const eventTopicsRaw = (event as Record<string, unknown>)["event.topics"];
            const eventTopics = Array.isArray(eventTopicsRaw) ? eventTopicsRaw.map(String) : [];
            const isForCurrentMachine = machineMarker.length === 0
                || eventTopics.some((topic) => topic.includes(machineMarker));
            if (!isForCurrentMachine) return;

            // Handle EntityHPMPUpdateEvent
            if (event.eventType === 'EntityHPMPUpdateEvent') {
                // Check if it's for our character (by entityId)
                if (state?.entityId && event.entityId === state.entityId) {
                    setState(prev => {
                        if (!prev) return null;
                        const newState = { ...prev };
                        if (event.newHP !== undefined && event.newHP !== null) newState.currentHP = event.newHP as number;
                        if (event.newMP !== undefined && event.newMP !== null) newState.currentMP = event.newMP as number;
                        return newState;
                    });
                }
            }

            const characterRefreshEvents = new Set([
                "CharacterLoadedEvent",
                "LoginResponseEvent",
                "AuthResponseEvent",
                "AgentListEvent"
            ]);
            if (characterRefreshEvents.has(String(event.eventType))) {
                scheduleRefresh();
            }

        }, (err) => console.error("Stream error", err));

        return () => {
            if (sub && typeof sub.unsubscribe === 'function') {
                sub.unsubscribe();
            }
        };
    }, [machineId, scheduleRefresh, state?.entityId]);

    // Dedicated machine status stream driven by backend IConnectionListener callbacks.
    useEffect(() => {
        if (!machineId) return;
        const sub = rsocketService.subscribeToMachineStatus(
            machineId,
            (statusEvent: MachineStatusEvent) => {
                if (!statusEvent || statusEvent.machineId !== machineId) return;
                const nextConnected = Boolean(statusEvent.connected);
                const nextAuthenticated = Boolean(statusEvent.authenticated);
                const nextPhase = statusEvent.loginPhase || (nextConnected ? "CONNECTED" : "DISCONNECTED");

                setRuntimeState((prev) => ({
                    ...prev,
                    connected: nextConnected
                }));

                onConnectionStateChange?.({
                    machineId,
                    connected: nextConnected,
                    authenticated: nextAuthenticated,
                    loginPhase: nextPhase,
                    agentsDiscovered: agentsDiscoveredRef.current
                });
            },
            (err) => console.error("Machine status stream error", err)
        );
        return () => {
            if (sub && typeof sub.unsubscribe === 'function') {
                sub.unsubscribe();
            }
        };
    }, [machineId, onConnectionStateChange]);

    const hasCharacter = Boolean(state?.characterName);
    const hpPercent = hasCharacter ? (state!.currentHP / state!.maxHP) * 100 : 0;
    const mpPercent = hasCharacter ? (state!.currentMP / state!.maxMP) * 100 : 0;

    return (
        <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg flex flex-col gap-4">
            {/* Character Info */}
            <div className="border-b border-border pb-3">
                {hasCharacter ? (
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary border border-primary/20 shrink-0">
                            <span className="text-sm font-bold font-mono">
                                {state!.characterName.substring(0, 2).toUpperCase()}
                            </span>
                        </div>
                        <div className="min-w-0">
                            <div className="flex items-baseline gap-2">
                                <h2 className="text-lg font-bold text-foreground truncate" title={state!.characterName}>
                                    {state!.characterName}
                                </h2>
                            </div>
                            <div className="flex items-center gap-2">
                                <span className="text-xs font-semibold px-1.5 py-0.5 rounded bg-secondary text-secondary-foreground">
                                    Lv.{state!.level}
                                </span>
                                <div className="text-[10px] text-muted-foreground font-mono">
                                    ({state!.x}, {state!.y})
                                </div>
                            </div>
                        </div>
                    </div>
                ) : (
                    <div className="text-sm text-muted-foreground">
                        {loading
                            ? "Loading character status..."
                            : "No character loaded yet. Start bot and complete login to populate character data."}
                    </div>
                )}
                {error && (
                    <div className="mt-2 text-xs text-red-500">{error}</div>
                )}
            </div>

            {/* Status Bars */}
            {hasCharacter ? (
                <div className="space-y-3">
                    {/* HP */}
                    <div className="space-y-1">
                        <div className="flex justify-between text-[10px] font-bold text-muted-foreground uppercase tracking-tight">
                            <span>HP</span>
                            <span>{Math.round(hpPercent)}%</span>
                        </div>
                        <div className="relative h-3 bg-secondary rounded-full overflow-hidden border border-border">
                            <div
                                className="absolute top-0 left-0 h-full bg-red-500 transition-all duration-300 shadow-[0_0_10px_rgba(239,68,68,0.5)]"
                                style={{ width: `${hpPercent}%` }}
                            />
                            <div className="absolute inset-0 flex items-center justify-center text-[9px] text-white font-bold drop-shadow-sm">
                                {state!.currentHP.toLocaleString()} / {state!.maxHP.toLocaleString()}
                            </div>
                        </div>
                    </div>

                    {/* MP */}
                    <div className="space-y-1">
                        <div className="flex justify-between text-[10px] font-bold text-muted-foreground uppercase tracking-tight">
                            <span>MP</span>
                            <span>{Math.round(mpPercent)}%</span>
                        </div>
                        <div className="relative h-3 bg-secondary rounded-full overflow-hidden border border-border">
                            <div
                                className="absolute top-0 left-0 h-full bg-blue-500 transition-all duration-300 shadow-[0_0_10px_rgba(59,130,246,0.5)]"
                                style={{ width: `${mpPercent}%` }}
                            />
                            <div className="absolute inset-0 flex items-center justify-center text-[9px] text-white font-bold drop-shadow-sm">
                                {state!.currentMP.toLocaleString()} / {state!.maxMP.toLocaleString()}
                            </div>
                        </div>
                    </div>
                </div>
            ) : null}

            {/* Stats & Actions */}
            <div className="pt-2 border-t border-border space-y-3">
                {/* Gold */}
                {hasCharacter ? (
                    <div className="flex justify-between items-center bg-muted/50 px-3 py-2 rounded-md border border-border">
                        <span className="text-[10px] text-muted-foreground font-bold uppercase tracking-wider">Gold</span>
                        <span className="text-xs font-mono font-bold text-yellow-600 dark:text-yellow-500">
                            {state!.gold.toLocaleString()}
                        </span>
                    </div>
                ) : null}

                {/* Start/Stop Button */}
                <Button
                    onClick={handleToggleBot}
                    variant={runtimeState.isRunning ? "destructive" : "default"}
                    className={cn(
                        "w-full h-9 text-xs font-bold uppercase tracking-wider transition-all",
                        !runtimeState.isRunning && "bg-emerald-600 hover:bg-emerald-700 text-white shadow-[0_0_15px_rgba(5,150,105,0.3)]"
                    )}
                >
                    {runtimeState.isRunning ? "Stop Bot" : "Start Bot"}
                </Button>
                <Button
                    onClick={handleSaveAllSettings}
                    variant="secondary"
                    className="w-full h-9 text-xs font-bold uppercase tracking-wider"
                >
                    Save All Settings
                </Button>
                {saveAllMessage && (
                    <div className="text-[11px] text-muted-foreground text-center">
                        {saveAllMessage}
                    </div>
                )}

                {/* Connection Status Details */}
                <div className="bg-muted/40 rounded-md border border-border p-3 space-y-2">
                    <div className="text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
                        Connection Status
                    </div>
                    <div className="text-xs flex justify-between">
                        <span className="text-muted-foreground">Agents discovered</span>
                        <span className="font-mono">{runtimeState.agentsDiscovered ?? 0}</span>
                    </div>
                    {!runtimeState.connected && (
                        <div className="text-[11px] text-muted-foreground border border-border rounded p-2">
                            Review setup and credentials, then click Start Bot in Machine Status.
                        </div>
                    )}
                    {((runtimeState.agentsDiscovered ?? 0) <= 0) && (
                        <div className="text-[11px] text-muted-foreground border border-border rounded p-2">
                            No agents discovered. Use manual Target Agent ID or refresh when agents are available.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};
