import React, { useEffect, useState } from 'react';
import { rsocketService } from './RSocketClient';

interface CharacterState {
    entityId?: number;
    characterName: string;
    level: number;
    currentHP: number;
    maxHP: number;
    currentMP: number;
    maxMP: number;
    gold: number;
    x: number;
    y: number;
    xSector: number;
    isRunning?: boolean;
}

interface CharacterStatusProps {
    machineId?: string; // Optional machine ID to filter/request
}

export const CharacterStatus: React.FC<CharacterStatusProps> = ({ machineId }) => {
    const [state, setState] = useState<CharacterState | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Initial Fetch
    const fetchState = async () => {
        try {
            setLoading(true);
            const request = machineId ? `getCharacterState:${machineId}` : "getCharacterState";
            const response = await rsocketService.requestResponse(request);

            // Response might be JSON string
            const data = typeof response === 'string' ? JSON.parse(response) : response;
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
        const action = state.isRunning ? "stopBot" : "startBot";
        try {
            await rsocketService.requestResponse(`${action}:${machineId}`);
            // Optimistically update or refetch
            fetchState();
        } catch (err) {
            console.error(`Failed to ${action}`, err);
        }
    };

    // Event Subscription
    useEffect(() => {
        if (!state?.entityId) return; // Wait until we have entity ID to match updates

        const sub = rsocketService.streamEvents((event: any) => {
            if (!event || !event.eventType) return;

            // Handle EntityHPMPUpdateEvent
            if (event.eventType === 'EntityHPMPUpdateEvent') {
                // Check if it's for our character (by entityId)
                // Ensure event.entityId matches state.entityId
                if (event.entityId === state.entityId) {
                    setState(prev => {
                        if (!prev) return null;
                        const newState = { ...prev };
                        if (event.newHP !== undefined && event.newHP !== null) newState.currentHP = event.newHP;
                        if (event.newMP !== undefined && event.newMP !== null) newState.currentMP = event.newMP;
                        return newState;
                    });
                }
            }

            // Handle other updates
            if (event.eventType === 'CharacterLoadedEvent') {
                fetchState();
            }

        }, (err: any) => console.error("Stream error", err));

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
        <div className="bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 p-4 shadow-sm rounded-lg">
            <div className="flex flex-col md:flex-row gap-4 items-start md:items-center justify-between">

                {/* Character Info */}
                <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-500 dark:text-slate-400 border border-slate-200 dark:border-slate-700">
                        <span className="text-xs font-mono">AV</span>
                    </div>
                    <div>
                        <div className="flex items-baseline gap-2">
                            <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">{state.characterName}</h2>
                            <span className="text-sm text-slate-500 dark:text-slate-400">Lv.{state.level}</span>
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-500 font-mono">
                            ({state.x}, {state.y})
                        </div>
                    </div>
                </div>

                {/* Status Bars */}
                <div className="flex-1 w-full md:w-auto md:max-w-md space-y-2">
                    {/* HP */}
                    <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-500 dark:text-slate-400 w-6">HP</span>
                        <div className="flex-1 relative h-4 bg-slate-200 dark:bg-slate-800 rounded overflow-hidden">
                            <div
                                className="absolute top-0 left-0 h-full bg-red-500/80 transition-all duration-300"
                                style={{ width: `${hpPercent}%` }}
                            />
                            <div className="absolute inset-0 flex items-center justify-center text-[10px] text-slate-900 dark:text-white font-medium drop-shadow-md">
                                {state.currentHP} / {state.maxHP}
                            </div>
                        </div>
                    </div>

                    {/* MP */}
                    <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-500 dark:text-slate-400 w-6">MP</span>
                        <div className="flex-1 relative h-4 bg-slate-200 dark:bg-slate-800 rounded overflow-hidden">
                            <div
                                className="absolute top-0 left-0 h-full bg-blue-500/80 transition-all duration-300"
                                style={{ width: `${mpPercent}%` }}
                            />
                            <div className="absolute inset-0 flex items-center justify-center text-[10px] text-slate-900 dark:text-white font-medium drop-shadow-md">
                                {state.currentMP} / {state.maxMP}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Actions: Gold & Start Button */}
                <div className="flex flex-col items-end gap-2">
                    {/* Gold */}
                    <div className="bg-slate-100 dark:bg-slate-950 px-3 py-1.5 rounded border border-slate-200 dark:border-slate-800">
                        <span className="text-xs text-slate-500 dark:text-slate-400 uppercase tracking-wider mr-2">Gold</span>
                        <span className="text-sm font-mono text-yellow-600 dark:text-yellow-500">{state.gold.toLocaleString()}</span>
                    </div>

                    {/* Start/Stop Button */}
                    <button
                        onClick={handleToggleBot}
                        className={`px-4 py-1.5 rounded text-xs font-bold uppercase tracking-wider border transition-all shadow-sm
                            ${state.isRunning
                                ? "bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 border-red-200 dark:border-red-800 hover:bg-red-200 dark:hover:bg-red-900/50"
                                : "bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800 hover:bg-emerald-200 dark:hover:bg-emerald-900/50"
                            }`}
                    >
                        {state.isRunning ? "Stop Bot" : "Start Bot"}
                    </button>
                </div>

            </div>
        </div>
    );
};
