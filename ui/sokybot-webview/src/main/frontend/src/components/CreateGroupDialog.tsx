import React, { useState } from 'react';
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

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            await rsocketService.requestResponse(`createGroup:${JSON.stringify({ name, path })}`);
            onCreated();
            onClose();
            setName('');
            setPath('');
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
