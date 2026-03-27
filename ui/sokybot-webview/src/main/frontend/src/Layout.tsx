import React, { useState } from 'react';
import { useSokybotStore } from './store';
import { useExtensionRegistry } from './extensions/ExtensionRegistry';
import { CharacterStatus } from './CharacterStatus';
import { rsocketService } from './RSocketClient';
import { useTheme } from './components/theme-provider';
import { CreateGroupDialog } from './components/CreateGroupDialog';
import CreateMachineDialog from './components/CreateMachineDialog';
import { ToolbarExtensions } from './components/ToolbarExtensions';
import { Plus, Bot, Monitor, Moon, Sun, Maximize, Minimize, Settings, Target, Package, Zap, Globe, FileText, Activity } from 'lucide-react';
import { Button } from '@sokybot/frontend-shared';
import { cn } from '@sokybot/frontend-shared';

interface LayoutProps {
    children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
    const {
        groups,
        machines,
        selectedMachineId,
        setSelectedMachineId,
        fetchInitialData
    } = useSokybotStore();

    const extensionRegistry = useExtensionRegistry();
    const { selectedPageId, setSelectedPageId } = useSokybotStore();

    // Helper to get machine simple name
    const getSimpleName = (machineId: string) => {
        return machineId.includes('.') ? machineId.split('.').pop() || machineId : machineId;
    };

    // Helper to get machine group
    const getMachineGroup = (machineId: string) => {
        return machineId.includes('.') ? machineId.split('.')[0] : '';
    };

    // Helper to get machine tabs (duplicated from MachineView for now)
    const getMachineTabs = (machineId: string) => {
        const machineTabs: string[] = [];

        Object.keys(extensionRegistry.pages || {}).forEach(pageId => {
            // Packet Analyzer is shown only as a modal from Packet Sniffer, not as a separate tab
            if (pageId.toLowerCase().startsWith('packetanalyzer_')) return;

            // Strict match: pageId must end with exactly _machineId
            if (pageId === machineId ||
                pageId.endsWith("_" + machineId)) {
                machineTabs.push(pageId);
            }
        });

        const corePageOrder = ['inventory', 'skills', 'training', 'environment', 'log'];
        machineTabs.sort((a, b) => {
            const aBase = a.split('_')[0].toLowerCase();
            const bBase = b.split('_')[0].toLowerCase();
            const aIndex = corePageOrder.indexOf(aBase);
            const bIndex = corePageOrder.indexOf(bBase);
            if (aIndex !== -1 && bIndex !== -1) return aIndex - bIndex;
            if (aIndex !== -1) return -1;
            if (bIndex !== -1) return 1;
            return a.localeCompare(b);
        });

        return machineTabs;
    };

    const { theme, setTheme } = useTheme();
    const [isCreateGroupOpen, setCreateGroupOpen] = useState(false);
    const [isCreateMachineOpen, setCreateMachineOpen] = useState(false);
    const [isFullscreen, setIsFullscreen] = useState(false);
    const [connectInFlightByMachine, setConnectInFlightByMachine] = useState<Record<string, boolean>>({});
    const [machineStatusById, setMachineStatusById] = useState<Record<string, {
        loginPhase: string;
        connected: boolean;
        authenticated: boolean;
        inGame: boolean;
        agentOptions: Array<{ value: string; label: string }>;
        availableCharacters: string[];
        selectedCharacter: string | null;
    }>>({});
    const [onboardingFormByMachine, setOnboardingFormByMachine] = useState<Record<string, {
        targetGateway: string;
        username: string;
        password: string;
        passcode: string;
        targetAgent: string;
        selectedCharacter: string;
    }>>({});
    const currentMachineStatus = selectedMachineId
        ? (machineStatusById[selectedMachineId] || {
            loginPhase: "DISCONNECTED",
            connected: false,
            authenticated: false,
            inGame: false,
            agentOptions: [],
            availableCharacters: [],
            selectedCharacter: null
        })
        : {
            loginPhase: "DISCONNECTED",
            connected: false,
            authenticated: false,
            inGame: false,
            agentOptions: [],
            availableCharacters: [],
            selectedCharacter: null
        };

