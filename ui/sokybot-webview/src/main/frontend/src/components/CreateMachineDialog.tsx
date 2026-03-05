import React, { useState, useEffect } from 'react';
import { rsocketService } from '../RSocketClient';
import type { GroupInfo, GroupDetails } from '../RSocketClient';
import { Button } from '@sokybot/frontend-shared';
import { cn } from '@sokybot/frontend-shared';
import { X, Server, Gamepad2, Settings2, User, ChevronRight, ChevronLeft } from 'lucide-react';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

const CreateMachineDialog: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
    const [currentStep, setCurrentStep] = useState(1);
    const [groups, setGroups] = useState<GroupInfo[]>([]);
    const [selectedGroup, setSelectedGroup] = useState('');
    const [gameData, setGameData] = useState<GroupDetails | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // UI Logic State
    const [selectedDivision, setSelectedDivision] = useState<string>('');

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
            setLoading(true);
            setError(null);
            // Fetch groups using new typed API
            rsocketService.getGroups()
                .then((groupList) => {
                    setGroups(groupList);
                    if (groupList.length > 0) {
                        setSelectedGroup(groupList[0].name);
                    } else {
                        setSelectedGroup('');
                    }
                })
                .catch(err => setError("Failed to load groups: " + err.message))
                .finally(() => setLoading(false));
        }
    }, [isOpen]);

    // Fetch Game Data when Group Changes
    useEffect(() => {
        if (selectedGroup) {
            setLoading(true);
            // Use new typed API
            rsocketService.getGroupDetails(selectedGroup)
                .then((data) => {
                    setGameData(data);

                    // Default Logic for Division/Host
                    if (data.hosts) {
                        const divs = Object.keys(data.hosts);
                        if (divs.length > 0) {
                            const firstDiv = divs[0];
                            setSelectedDivision(firstDiv);
                            const hosts = data.hosts[firstDiv];
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

    const inputClass = "flex h-8 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50";
    const labelClass = "text-xs font-medium leading-none text-foreground/80 block mb-1";

    return (
        <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in-0">
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
                                <select
                                    value={selectedGroup}
                                    onChange={(e) => setSelectedGroup(e.target.value)}
                                    disabled={loading}
                                    className={inputClass}
                                >
                                    {groups.map(g => (
                                        <option key={g.name} value={g.name}>{g.name}</option>
                                    ))}
                                </select>
                            </div>

                            {/* Game Data */}
                            <fieldset className="border border-border rounded-md p-3 relative bg-card/50">
                                <legend className="text-xs font-bold px-2 text-foreground/80 flex items-center gap-1">
                                    <Server className="h-3 w-3" />
                                    Game Data
                                </legend>
                                <div className="space-y-3 pt-1">
                                    <div>
                                        <label className={labelClass}>Division(s)</label>
                                        <select
                                            value={selectedDivision}
                                            onChange={(e) => handleDivisionChange(e.target.value)}
                                            disabled={!gameData || !gameData.hosts}
                                            className={inputClass}
                                        >
                                            {gameData && gameData.hosts && Object.keys(gameData.hosts).map(div => (
                                                <option key={div} value={div}>{div}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className={labelClass}>Host(s)</label>
                                        <select
                                            value={formData.host}
                                            onChange={(e) => setFormData({ ...formData, host: e.target.value })}
                                            disabled={!selectedDivision}
                                            className={inputClass}
                                        >
                                            {gameData && selectedDivision && gameData.hosts[selectedDivision]?.map((host) => (
                                                <option key={host} value={host}>{host}</option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="flex justify-end gap-4 pt-2 border-t border-dashed border-border/50 text-[10px] text-muted-foreground">
                                        <div><span className="font-bold">Version:</span> {gameData?.version !== undefined ? gameData.version : 'N/A'}</div>
                                        <div><span className="font-bold">Port:</span> 15779</div>
                                    </div>
                                </div>
                            </fieldset>
                        </div>
                    )}

                    {/* Step 2: Bot Data */}
                    {currentStep === 2 && (
                        <div className="space-y-4 animate-in fade-in-0 slide-in-from-right-2 duration-200">
                            <fieldset className="border border-border rounded-md p-3 relative bg-card/50">
                                <legend className="text-xs font-bold px-2 text-foreground/80 flex items-center gap-1">
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
                            <Button size="sm" onClick={handleNext} disabled={!canProceedToStep2() || loading}>
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
