import React, { useState, useEffect } from 'react';
import { rsocketService } from '../RSocketClient';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Folder, HardDrive, ArrowUp, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

interface DirectoryPickerDialogProps {
    isOpen: boolean;
    onClose: () => void;
    onSelect: (path: string) => void;
    initialPath?: string;
}

interface FileEntry {
    name: string;
    path: string;
    isDirectory: boolean;
}

interface FsResponse {
    current: string;
    files: FileEntry[];
}

export const DirectoryPickerDialog: React.FC<DirectoryPickerDialogProps> = ({ isOpen, onClose, onSelect, initialPath = "." }) => {
    const [currentPath, setCurrentPath] = useState(initialPath);
    const [files, setFiles] = useState<FileEntry[]>([]);
    const [roots, setRoots] = useState<FileEntry[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedPath, setSelectedPath] = useState<string | null>(null);

    const fetchPath = async (path: string) => {
        setLoading(true);
        try {
            const resp = await rsocketService.requestResponse(`fs.list:${path}`);
            const data: FsResponse = typeof resp === 'string' ? JSON.parse(resp) : resp;
            setCurrentPath(data.current);
            setFiles(data.files);
            // If fetching new dir, clear selection unless it matches?
            setSelectedPath(null);
        } catch (err) {
            console.error("Failed to list dir", err);
        } finally {
            setLoading(false);
        }
    };

    const fetchRoots = async () => {
        try {
            const resp = await rsocketService.requestResponse(`fs.roots`);
            const data: FileEntry[] = typeof resp === 'string' ? JSON.parse(resp) : resp;
            setRoots(data);
        } catch (err) {
            console.error("Failed to list roots", err);
        }
    };

    useEffect(() => {
        if (isOpen) {
            fetchRoots();
            fetchPath(initialPath);
        }
    }, [isOpen]);

    const handleNavigate = (path: string) => {
        fetchPath(path);
    };

    const handleSelect = () => {
        // Return current path if nothing selected, or selected directory
        if (selectedPath) {
            onSelect(selectedPath);
        } else {
            onSelect(currentPath);
        }
        onClose();
    };


    // Replicates logic from SilkroadUtils.isSilkraodDirectory
    const isValidGameDirectory = (files: FileEntry[]) => {
        const requiredFiles = new Set([
            "sro_client.exe",
            "data.pk2",
            "map.pk2",
            "media.pk2",
            "music.pk2",
            "particles.pk2",
            "gfxfilemanager.dll"
        ]);

        // Convert listings to lower case for case-insensitive check
        const currentFileNames = new Set(files.map(f => f.name.toLowerCase()));

        let valid = true;
        requiredFiles.forEach(req => {
            if (!currentFileNames.has(req)) {
                valid = false;
            }
        });
        return valid;
    };

    const isCurrentDirValid = isValidGameDirectory(files);

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-lg h-[80vh] flex flex-col">
                <DialogHeader>
                    <DialogTitle>Select Directory</DialogTitle>
                    <DialogDescription>
                        Navigate to the game folder.
                    </DialogDescription>
                </DialogHeader>

                <div className="flex gap-2 mb-2 items-center">
                    <Button variant="outline" size="icon" onClick={() => {
                        const parent = files.find(f => f.name === "..");
                        if (parent) {
                            handleNavigate(parent.path);
                        } else {
                            // Fallback using string manipulation
                            const parts = currentPath.split('/');
                            parts.pop();
                            const parentPath = parts.join('/') || "/";
                            handleNavigate(parentPath);
                        }
                    }} disabled={loading} title="Up">
                        <ArrowUp className="h-4 w-4" />
                    </Button>
                    <div className="flex-1 text-sm bg-muted p-2 rounded truncate font-mono border">
                        {currentPath}
                    </div>
                </div>

                <div className="flex-1 overflow-y-auto border rounded bg-background p-2">
                    {loading ? (
                        <div className="flex justify-center items-center h-full">
                            <Loader2 className="animate-spin h-6 w-6 text-primary" />
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 gap-1">
                            {/* Roots Quick Access */}
                            {currentPath === "." && roots.length > 0 && (
                                <div className="text-xs font-semibold text-muted-foreground mb-1">Drives</div>
                            )}
                            {currentPath === "." && roots.map(root => (
                                <div
                                    key={root.path}
                                    className="flex items-center gap-2 p-2 rounded hover:bg-accent cursor-pointer"
                                    onClick={() => handleNavigate(root.path)}
                                >
                                    <HardDrive className="h-4 w-4 text-blue-500" />
                                    <span className="text-sm">{root.name}</span>
                                </div>
                            ))}

                            {files.map(f => (
                                <div
                                    key={f.path}
                                    className={cn(
                                        "flex items-center gap-2 p-2 rounded cursor-pointer",
                                        selectedPath === f.path ? "bg-primary/20" : "hover:bg-accent"
                                    )}
                                    onClick={() => {
                                        if (f.isDirectory) {
                                            setSelectedPath(f.path);
                                        }
                                    }}
                                    onDoubleClick={() => {
                                        if (f.isDirectory) handleNavigate(f.path);
                                    }}
                                >
                                    {f.isDirectory ? (
                                        <Folder className="h-4 w-4 text-yellow-500" />
                                    ) : (
                                        <div className="h-4 w-4 opacity-0" />
                                    )}
                                    <span className="text-sm truncate flex-1">{f.name}</span>
                                </div>
                            ))}
                            {files.length === 0 && (
                                <div className="text-muted-foreground text-sm italic p-4 text-center">Empty directory</div>
                            )}
                        </div>
                    )}
                </div>

                <div className="text-xs text-muted-foreground min-h-[1.25rem]">
                    {selectedPath ? `Selected: ${selectedPath}` : (
                        !isCurrentDirValid && !loading ? "Not a valid Silkroad directory" : ""
                    )}
                </div>

                <DialogFooter>
                    <Button variant="secondary" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button onClick={handleSelect} disabled={!isCurrentDirValid || loading}>
                        Select Current Folder
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};
