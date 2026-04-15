import React, { useState, useEffect } from 'react';
import { rsocketService } from '../RSocketClient';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@sokybot/frontend-shared';
import { Button } from '@sokybot/frontend-shared';
import { Input } from '@sokybot/frontend-shared';
import { Label } from '@sokybot/frontend-shared';
import { Loader2, FolderOpen } from 'lucide-react';
import { DirectoryPickerDialog } from './DirectoryPickerDialog';

interface CreateGroupDialogProps {
    isOpen: boolean;
    onClose: () => void;
    onCreated: () => void;
}

export const CreateGroupDialog: React.FC<CreateGroupDialogProps> = ({ isOpen, onClose, onCreated }) => {
    const [name, setName] = useState('');
    const [path, setPath] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isPickerOpen, setPickerOpen] = useState(false);
    const [isManual, setIsManual] = useState(false);
    const [manualHost, setManualHost] = useState('');
    const [manualDivision, setManualDivision] = useState('');

    useEffect(() => {
        if (isOpen) {
            setError(null);
            setIsManual(false);
            setManualDivision("");
            setManualHost("");
            setName("");
            setPath("");
        }
    }, [isOpen]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            // Use new typed API
            await rsocketService.createGroup(name, path, isManual, manualHost, manualDivision);
            onCreated();
            onClose();
            setName('');
            setPath('');
            setIsManual(false);
            setManualHost('');
            setManualDivision('');
        } catch (err) {
            console.error("Failed to create group", err);
            setError("Failed to create group. Ensure name is unique and path is valid.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <Dialog open={isOpen} onOpenChange={onClose}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>Create Group</DialogTitle>
                        <DialogDescription>
                            Create a new bot group by specifying the name and game path.
                        </DialogDescription>
                    </DialogHeader>

                    {error && (
                        <div className="p-2 bg-destructive/15 text-destructive text-sm rounded">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="grid w-full gap-1.5">
                            <Label htmlFor="group-name">Group Name</Label>
                            <Input
                                id="group-name"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="MyGroup"
                                required
                            />
                        </div>

                        <div className="grid w-full gap-1.5">
                            <Label htmlFor="game-path">Game Path</Label>
                            <div className="flex gap-2">
                                <Input
                                    id="game-path"
                                    value={path}
                                    onChange={(e) => setPath(e.target.value)}
                                    placeholder="/path/to/game/folder"
                                    required
                                    className="flex-1"
                                />
                                <Button type="button" variant="outline" size="icon" onClick={() => setPickerOpen(true)} title="Browse">
                                    <FolderOpen className="h-4 w-4" />
                                </Button>
                            </div>
                            <p className="text-xs text-muted-foreground">
                                Full path to the directory containing game files (sro_client.exe).
                            </p>
                        </div>

                        <div className="space-y-4 pt-2 border-t">
                            <div className="flex items-center space-x-2">
                                <input
                                    type="checkbox"
                                    id="manual-override"
                                    checked={isManual}
                                    onChange={(e) => setIsManual(e.target.checked)}
                                    className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
                                />
                                <Label htmlFor="manual-override" className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                                    Manual Connection Settings
                                </Label>
                            </div>

                            {isManual && (
                                <div className="grid grid-cols-2 gap-4 animate-in fade-in slide-in-from-top-1">
                                    <div className="grid gap-1.5">
                                        <Label htmlFor="manual-division">Division Name</Label>
                                        <Input
                                            id="manual-division"
                                            value={manualDivision}
                                            onChange={(e) => setManualDivision(e.target.value)}
                                            placeholder="S_Official"
                                            required={isManual}
                                        />
                                    </div>
                                    <div className="grid gap-1.5">
                                        <Label htmlFor="manual-host">Login Host (IP/DNS)</Label>
                                        <Input
                                            id="manual-host"
                                            value={manualHost}
                                            onChange={(e) => setManualHost(e.target.value)}
                                            placeholder="127.0.0.1"
                                            required={isManual}
                                        />
                                    </div>
                                </div>
                            )}
                        </div>

                        <DialogFooter>
                            <Button type="button" variant="secondary" onClick={onClose}>
                                Cancel
                            </Button>
                            <Button type="submit" disabled={loading}>
                                {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                                {loading ? 'Creating...' : 'Create Group'}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            <DirectoryPickerDialog
                isOpen={isPickerOpen}
                onClose={() => setPickerOpen(false)}
                onSelect={(val) => {
                    setPath(val);
                }}
                initialPath={path || "."}
            />
        </>
    );
};