    const currentOnboardingForm = selectedMachineId
        ? (onboardingFormByMachine[selectedMachineId] || {
            targetGateway: "",
            username: "",
            password: "",
            passcode: "",
            targetAgent: "",
            selectedCharacter: ""
        })
        : {
            targetGateway: "",
            username: "",
            password: "",
            passcode: "",
            targetAgent: "",
            selectedCharacter: ""
        };
    const showConnectCard = currentMachineStatus.loginPhase === "MISSING_GATEWAY"
        || currentMachineStatus.loginPhase === "DISCONNECTED"
        || currentMachineStatus.loginPhase === "CONNECTING_GATEWAY";

    const toggleFullscreen = () => {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
            setIsFullscreen(true);
        } else {
            document.exitFullscreen();
            setIsFullscreen(false);
        }
    };


    // Normalize machines once so rendering stays stable even if backend sends duplicate rows.
    const uniqueMachines = React.useMemo(() => {
        const byId = new Map<string, typeof machines[number]>();
        machines.forEach((machine) => {
            if (!machine?.machineId) return;
            const existing = byId.get(machine.machineId);
            byId.set(machine.machineId, {
                ...(existing || {}),
                ...machine,
                groupName: machine.groupName || existing?.groupName
            });
        });
        return Array.from(byId.values());
    }, [machines]);

    // Get group names for display
    const groupNames = groups.map(g => g.name);

    // Helper to get icon for a page
    const getPageIcon = (pageId: string) => {
        const base = pageId.split('_')[0].toLowerCase();
        if (base === 'training') return <Target className="h-3 w-3 mr-2" />;
        if (base === 'inventory' || base === 'items') return <Package className="h-3 w-3 mr-2" />;
        if (base === 'skills') return <Zap className="h-3 w-3 mr-2" />;
        if (base === 'environment') return <Globe className="h-3 w-3 mr-2" />;
        if (base === 'log') return <FileText className="h-3 w-3 mr-2" />;
        if (base === 'packetsniffer' || base === 'activity') return <Activity className="h-3 w-3 mr-2" />;
        return <div className="w-1.5 h-1.5 rounded-full bg-slate-300 dark:bg-slate-700 mr-2" />;
    };

    const hasSystemLog = !!extensionRegistry.pages?.['SystemLog'];

    const parseMachineParts = React.useCallback((machineId: string) => {
        const parts = machineId.split('.', 2);
        if (parts.length < 2) return null;
        return { group: parts[0], name: parts[1] };
    }, []);

    const refreshMachineStatusSnapshot = React.useCallback(async (machineId: string) => {
        try {
            const data = await rsocketService.getCharacterState(machineId);
            setMachineStatusById((prev) => ({
                ...prev,
                [machineId]: {
                    loginPhase: data?.loginPhase || prev[machineId]?.loginPhase || "DISCONNECTED",
                    connected: Boolean(data?.connected),
                    authenticated: Boolean(data?.authenticated),
                    inGame: Boolean(data?.inGame),
                    agentOptions: Array.isArray(data?.agentOptions) ? data.agentOptions : (prev[machineId]?.agentOptions || []),
                    availableCharacters: Array.isArray(data?.availableCharacters) ? data.availableCharacters : (prev[machineId]?.availableCharacters || []),
                    selectedCharacter: data?.selectedCharacter ?? prev[machineId]?.selectedCharacter ?? null
                }
            }));
            const saved = data?.savedLogin;
            const savedGateway = saved && typeof saved.targetGateway === "string" ? saved.targetGateway : "";
            const savedAgent = saved && typeof saved.targetAgent === "string" ? saved.targetAgent : "";
            setOnboardingFormByMachine((prev) => ({
                ...prev,
                [machineId]: {
                    targetGateway: prev[machineId]?.targetGateway || savedGateway,
                    username: prev[machineId]?.username || "",
                    password: prev[machineId]?.password || "",
                    passcode: prev[machineId]?.passcode || "",
                    targetAgent: prev[machineId]?.targetAgent || savedAgent || String(data?.agentOptions?.[0]?.value || ""),
                    selectedCharacter: prev[machineId]?.selectedCharacter || String(data?.selectedCharacter || data?.availableCharacters?.[0] || "")
                }
            }));
        } catch (err) {
            console.error("Failed to refresh machine status snapshot", err);
        }
    }, []);

    React.useEffect(() => {
        if (!selectedMachineId) return;
        void refreshMachineStatusSnapshot(selectedMachineId);
        const sub = rsocketService.subscribeToMachineStatus(
            selectedMachineId,
            (statusEvent) => {
                if (!statusEvent || statusEvent.machineId !== selectedMachineId) return;
                setMachineStatusById((prev) => ({
                    ...prev,
                    [selectedMachineId]: {
                        loginPhase: statusEvent.loginPhase !== undefined && statusEvent.loginPhase !== ""
                            ? statusEvent.loginPhase
                            : (prev[selectedMachineId]?.loginPhase || "DISCONNECTED"),
                        connected: statusEvent.connected !== undefined
                            ? Boolean(statusEvent.connected)
                            : Boolean(prev[selectedMachineId]?.connected),
                        authenticated: statusEvent.authenticated !== undefined
                            ? Boolean(statusEvent.authenticated)
                            : Boolean(prev[selectedMachineId]?.authenticated),
                        inGame: prev[selectedMachineId]?.inGame || false,
                        agentOptions: prev[selectedMachineId]?.agentOptions || [],
                        availableCharacters: prev[selectedMachineId]?.availableCharacters || [],
                        selectedCharacter: prev[selectedMachineId]?.selectedCharacter ?? null
                    }
                }));
                void refreshMachineStatusSnapshot(selectedMachineId);
            },
            (err) => console.error("Layout machine status stream error", err)
        );
        const intervalId = window.setInterval(() => {
            void refreshMachineStatusSnapshot(selectedMachineId);
        }, 4000);
        return () => {
            window.clearInterval(intervalId);
            if (sub && typeof sub.unsubscribe === 'function') {
                sub.unsubscribe();
            }
        };
    }, [selectedMachineId, refreshMachineStatusSnapshot]);

    const updateOnboardingForm = (machineId: string, patch: Partial<{
        targetGateway: string;
        username: string;
        password: string;
        passcode: string;
        targetAgent: string;
        selectedCharacter: string;
    }>) => {
        setOnboardingFormByMachine((prev) => ({
            ...prev,
            [machineId]: {
                targetGateway: prev[machineId]?.targetGateway || "",
                username: prev[machineId]?.username || "",
                password: prev[machineId]?.password || "",
                passcode: prev[machineId]?.passcode || "",
                targetAgent: prev[machineId]?.targetAgent || "",
                selectedCharacter: prev[machineId]?.selectedCharacter || "",
                ...patch
            }
        }));
    };

    const saveLoginPayload = async (machineId: string, payload: Record<string, unknown>, startAfterSave?: boolean) => {
        const parts = parseMachineParts(machineId);
        if (!parts) return;
        if (startAfterSave) {
            setConnectInFlightByMachine((prev) => ({ ...prev, [machineId]: true }));
        }
        try {
            await rsocketService.initializeMachine(parts.group, parts.name, "login", payload);
            if (startAfterSave) {
                await rsocketService.startBot(machineId);
            }
            await refreshMachineStatusSnapshot(machineId);
        } finally {
            if (startAfterSave) {
                setConnectInFlightByMachine((prev) => ({ ...prev, [machineId]: false }));
            }
        }
    };

    return (
        <div className="flex h-screen bg-background text-foreground font-sans transition-colors duration-300 overflow-hidden">
            {/* Sidebar */}
            <div className="w-64 border-r border-border/40 bg-card/80 backdrop-blur-md flex flex-col transition-all duration-300 z-20">
                <div className="h-16 px-6 border-b border-border/40 flex items-center justify-between bg-card/50">
                    <div className="font-bold text-lg text-primary font-mono flex items-center gap-3 tracking-tight">
                        <div className="p-1.5 rounded-md bg-primary/10">
                            <Monitor className="h-5 w-5" />
                        </div>
                        Sokybot v2
                    </div>
                </div>

                {/* Actions */}
                <div className="p-4 grid grid-cols-2 gap-3 border-b border-border/40">
                    <Button variant="outline" size="sm" onClick={() => setCreateGroupOpen(true)} className="h-9 shadow-sm hover:shadow transition-all hover:bg-primary/5 hover:text-primary hover:border-primary/20">
                        <Plus className="h-3.5 w-3.5 mr-1.5" /> Group
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setCreateMachineOpen(true)} className="h-9 shadow-sm hover:shadow transition-all hover:bg-primary/5 hover:text-primary hover:border-primary/20">
                        <Plus className="h-3.5 w-3.5 mr-1.5" /> Bot
                    </Button>
                </div>

                <div className="flex-1 overflow-y-auto p-3 space-y-6 scrollbar-thin scrollbar-thumb-border/40 scrollbar-track-transparent">
                    {groupNames.map(group => {
                        const groupMachines = uniqueMachines.filter(m =>
                            (m.groupName || getMachineGroup(m.machineId)) === group
                        );
                        const groupLogPageId = `GroupLog_${group}`;
                        const hasGroupLog = !!extensionRegistry.pages?.[groupLogPageId];
                        return (
                            <div key={group} className="space-y-1">
                                <div className="px-3 text-[10px] font-bold text-muted-foreground/70 uppercase tracking-widest flex items-center justify-between mb-2">
                                    <span>{group}</span>
                                    <div className="flex items-center gap-1">
                                        {hasGroupLog && (
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                className="h-6 w-6 text-muted-foreground hover:text-primary"
                                                title="Group Log"
                                                onClick={() => {
                                                    setSelectedMachineId(null);
                                                    setSelectedPageId(groupLogPageId);
                                                }}
                                            >
                                                <FileText className="h-3 w-3" />
                                            </Button>
                                        )}
                                        <span className="text-[9px] bg-secondary/80 px-1.5 py-0.5 rounded-full text-foreground/80 font-mono shadow-sm">{groupMachines.length}</span>
                                    </div>
                                </div>
                                {groupMachines.map(m => (
                                    /* Machine Item */
                                    <div key={m.machineId} className="space-y-1">
                                        <Button
                                            key={m.machineId}
                                            variant={selectedMachineId === m.machineId ? "secondary" : "ghost"}
                                            className={cn(
                                                "w-full justify-start font-normal pl-3 h-9 transition-all duration-200 group relative overflow-hidden",
                                                selectedMachineId === m.machineId
                                                    ? "font-medium bg-primary/10 text-primary hover:bg-primary/15"
                                                    : "text-muted-foreground hover:text-foreground hover:bg-secondary/40"
                                            )}
                                            onClick={() => {
                                                setSelectedMachineId(m.machineId);
                                            }}
                                        >
                                            <Bot className={cn("h-4 w-4 mr-2.5 transition-colors",
                                                m.isRunning ? "text-emerald-500" : "text-muted-foreground/60 group-hover:text-muted-foreground"
                                            )} />
                                            <span className="truncate">{m.name || getSimpleName(m.machineId)}</span>
                                            {selectedMachineId === m.machineId && (
                                                <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-primary animate-in fade-in slide-in-from-left-1 duration-300" />
                                            )}
                                        </Button>

                                        {/* Nested Pages (Tree View) */}
                                        {selectedMachineId === m.machineId && (
                                            <div className="ml-5 pl-3 border-l border-border/30 space-y-0.5 my-1 animate-in slide-in-from-top-2 duration-200 fade-in-0">
                                                {getMachineTabs(m.machineId).length > 0 ? (
                                                    getMachineTabs(m.machineId).map(tabId => {
                                                        const page = extensionRegistry.pages[tabId];
                                                        const isActive = selectedPageId === tabId;
                                                        return (
                                                            <Button
                                                                key={tabId}
                                                                variant="ghost"
                                                                size="sm"
                                                                className={cn(
                                                                    "w-full justify-start h-8 text-xs font-normal transition-colors",
                                                                    isActive
                                                                        ? "text-primary bg-primary/5 font-medium"
                                                                        : "text-muted-foreground/80 hover:text-foreground hover:bg-secondary/30"
                                                                )}
                                                                onClick={() => setSelectedPageId(tabId)}
                                                            >
                                                                <div className="flex items-center">
                                                                    {getPageIcon(tabId)}
                                                                    {page?.title || tabId}
                                                                </div>
                                                            </Button>
                                                        );
                                                    })
                                                ) : (
                                                    <div className="text-[10px] text-muted-foreground/50 italic pl-2 py-1.5">
                                                        No pages registered
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        );
                    })}

                    {/* Orphaned machines or if no groups defined yet (legacy fallback) */}
                    {groupNames.length === 0 && uniqueMachines.length > 0 && (
                        <div className="space-y-1">
                            <div className="px-3 text-[10px] font-bold text-muted-foreground/70 uppercase tracking-widest mb-2">Default</div>
                            {uniqueMachines.map(m => (
                                <div key={m.machineId} className="space-y-1">
                                    <Button
                                        variant={selectedMachineId === m.machineId ? "secondary" : "ghost"}
                                        className={cn(
                                            "w-full justify-start font-normal pl-3 h-9 transition-all duration-200 group relative",
                                            selectedMachineId === m.machineId
                                                ? "font-medium bg-primary/10 text-primary hover:bg-primary/15"
                                                : "text-muted-foreground hover:text-foreground hover:bg-secondary/40"
                                        )}
                                        onClick={() => setSelectedMachineId(m.machineId)}
                                    >
                                        <Bot className={cn("h-4 w-4 mr-2.5 transition-colors",
                                            m.isRunning ? "text-emerald-500" : "text-muted-foreground/60 group-hover:text-muted-foreground"
                                        )} />
                                        <span className="truncate">{m.name || m.machineId}</span>
                                        {selectedMachineId === m.machineId && (
                                            <div className="absolute left-0 top-0 bottom-0 w-0.5 bg-primary rounded-r-full" />
                                        )}
                                    </Button>

                                    {/* Nested Pages (Tree View) */}
                                    {selectedMachineId === m.machineId && (
                                        <div className="ml-5 pl-3 border-l border-border/30 space-y-0.5 my-1 animate-in slide-in-from-top-2 duration-200 fade-in-0">
                                            {getMachineTabs(m.machineId).length > 0 ? (
                                                getMachineTabs(m.machineId).map(tabId => {
                                                    const page = extensionRegistry.pages[tabId];
                                                    const isActive = selectedPageId === tabId;
                                                    return (
                                                        <Button
                                                            key={tabId}
                                                            variant="ghost"
                                                            size="sm"
                                                            className={cn(
                                                                "w-full justify-start h-8 text-xs font-normal transition-colors",
                                                                isActive
                                                                    ? "text-primary bg-primary/5 font-medium"
                                                                    : "text-muted-foreground/80 hover:text-foreground hover:bg-secondary/30"
                                                            )}
                                                            onClick={() => setSelectedPageId(tabId)}
                                                        >
                                                            <div className="flex items-center">
                                                                {getPageIcon(tabId)}
                                                                {page?.title || tabId}
                                                            </div>
                                                        </Button>
                                                    );
                                                })
                                            ) : (
                                                <div className="text-[10px] text-muted-foreground/50 italic pl-2 py-1.5">
                                                    No pages registered
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}

                    {groupNames.length === 0 && uniqueMachines.length === 0 && (
                        <div className="text-muted-foreground text-xs italic p-8 text-center border-2 border-dashed border-border/30 rounded-lg mx-2 bg-secondary/10">
                            No groups or bots found.<br />Create a group to start.
                        </div>
                    )}
                </div>
                <div className="p-4 border-t border-border/40 text-[10px] font-medium text-muted-foreground/60 flex justify-between items-center bg-card/30">
                    <span>{uniqueMachines.length} Total Bots</span>
                    <div className="flex items-center gap-2">
                        {hasSystemLog && (
                            <Button
                                variant="ghost"
                                size="sm"
                                className="h-6 px-2 text-[10px] font-medium text-muted-foreground hover:text-primary hover:bg-secondary/40"
                                onClick={() => {
                                    setSelectedMachineId(null);
                                    setSelectedPageId('SystemLog');
                                }}
                            >
                                <FileText className="h-3 w-3 mr-1" />
                                System Log
                            </Button>
                        )}
                        <span className="w-2 h-2 rounded-full bg-emerald-500/20 ring-1 ring-emerald-500/40"></span>
                    </div>
                </div>
            </div>

            <CreateGroupDialog
                isOpen={isCreateGroupOpen}
                onClose={() => setCreateGroupOpen(false)}
                onCreated={fetchInitialData}
            />
            <CreateMachineDialog
                isOpen={isCreateMachineOpen}
                onClose={() => setCreateMachineOpen(false)}
                onSuccess={fetchInitialData}
            />

            {/* Main Content Area */}
            <div className="flex-1 flex flex-col min-w-0 bg-background/50 relative">
                {/* Decorative background gradients */}
                <div className="absolute inset-0 bg-gradient-to-tr from-primary/5 via-transparent to-transparent pointer-events-none opacity-50" />

                {/* Header */}
                <div className="h-16 px-6 border-b border-border/40 bg-card/80 backdrop-blur-md shadow-sm flex justify-between items-center z-10 transition-colors duration-300">
                    <div className="font-semibold text-lg flex items-center gap-3">
                        {selectedMachineId ? (
                            <div className="flex items-center animate-in fade-in slide-in-from-left-2 duration-300">
                                <div className="p-1.5 bg-primary/10 rounded-md mr-3 text-primary">
                                    <Bot className="h-5 w-5" />
                                </div>
                                <div className="flex flex-col leading-none">
                                    <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider mb-0.5">Active Machine</span>
                                    <span className="text-base text-foreground font-medium">{getSimpleName(selectedMachineId)}</span>
                                </div>
                            </div>
                        ) : (
                            <span className="text-muted-foreground flex items-center gap-2">
                                <Target className="h-4 w-4" /> Select a Machine
                            </span>
                        )}
                    </div>
                    <div className="flex items-center gap-1.5">
                        {/* Extension Toolbar Actions */}
                        <div className="mr-2">
                            <ToolbarExtensions />
                        </div>

                        <div className="h-6 w-px bg-border/40 mx-2" />

                        <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-foreground" onClick={() => {
                            if (theme === 'light') setTheme('dark');
                            else if (theme === 'dark') setTheme('desktop');
                            else if (theme === 'desktop') setTheme('desktop-dark');
                            else setTheme('light');
                        }} title={`Theme: ${theme}`}>
                            {theme === 'dark' ? <Moon className="h-4 w-4 transition-all" /> :
                                theme === 'desktop' ? <Monitor className="h-4 w-4 transition-all" /> :
                                    theme === 'desktop-dark' ? <Monitor className="h-4 w-4 text-primary transition-all" /> :
                                        <Sun className="h-4 w-4 transition-all" />}
                        </Button>

                        <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-foreground" onClick={toggleFullscreen}>
                            {isFullscreen ? <Minimize className="h-4 w-4" /> : <Maximize className="h-4 w-4" />}
                        </Button>
                        <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-foreground">
                            <Settings className="h-4 w-4" />
                        </Button>
                    </div>
                </div>

                <div className="flex-1 overflow-auto transition-colors duration-300 z-0 relative">
                    <div className="h-full">
                        {children}
                    </div>
                </div>
            </div>

            {/* Right Sidebar (Dashboard) */}
            {selectedMachineId && (
                <div className="w-80 border-l border-border/40 bg-card/80 backdrop-blur-md flex flex-col transition-all duration-300 z-20 shadow-[-4px_0_20px_rgba(0,0,0,0.02)]">
                    <div className="h-20 px-4 py-2 border-b border-border/40 bg-card/60">
                        <div className="w-full h-full flex flex-col justify-between">
                            <div className="w-full flex items-center justify-between text-[10px]">
                                <span className="font-bold uppercase tracking-widest text-muted-foreground">Status</span>
                                <span className="font-mono text-foreground/90">{currentMachineStatus.loginPhase}</span>
                            </div>
                            <div className="w-full grid grid-cols-2 gap-2">
                                <span className={cn(
                                    "inline-flex items-center justify-center gap-1.5 rounded-md px-2 py-1 text-[10px] font-semibold border",
                                    currentMachineStatus.connected
                                        ? "bg-emerald-600/15 text-emerald-400 border-emerald-500/30"
                                        : "bg-secondary/70 text-secondary-foreground border-border"
                                )}>
                                    <span className={cn(
                                        "h-1.5 w-1.5 rounded-full",
                                        currentMachineStatus.connected ? "bg-emerald-400" : "bg-muted-foreground/60"
                                    )} />
                                    <span>Connection</span>
                                    <span className="font-mono">{currentMachineStatus.connected ? "ON" : "OFF"}</span>
                                </span>
                                <span className={cn(
                                    "inline-flex items-center justify-center gap-1.5 rounded-md px-2 py-1 text-[10px] font-semibold border",
                                    currentMachineStatus.authenticated
                                        ? "bg-primary/15 text-primary border-primary/30"
                                        : "bg-secondary/70 text-secondary-foreground border-border"
                                )}>
                                    <span className={cn(
                                        "h-1.5 w-1.5 rounded-full",
                                        currentMachineStatus.authenticated ? "bg-primary" : "bg-muted-foreground/60"
                                    )} />
                                    <span>Auth</span>
                                    <span className="font-mono">{currentMachineStatus.authenticated ? "OK" : "WAIT"}</span>
                                </span>
                            </div>
                        </div>
                    </div>
                    <div className="flex-1 overflow-y-auto p-4">
                        <div className="space-y-4">
                            {showConnectCard && (
                                <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
                                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Connect</div>
                                    <input
                                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                                        placeholder="Target Gateway (IP or host)"
                                        value={currentOnboardingForm.targetGateway}
                                        onChange={(e) => selectedMachineId && updateOnboardingForm(selectedMachineId, { targetGateway: e.target.value })}
                                    />
                                    <Button
                                        className="w-full"
                                        disabled={Boolean(selectedMachineId && connectInFlightByMachine[selectedMachineId])}
                                        onClick={async () => {
                                            if (!selectedMachineId || !currentOnboardingForm.targetGateway.trim()) return;
                                            await saveLoginPayload(selectedMachineId, {
                                                targetGateway: currentOnboardingForm.targetGateway.trim(),
                                                autoLogin: true
                                            }, true);
                                        }}
                                    >
                                        {selectedMachineId && connectInFlightByMachine[selectedMachineId] ? "Connecting..." : "Connect"}
                                    </Button>
                                </div>
                            )}

                            {currentMachineStatus.loginPhase === "MISSING_AGENT_SERVER" && (
                                <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
                                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Agent Server</div>
                                    <select
                                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                                        value={currentOnboardingForm.targetAgent}
                                        onChange={(e) => selectedMachineId && updateOnboardingForm(selectedMachineId, { targetAgent: e.target.value })}
                                    >
                                        <option value="">Select agent server</option>
                                        {currentMachineStatus.agentOptions.map((option) => (
                                            <option key={option.value} value={option.value}>{option.label}</option>
                                        ))}
                                    </select>
                                    <Button
                                        className="w-full"
                                        onClick={async () => {
                                            if (!selectedMachineId || !currentOnboardingForm.targetAgent) return;
                                            await saveLoginPayload(selectedMachineId, { targetAgent: currentOnboardingForm.targetAgent }, false);
                                        }}
                                    >
                                        Save Agent Server
                                    </Button>
                                </div>
                            )}

                            {currentMachineStatus.loginPhase === "MISSING_CREDENTIALS" && (
                                <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
                                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Authentication</div>
                                    <input
                                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                                        placeholder="Username"
                                        value={currentOnboardingForm.username}
                                        onChange={(e) => selectedMachineId && updateOnboardingForm(selectedMachineId, { username: e.target.value })}
                                    />
                                    <input
                                        type="password"
                                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                                        placeholder="Password"
                                        value={currentOnboardingForm.password}
                                        onChange={(e) => selectedMachineId && updateOnboardingForm(selectedMachineId, { password: e.target.value })}
                                    />
                                    <input
                                        type="password"
                                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                                        placeholder="Passcode"
                                        value={currentOnboardingForm.passcode}
                                        onChange={(e) => selectedMachineId && updateOnboardingForm(selectedMachineId, { passcode: e.target.value })}
                                    />
                                    <Button
                                        className="w-full"
                                        onClick={async () => {
                                            if (!selectedMachineId) return;
                                            await saveLoginPayload(selectedMachineId, {
                                                username: currentOnboardingForm.username,
                                                password: currentOnboardingForm.password,
                                                passcode: currentOnboardingForm.passcode,
                                                autoLogin: true
                                            }, false);
                                        }}
                                    >
                                        Save Credentials
                                    </Button>
                                </div>
                            )}

                            {currentMachineStatus.loginPhase === "MISSING_CHARACTER_SELECTION" && (
                                <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
                                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Character List</div>
                                    <select
                                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                                        value={currentOnboardingForm.selectedCharacter}
                                        onChange={(e) => selectedMachineId && updateOnboardingForm(selectedMachineId, { selectedCharacter: e.target.value })}
                                    >
                                        <option value="">Select character</option>
                                        {currentMachineStatus.availableCharacters.map((name) => (
                                            <option key={name} value={name}>{name}</option>
                                        ))}
                                    </select>
                                    <Button
                                        className="w-full"
                                        onClick={async () => {
                                            if (!selectedMachineId || !currentOnboardingForm.selectedCharacter) return;
                                            await saveLoginPayload(selectedMachineId, {
                                                selectedCharacter: currentOnboardingForm.selectedCharacter
                                            }, false);
                                        }}
                                    >
                                        Save Character
                                    </Button>
                                </div>
                            )}

                            {currentMachineStatus.inGame && (
                                <CharacterStatus
                                    machineId={selectedMachineId}
                                />
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
