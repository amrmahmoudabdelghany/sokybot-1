import React, { useState, useEffect } from 'react';
import { rsocketService } from '../RSocketClient';
import type { GroupDetails } from '../RSocketClient';
import { useGroupsQuery } from '../query/sokybotQueries';
import {
    Button,
    cn,
    Select as SelectRoot,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    encodeSelectItemValue,
    decodeSelectItemValue,
} from '@sokybot/frontend-shared';
import { X, Server, Gamepad2, Settings2, User, ChevronRight, ChevronLeft } from 'lucide-react';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

const CreateMachineDialog: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
    const [currentStep, setCurrentStep] = useState(1);
    const { data: groups = [], isLoading: groupsLoading, isError: groupsError } = useGroupsQuery(isOpen);
    const [selectedGroup, setSelectedGroup] = useState('');
    const [gameData, setGameData] = useState<GroupDetails | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // UI Logic State
    const [selectedDivision, setSelectedDivision] = useState<string>('');
    const [isManual, setIsManual] = useState(false);

    const [formData, setFormData] = useState({
        name: '',
        type: 'Client', // Default legacy behavior
        host: '',
        autoLogin: false,
        username: '',
        password: '',
        passcode: '',
        agentServer: ''
    });

    useEffect(() => {
        if (isOpen) {
            setCurrentStep(1);
            setError(null);
        }
    }, [isOpen]);

    useEffect(() => {
        if (!isOpen) return;
        if (groups.length > 0) {
            setSelectedGroup((prev) => prev || groups[0].name);
        } else {
            setSelectedGroup('');
        }
    }, [isOpen, groups]);

    useEffect(() => {
        if (isOpen && groupsError) {
            setError('Failed to load groups');
        }
    }, [isOpen, groupsError]);

    // Fetch Game Data when Group Changes
    useEffect(() => {
        if (selectedGroup) {
            setLoading(true);
            // Use new typed API
            rsocketService.getGroupDetails(selectedGroup)
                .then((data) => {
                    setGameData(data);

                    // Reset to manual false unless the group explicitly overrides it
                    let manualMode = data.isManualOverride || false;
                    setIsManual(manualMode);

                    if (manualMode) {
                        setFormData(prev => ({
                            ...prev,
                            host: data.manualHost || "",
                            name: prev.name // preserve name
                        }));
                        setSelectedDivision(data.manualDivision || "");
                    } else if (data.hosts) {
                        const divisions = Object.keys(data.hosts);
                        if (divisions.length > 0) {
                            setSelectedDivision(divisions[0]);
                            const hosts = data.hosts[divisions[0]];
                            if (hosts && hosts.length > 0) {
                                setFormData(prev => ({ ...prev, host: hosts[0] }));
                            }
                        }
                    }
                })
                .catch(err => setError("Failed to load game data: " + err.message))
                .finally(() => setLoading(false));
        } else {
            setGameData(null);
        }
    }, [selectedGroup]);

    const handleDivisionChange = (newDiv: string) => {
        setSelectedDivision(newDiv);
        if (gameData && gameData.hosts[newDiv] && gameData.hosts[newDiv].length > 0) {
            setFormData(prev => ({ ...prev, host: gameData.hosts[newDiv][0] }));
        } else {
            setFormData(prev => ({ ...prev, host: '' }));
        }
    };

    const canProceedToStep2 = () => {
        return selectedGroup && selectedDivision && formData.host;
    };

    const handleNext = () => {
        if (currentStep === 1 && canProceedToStep2()) {
            setCurrentStep(2);
        }
    };

    const handlePrevious = () => {
        if (currentStep === 2) {
            setCurrentStep(1);
        }
    };

    const handleCreate = async () => {
        try {
            setLoading(true);
            setError(null);

            // Step 1: Create the bare machine context (Empty options since they were legacy)
            await rsocketService.createMachine(selectedGroup, formData.name, []);

            // Step 2: Initialize Settings dynamically if any are configured
            const payload: Record<string, unknown> = {};
            let needsInitialization = false;

            if (formData.host) {
                payload.targetGateway = formData.host;
                needsInitialization = true;
            }

            if (formData.autoLogin) {
                payload.autoLogin = formData.autoLogin;
                payload.username = formData.username;
                payload.password = formData.password;
                payload.passcode = formData.passcode;
                payload.targetAgent = formData.agentServer;
                needsInitialization = true;
            }

            if (needsInitialization) {
                await rsocketService.initializeMachine(selectedGroup, formData.name, "login", payload);
            }

            onSuccess();
            onClose();
        } catch (err: any) {
            setError("Creation failed: " + err.message);
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    const step1Blocked = loading || groupsLoading;

    const inputClass = "flex h-8 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50";
    const labelClass = "text-xs font-medium leading-none text-foreground block mb-1";

    return (
        <div className="fixed inset-0 z-50 bg-background/90 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in-0">
            <div className="w-full max-w-md bg-card border border-border rounded-lg shadow-lg flex flex-col animate-in zoom-in-95 duration-200">

                {/* Header */}
                <div className="flex items-center justify-between p-4 border-b border-border bg-muted/30 rounded-t-lg">
                    <div className="flex items-center gap-3">
                        <Settings2 className="h-5 w-5 text-primary" />
                        <div>
                            <h3 className="font-semibold text-lg">Machine Builder</h3>
                            <p className="text-xs text-muted-foreground">Step {currentStep} of 2</p>
                        </div>
                    </div>
                    <Button variant="ghost" size="icon" className="h-6 w-6" onClick={onClose}>
                        <X className="h-4 w-4" />
                    </Button>
                </div>

                {/* Step Indicator */}
                <div className="px-4 pt-4">
                    <div className="flex items-center gap-2">
                        <div className={cn(
                            "flex-1 h-1 rounded-full transition-colors",
                            currentStep >= 1 ? "bg-primary" : "bg-muted"
                        )}></div>
                        <div className={cn(
                            "flex-1 h-1 rounded-full transition-colors",
                            currentStep >= 2 ? "bg-primary" : "bg-muted"
                        )}></div>
                    </div>
                    <div className="flex justify-between mt-1 text-xs text-muted-foreground">
                        <span className={currentStep === 1 ? "text-primary font-medium" : ""}>Game Setup</span>
                        <span className={currentStep === 2 ? "text-primary font-medium" : ""}>Bot Config</span>
                    </div>
                </div>

                <div className="p-4 space-y-4 overflow-y-auto max-h-[60vh]">
                    {error && (
                        <div className="bg-destructive/15 text-destructive text-xs p-2 rounded border border-destructive/20 font-medium">
                            {error}
                        </div>
                    )}

                    {/* Step 1: Group + Game Data */}
                    {currentStep === 1 && (
                        <div className="space-y-4 animate-in fade-in-0 slide-in-from-right-2 duration-200">
                            {/* Group Selection */}
                            <div>
                                <label className={labelClass}>Group(s)</label>
                                {groups.length === 0 ? (
                                    <div className={cn(inputClass, 'flex items-center text-muted-foreground')}>
                                        No groups available
                                    </div>
                                ) : (
                                    <SelectRoot
                                        value={encodeSelectItemValue(selectedGroup)}
                                        onValueChange={(v) => setSelectedGroup(decodeSelectItemValue(v))}
                                        disabled={step1Blocked}
                                    >
                                        <SelectTrigger className={inputClass}>
                                            <SelectValue placeholder="Choose a group" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {groups.map((g) => (
                                                <SelectItem key={g.name} value={encodeSelectItemValue(g.name)}>
                                                    {g.name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </SelectRoot>
                                )}
                            </div>

                            {/* Game Data */}
                            <fieldset className="border border-border rounded-md p-3 relative bg-muted/25">
                                <legend className="text-xs font-bold px-2 text-foreground flex items-center gap-1">
                                    <Server className="h-3 w-3" />
                                    Game Data
                                </legend>
                                <div className="space-y-3 pt-1">
                                    <div className="flex items-center space-x-2 mb-3">
                                        <input
                                            type="checkbox"
                                            id="machine-manual-override"
                                            checked={isManual}
                                            onChange={(e) => setIsManual(e.target.checked)}
                                            className="h-3 w-3 rounded border-gray-300 text-primary focus:ring-primary"
                                        />
                                        <label htmlFor="machine-manual-override" className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider cursor-pointer">
                                            Manual Connection
                                        </label>
                                    </div>

                                    <div>
                                        <label className={labelClass}>Division Name</label>
                                        {isManual ? (
                                            <input
                                                type="text"
                                                value={selectedDivision}
                                                onChange={(e) => setSelectedDivision(e.target.value)}
                                                className={inputClass}
                                                placeholder="S_Official"
                                            />
                                        ) : (
                                            <SelectRoot
                                                value={encodeSelectItemValue(selectedDivision)}
                                                onValueChange={(v) => handleDivisionChange(decodeSelectItemValue(v))}
                                                disabled={!gameData || !gameData.hosts}
                                            >
                                                <SelectTrigger className={inputClass}>
                                                    <SelectValue placeholder="Division" />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    {gameData?.hosts &&
                                                        Object.keys(gameData.hosts).map((div) => (
                                                            <SelectItem key={div} value={encodeSelectItemValue(div)}>
                                                                {div}
                                                            </SelectItem>
                                                        ))}
                                                </SelectContent>
                                            </SelectRoot>
                                        )}
                                    </div>
                                    <div>
                                        <label className={labelClass}>Login Host (IP/DNS)</label>
                                        {isManual ? (
                                            <input
                                                type="text"
                                                value={formData.host}
                                                onChange={(e) => setFormData({ ...formData, host: e.target.value })}
                                                className={inputClass}
                                                placeholder="127.0.0.1"
                                            />
                                        ) : (
                                            <SelectRoot
                                                value={encodeSelectItemValue(formData.host)}
                                                onValueChange={(v) =>
                                                    setFormData({ ...formData, host: decodeSelectItemValue(v) })
                                                }
                                                disabled={!selectedDivision}
                                            >
                                                <SelectTrigger className={inputClass}>
                                                    <SelectValue placeholder="Host" />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    {gameData &&
                                                        selectedDivision &&
                                                        gameData.hosts[selectedDivision]?.map((host) => (
                                                            <SelectItem key={host} value={encodeSelectItemValue(host)}>
                                                                {host}
                                                            </SelectItem>
                                                        ))}
                                                </SelectContent>
                                            </SelectRoot>
                                        )}
                                    </div>

                                    <div className="flex justify-end gap-4 pt-2 border-t border-dashed border-border text-[10px] text-muted-foreground">
                                        <div><span className="font-bold">Version:</span> {gameData?.version !== undefined ? gameData.version : 'N/A'}</div>
                                        <div><span className="font-bold">Port:</span> {gameData?.port !== undefined && gameData.port > 0 ? gameData.port : 'N/A'}</div>
                                    </div>
                                </div>
                            </fieldset>
                        </div>
                    )}

                    {/* Step 2: Bot Data */}
                    {currentStep === 2 && (
                        <div className="space-y-4 animate-in fade-in-0 slide-in-from-right-2 duration-200">
                            <fieldset className="border border-border rounded-md p-3 relative bg-muted/25">
                                <legend className="text-xs font-bold px-2 text-foreground flex items-center gap-1">
                                    <Gamepad2 className="h-3 w-3" />
                                    Bot Data
                                </legend>
                                <div className="space-y-3 pt-1">
                                    <div>
                                        <label className={labelClass}>Type</label>
                                        <div className="flex gap-4 items-center">
                                            <label className="flex items-center gap-2 text-sm cursor-pointer">
                                                <input
                                                    type="radio"
                                                    name="type"
                                                    value="Client"
                                                    checked={formData.type === 'Client'}
                                                    onChange={() => setFormData({ ...formData, type: 'Client' })}
                                                    className="text-primary focus:ring-primary h-3 w-3"
                                                /> Client
                                            </label>
                                            <label className="flex items-center gap-2 text-sm text-muted-foreground cursor-not-allowed">
                                                <input
                                                    type="radio"
                                                    name="type"
                                                    value="Clientless"
                                                    disabled={true}
                                                    checked={formData.type === 'Clientless'}
                                                    className="text-muted h-3 w-3"
                                                /> Clientless
                                            </label>
                                        </div>
                                    </div>

                                    <div>
                                        <label className={labelClass}>Trainer</label>
                                        <input
                                            type="text"
                                            value={formData.name}
                                            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                            className={inputClass}
                                            placeholder="Character Name"
                                        />
                                    </div>

                                    <div>
                                        <label className="flex items-center gap-2 text-xs font-medium cursor-pointer select-none">
                                            <input
                                                type="checkbox"
                                                checked={formData.autoLogin}
                                                onChange={(e) => setFormData({ ...formData, autoLogin: e.target.checked })}
                                                className="h-3 w-3 rounded border-gray-300 text-primary focus:ring-primary"
                                            />
                                            Auto Login
                                        </label>
                                    </div>

                                    {formData.autoLogin && (
                                        <div className="space-y-2 p-3 border-l-2 border-border/50 ml-2 animate-in slide-in-from-top-2 duration-200">
                                            <div className="relative">
                                                <User className="absolute left-2 top-2 h-4 w-4 text-muted-foreground" />
                                                <input
                                                    type="text"
                                                    placeholder="Username"
                                                    value={formData.username}
                                                    onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                                                    className={cn(inputClass, "pl-8")}
                                                />
                                            </div>
                                            <input
                                                type="password"
                                                placeholder="Password"
                                                value={formData.password}
                                                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                                                className={inputClass}
                                            />
                                            <input
                                                type="password"
                                                placeholder="Passcode"
                                                value={formData.passcode}
                                                onChange={(e) => setFormData({ ...formData, passcode: e.target.value })}
                                                className={inputClass}
                                            />
                                            <input
                                                type="text"
                                                placeholder="Server"
                                                value={formData.agentServer}
                                                onChange={(e) => setFormData({ ...formData, agentServer: e.target.value })}
                                                className={inputClass}
                                            />
                                        </div>
                                    )}
                                </div>
                            </fieldset>
                        </div>
                    )}
                </div>

                {/* Footer with Navigation */}
                <div className="p-4 border-t border-border bg-muted/10 rounded-b-lg flex justify-between">
                    <div>
                        {currentStep > 1 && (
                            <Button variant="outline" size="sm" onClick={handlePrevious} disabled={loading}>
                                <ChevronLeft className="h-4 w-4 mr-1" />
                                Previous
                            </Button>
                        )}
                    </div>
                    <div className="flex gap-2">
                        <Button variant="outline" size="sm" onClick={onClose} disabled={loading}>Cancel</Button>
                        {currentStep === 1 ? (
                            <Button size="sm" onClick={handleNext} disabled={!canProceedToStep2() || step1Blocked}>
                                Next
                                <ChevronRight className="h-4 w-4 ml-1" />
                            </Button>
                        ) : (
                            <Button size="sm" onClick={handleCreate} disabled={loading || !formData.name} className="min-w-[80px]">
                                {loading ? 'Creating...' : 'Create'}
                            </Button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CreateMachineDialog;
