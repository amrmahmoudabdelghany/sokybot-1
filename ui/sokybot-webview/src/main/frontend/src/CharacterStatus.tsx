import React, { useEffect, useState } from 'react';
import { rsocketService } from './RSocketClient';
import type { CharacterState } from './RSocketClient';
import { Button, cn } from '@sokybot/frontend-shared';

interface CharacterStatusProps {
    machineId?: string; // Optional machine ID to filter/request
}

export const CharacterStatus: React.FC<CharacterStatusProps> = ({ machineId }) => {
    const [state, setState] = useState<CharacterState | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Initial Fetch using new typed API
    const fetchState = async () => {
        try {
            setLoading(true);
            const data = await rsocketService.getCharacterState(machineId);
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
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchState();
    }, [machineId]);

    const handleToggleBot = async () => {
        if (!state || !machineId) return;
        try {
            if (state.isRunning) {
                await rsocketService.stopBot(machineId);
            } else {
                await rsocketService.startBot(machineId);
            }
            // Refetch state after action
            fetchState();
        } catch (err) {
            console.error("Failed to toggle bot", err);
        }
    };

    // Event Subscription using new typed API
    useEffect(() => {
        if (!state?.entityId) return; // Wait until we have entity ID to match updates

        const sub = rsocketService.subscribeToGameEvents((event) => {
            if (!event || !event.eventType) return;

            // Handle EntityHPMPUpdateEvent
            if (event.eventType === 'EntityHPMPUpdateEvent') {
                // Check if it's for our character (by entityId)
                if (event.entityId === state.entityId) {
                    setState(prev => {
                        if (!prev) return null;
                        const newState = { ...prev };
                        if (event.newHP !== undefined && event.newHP !== null) newState.currentHP = event.newHP as number;
                        if (event.newMP !== undefined && event.newMP !== null) newState.currentMP = event.newMP as number;
                        return newState;
                    });
                }
            }

            // Handle other updates
            if (event.eventType === 'CharacterLoadedEvent') {
                fetchState();
            }

        }, (err) => console.error("Stream error", err));

        return () => {
            if (sub && typeof sub.unsubscribe === 'function') {
                sub.unsubscribe();
            }
        };
    }, [state?.entityId, machineId]);

    if (loading) return <div>Loading Character Status...</div>;
    if (error) return <div className="text-red-500">{error}</div>;
    if (!state) return <div className="p-4">No character loaded</div>;

    const hpPercent = (state.currentHP / state.maxHP) * 100;
    const mpPercent = (state.currentMP / state.maxMP) * 100;

    return (
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 shadow-sm rounded-lg flex flex-col gap-4">
            {/* Character Info */}
            <div className="flex items-center gap-3 border-b border-slate-100 dark:border-slate-800 pb-3">
                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary border border-primary/20 shrink-0">
                    <span className="text-sm font-bold font-mono">
                        {state.characterName.substring(0, 2).toUpperCase()}
                    </span>
                </div>
                <div className="min-w-0">
                    <div className="flex items-baseline gap-2">
                        <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100 truncate" title={state.characterName}>
                            {state.characterName}
                        </h2>
                    </div>
                    <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold px-1.5 py-0.5 rounded bg-secondary text-secondary-foreground">
                            Lv.{state.level}
                        </span>
                        <div className="text-[10px] text-muted-foreground font-mono">
                            ({state.x}, {state.y})
                        </div>
                    </div>
                </div>
            </div>

            {/* Status Bars */}
            <div className="space-y-3">
                {/* HP */}
                <div className="space-y-1">
                    <div className="flex justify-between text-[10px] font-bold text-muted-foreground uppercase tracking-tight">
                        <span>HP</span>
                        <span>{Math.round(hpPercent)}%</span>
                    </div>
                    <div className="relative h-3 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden border border-slate-200 dark:border-slate-700">
                        <div
                            className="absolute top-0 left-0 h-full bg-red-500 transition-all duration-300 shadow-[0_0_10px_rgba(239,68,68,0.5)]"
                            style={{ width: `${hpPercent}%` }}
                        />
                        <div className="absolute inset-0 flex items-center justify-center text-[9px] text-white font-bold drop-shadow-sm">
                            {state.currentHP.toLocaleString()} / {state.maxHP.toLocaleString()}
                        </div>
                    </div>
                </div>

                {/* MP */}
                <div className="space-y-1">
                    <div className="flex justify-between text-[10px] font-bold text-muted-foreground uppercase tracking-tight">
                        <span>MP</span>
                        <span>{Math.round(mpPercent)}%</span>
                    </div>
                    <div className="relative h-3 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden border border-slate-200 dark:border-slate-700">
                        <div
                            className="absolute top-0 left-0 h-full bg-blue-500 transition-all duration-300 shadow-[0_0_10px_rgba(59,130,246,0.5)]"
                            style={{ width: `${mpPercent}%` }}
                        />
                        <div className="absolute inset-0 flex items-center justify-center text-[9px] text-white font-bold drop-shadow-sm">
                            {state.currentMP.toLocaleString()} / {state.maxMP.toLocaleString()}
                        </div>
                    </div>
                </div>
            </div>

            {/* Stats & Actions */}
            <div className="pt-2 border-t border-slate-100 dark:border-slate-800 space-y-3">
                {/* Gold */}
                <div className="flex justify-between items-center bg-slate-50 dark:bg-slate-950 px-3 py-2 rounded-md border border-slate-100 dark:border-slate-800">
                    <span className="text-[10px] text-muted-foreground font-bold uppercase tracking-wider">Gold</span>
                    <span className="text-xs font-mono font-bold text-yellow-600 dark:text-yellow-500">
                        {state.gold.toLocaleString()}
                    </span>
                </div>

                {/* Start/Stop Button */}
                <Button
                    onClick={handleToggleBot}
                    variant={state.isRunning ? "destructive" : "default"}
                    className={cn(
                        "w-full h-9 text-xs font-bold uppercase tracking-wider transition-all",
                        !state.isRunning && "bg-emerald-600 hover:bg-emerald-700 text-white shadow-[0_0_15px_rgba(5,150,105,0.3)]"
                    )}
                >
                    {state.isRunning ? "Stop Bot" : "Start Bot"}
                </Button>
            </div>
        </div>
    );
};
