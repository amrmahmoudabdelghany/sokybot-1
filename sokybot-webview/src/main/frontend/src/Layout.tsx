import React, { useEffect, useState } from 'react';
import { rsocketService } from './RSocketClient';
import { CharacterStatus } from './CharacterStatus';
import { useTheme } from './components/theme-provider';
import { CreateGroupDialog } from './components/CreateGroupDialog';
import { CreateMachineDialog } from './components/CreateMachineDialog';
import { ToolbarExtensions } from './components/ToolbarExtensions';
import { Plus, Bot, Monitor, Moon, Sun, Maximize, Minimize } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface LayoutProps {
    children: React.ReactNode;
    selectedMachine?: string;
    onMachineSelect: (machine: string) => void;
}

export const Layout: React.FC<LayoutProps> = ({ children, selectedMachine, onMachineSelect }) => {
    const [machines, setMachines] = useState<string[]>([]);
    const { theme, setTheme } = useTheme();
    const [isCreateGroupOpen, setCreateGroupOpen] = useState(false);
    const [isCreateMachineOpen, setCreateMachineOpen] = useState(false);
    const [isFullscreen, setIsFullscreen] = useState(false);

    const fetchMachines = async () => {
        try {
            const response = await rsocketService.requestResponse("getMachines");
            const data = typeof response === 'string' ? JSON.parse(response) : response;
            if (Array.isArray(data)) {
                setMachines(data);
                // Select first machine if none selected
                // if (!selectedMachine && data.length > 0) {
                //     onMachineSelect(data[0]);
                // }
            }
        } catch (err) {
            console.error("Failed to fetch machines", err);
        }
    };

    useEffect(() => {
        fetchMachines();
    }, []);

    const toggleFullscreen = () => {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
            setIsFullscreen(true);
        } else {
            document.exitFullscreen();
            setIsFullscreen(false);
        }
    };

    return (
        <div className="flex h-screen bg-background text-foreground font-sans transition-colors duration-300">
            {/* Sidebar */}
            <div className="w-64 border-r bg-card flex flex-col transition-colors duration-300">
                <div className="p-4 border-b flex items-center justify-between">
                    <div className="font-bold text-lg text-primary font-mono flex items-center gap-2">
                        <Monitor className="h-5 w-5" />
                        Sokybot v2
                    </div>
                </div>

                {/* Actions */}
                <div className="p-2 grid grid-cols-2 gap-2 border-b">
                    <Button variant="outline" size="sm" onClick={() => setCreateGroupOpen(true)} className="h-8">
                        <Plus className="h-3 w-3 mr-1" /> Group
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setCreateMachineOpen(true)} className="h-8">
                        <Plus className="h-3 w-3 mr-1" /> Bot
                    </Button>
                </div>

                <div className="flex-1 overflow-y-auto p-2 space-y-1">
                    {machines.map(m => (
                        <Button
                            key={m}
                            variant={selectedMachine === m ? "secondary" : "ghost"}
                            className={cn("w-full justify-start font-normal", selectedMachine === m && "font-medium")}
                            onClick={() => onMachineSelect(m)}
                        >
                            <Bot className="h-4 w-4 mr-2 text-muted-foreground" />
                            {m}
                        </Button>
                    ))}
                    {machines.length === 0 && (
                        <div className="text-muted-foreground text-xs italic p-4 text-center">No machines found</div>
                    )}
                </div>
                <div className="p-4 border-t text-xs text-muted-foreground">
                    {machines.length} Machines Active
                </div>
            </div>

            <CreateGroupDialog
                isOpen={isCreateGroupOpen}
                onClose={() => setCreateGroupOpen(false)}
                onCreated={fetchMachines}
            />
            <CreateMachineDialog
                isOpen={isCreateMachineOpen}
                onClose={() => setCreateMachineOpen(false)}
                onCreated={fetchMachines}
            />

            {/* Main Content */}
            <div className="flex-1 flex flex-col overflow-hidden">
                {/* Header */}
                <div className="bg-card border-b p-4 shadow-sm flex justify-between items-center h-16 transition-colors duration-300">
                    <div className="font-semibold text-lg flex items-center gap-2">
                        {selectedMachine ? (
                            <>
                                <Bot className="h-5 w-5 text-primary" />
                                {selectedMachine}
                            </>
                        ) : "Select a Machine"}
                    </div>
                    <div className="flex items-center gap-2">
                        {/* Extension Toolbar Actions */}
                        <ToolbarExtensions />

                        <Button variant="ghost" size="icon" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>
                            {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                        </Button>

                        <Button variant="ghost" size="icon" onClick={toggleFullscreen}>
                            {isFullscreen ? <Minimize className="h-4 w-4" /> : <Maximize className="h-4 w-4" />}
                        </Button>
                    </div>
                </div>

                <div className="flex-1 overflow-auto p-6 bg-secondary/20 transition-colors duration-300">
                    {/* Machine Specific Header (Character Status) */}
                    {selectedMachine && (
                        <div className="mb-6">
                            <CharacterStatus machineId={selectedMachine} />
                        </div>
                    )}

                    {children}
                </div>
            </div>
        </div>
    );
};
