import React, { useState, useEffect } from 'react';
import { rsocketService } from '../RSocketClient';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

interface CreateMachineDialogProps {
    isOpen: boolean;
    onClose: () => void;
    onCreated: () => void;
}

export const CreateMachineDialog: React.FC<CreateMachineDialogProps> = ({ isOpen, onClose, onCreated }) => {
    const [groups, setGroups] = useState<string[]>([]);
    const [selectedGroup, setSelectedGroup] = useState('');

    // Machine Details
    const [name, setName] = useState('');
    const [autoLogin, setAutoLogin] = useState(false);

    // Host Selection (Fetched from Group Details)
    const [hosts, setHosts] = useState<Record<string, string>>({});
    const [selectedHost, setSelectedHost] = useState('');

    // Credentials (if auto login)
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [passcode, setPasscode] = useState('');
    const [agentServer, setAgentServer] = useState('');

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Fetch Groups on Open
    useEffect(() => {
        if (isOpen) {
            rsocketService.requestResponse('getGroups').then(resp => {
                const list = typeof resp === 'string' ? JSON.parse(resp) : resp;
                setGroups(list);
                if (list.length > 0) setSelectedGroup(list[0]);
            }).catch(e => console.error("Failed to fetch groups", e));
        }
    }, [isOpen]);

    // Fetch Group Details when Group Selected
    useEffect(() => {
        if (selectedGroup) {
            rsocketService.requestResponse(`getGroupDetails:${selectedGroup}`).then(resp => {
                const details = typeof resp === 'string' ? JSON.parse(resp) : resp;
                if (details.hosts) {
                    setHosts(details.hosts);
                    const keys = Object.keys(details.hosts);
                    if (keys.length > 0) setSelectedHost(details.hosts[keys[0]]); // Default to first host
                }
            }).catch(e => console.error("Failed to fetch group details", e));
        }
    }, [selectedGroup]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        const options = [];
        // Construct options list similar to legacy builder
        // "gateway=IP"
        if (selectedHost) {
            options.push(`gateway=${selectedHost}`);
        }

        if (autoLogin) {
            options.push("--auto-login");
            if (username) options.push(`username=${username}`);
            if (password) options.push(`password=${password}`);
            if (passcode) options.push(`passcode=${passcode}`);
            if (agentServer) options.push(`agent=${agentServer}`);
        }

        try {
            const payload = {
                group: selectedGroup,
                name: name,
                options: options
            };
            await rsocketService.requestResponse(`createMachine:${JSON.stringify(payload)}`);
            onCreated();
            onClose();
            setName('');
        } catch (err) {
            console.error("Failed to create machine", err);
            setError("Failed to create machine.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>Create New Bot</DialogTitle>
                    <DialogDescription>
                        Configure a new bot instance.
                    </DialogDescription>
                </DialogHeader>

                {error && (
                    <div className="p-2 bg-destructive/15 text-destructive text-sm rounded">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">

                    {/* Group Selection */}
                    <div className="grid w-full gap-1.5">
                        <Label htmlFor="group-select">Group</Label>
                        <select
                            id="group-select"
                            value={selectedGroup}
                            onChange={(e) => setSelectedGroup(e.target.value)}
                            className={cn(
                                "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                            )}
                        >
                            {groups.map(g => <option key={g} value={g}>{g}</option>)}
                        </select>
                    </div>

                    {/* Character Name */}
                    <div className="grid w-full gap-1.5">
                        <Label htmlFor="bot-name">Character Name</Label>
                        <Input
                            id="bot-name"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                        />
                    </div>

                    {/* Host Selection */}
                    <div className="grid w-full gap-1.5">
                        <Label htmlFor="host-select">Server/Host</Label>
                        <select
                            id="host-select"
                            value={selectedHost}
                            onChange={(e) => setSelectedHost(e.target.value)}
                            className={cn(
                                "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                            )}
                        >
                            {Object.entries(hosts).map(([key, val]) => (
                                <option key={key} value={val}>{key}</option>
                            ))}
                        </select>
                    </div>

                    {/* Auto Login Toggle */}
                    <div className="flex items-center space-x-2">
                        <input
                            type="checkbox"
                            id="auto-login"
                            checked={autoLogin}
                            onChange={(e) => setAutoLogin(e.target.checked)}
                            className="h-4 w-4 rounded border-primary text-primary focus:ring-primary"
                        />
                        <Label htmlFor="auto-login">Enable Auto Login</Label>
                    </div>

                    {/* Auto Login Fields */}
                    {autoLogin && (
                        <div className="space-y-3 pl-4 border-l-2 border-muted">
                            <div className="grid w-full gap-1.5">
                                <Label htmlFor="username" className="text-xs">Username</Label>
                                <Input id="username" value={username} onChange={e => setUsername(e.target.value)} className="h-8" />
                            </div>
                            <div className="grid w-full gap-1.5">
                                <Label htmlFor="password" className="text-xs">Password</Label>
                                <Input id="password" type="password" value={password} onChange={e => setPassword(e.target.value)} className="h-8" />
                            </div>
                            <div className="grid w-full gap-1.5">
                                <Label htmlFor="passcode" className="text-xs">Second Passcode</Label>
                                <Input id="passcode" type="password" value={passcode} onChange={e => setPasscode(e.target.value)} className="h-8" />
                            </div>
                            <div className="grid w-full gap-1.5">
                                <Label htmlFor="agent" className="text-xs">Agent Server</Label>
                                <Input id="agent" value={agentServer} onChange={e => setAgentServer(e.target.value)} className="h-8" />
                            </div>
                        </div>
                    )}

                    <DialogFooter>
                        <Button type="button" variant="secondary" onClick={onClose}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={loading}>
                            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            {loading ? 'Creating...' : 'Create Bot'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};
