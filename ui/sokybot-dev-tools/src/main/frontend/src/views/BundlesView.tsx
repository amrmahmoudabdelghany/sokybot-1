import { useEffect, useState } from 'react'
import { devToolsService, Bundle } from '@/services/devtools-service'
import { Button } from '@sokybot/frontend-shared'
import { Badge } from '@sokybot/frontend-shared'
import { Input } from '@sokybot/frontend-shared'
import { Card, CardContent } from '@sokybot/frontend-shared'
import { RefreshCw, Play, Square, RotateCw, Package, Search } from 'lucide-react'
import { cn } from '@sokybot/frontend-shared'

export function BundlesView() {
    const [bundles, setBundles] = useState<Bundle[]>([])
    const [loading, setLoading] = useState(true)
    const [searchTerm, setSearchTerm] = useState('')
    const [error, setError] = useState<string | null>(null)

    const loadBundles = async () => {
        try {
            setLoading(true)
            setError(null)
            const data = await devToolsService.getBundles()
            setBundles(data)
        } catch (err: any) {
            setError(err.message || 'Failed to load bundles')
            console.error('Failed to load bundles', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadBundles()

        // Subscribe to bundle events
        const subscription = devToolsService.subscribeToBundles(
            (event) => {
                // Reload bundles when events occur
                if (event.type === 'bundle.changed' || event.type === 'fileinstall') {
                    loadBundles()
                }
            },
            (err) => console.error('Bundle stream error', err)
        )

        return () => {
            subscription.unsubscribe()
        }
    }, [])

    const handleStart = async (bundleId: number) => {
        try {
            await devToolsService.startBundle(bundleId)
            loadBundles()
        } catch (err: any) {
            alert('Failed to start bundle: ' + err.message)
        }
    }

    const handleStop = async (bundleId: number) => {
        try {
            await devToolsService.stopBundle(bundleId)
            loadBundles()
        } catch (err: any) {
            alert('Failed to stop bundle: ' + err.message)
        }
    }

    const handleRestart = async (bundleId: number) => {
        try {
            await devToolsService.restartBundle(bundleId)
            loadBundles()
        } catch (err: any) {
            alert('Failed to restart bundle: ' + err.message)
        }
    }

    const handleReload = async (symbolicName: string) => {
        try {
            await devToolsService.reloadBundle(symbolicName)
            loadBundles()
        } catch (err: any) {
            alert('Failed to reload bundle: ' + err.message)
        }
    }

    const getStateColor = (state: string) => {
        switch (state) {
            case 'ACTIVE': return 'bg-green-500'
            case 'INSTALLED': return 'bg-yellow-500'
            case 'RESOLVED': return 'bg-blue-500'
            case 'STARTING': return 'bg-purple-500'
            case 'STOPPING': return 'bg-orange-500'
            default: return 'bg-gray-500'
        }
    }

    const filteredBundles = bundles.filter(bundle =>
        bundle.symbolicName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        bundle.location.toLowerCase().includes(searchTerm.toLowerCase())
    )

    if (loading && bundles.length === 0) {
        return (
            <div className="flex items-center justify-center h-full">
                <div className="text-center">
                    <RefreshCw className="h-8 w-8 animate-spin mx-auto mb-4 text-muted-foreground" />
                    <p className="text-muted-foreground">Loading bundles...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="space-y-4">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <Package className="h-5 w-5" />
                    <h2 className="text-xl font-semibold">Bundles</h2>
                    <Badge variant="secondary">{filteredBundles.length}</Badge>
                </div>
                <div className="flex items-center gap-2">
                    <div className="relative">
                        <Search className="absolute left-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                            placeholder="Search bundles..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="pl-8 w-64"
                        />
                    </div>
                    <Button onClick={loadBundles} variant="outline" size="sm">
                        <RefreshCw className={cn("h-4 w-4 mr-2", loading && "animate-spin")} />
                        Refresh
                    </Button>
                </div>
            </div>

            {error && (
                <Card className="border-destructive">
                    <CardContent className="pt-6">
                        <p className="text-destructive">{error}</p>
                    </CardContent>
                </Card>
            )}

            {/* Bundles Table */}
            <Card>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                                <tr className="border-b">
                                    <th className="text-left p-4 font-medium">Name</th>
                                    <th className="text-left p-4 font-medium">Version</th>
                                    <th className="text-left p-4 font-medium">State</th>
                                    <th className="text-left p-4 font-medium">Status</th>
                                    <th className="text-left p-4 font-medium">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredBundles.map((bundle) => (
                                    <tr key={bundle.id} className="border-b hover:bg-muted/50">
                                        <td className="p-4">
                                            <div className="flex flex-col">
                                                <span className="font-medium">{bundle.symbolicName}</span>
                                                <span className="text-xs text-muted-foreground truncate max-w-md">
                                                    {bundle.location}
                                                </span>
                                            </div>
                                        </td>
                                        <td className="p-4 text-sm">{bundle.version}</td>
                                        <td className="p-4">
                                            <Badge variant="outline" className="gap-1">
                                                <div className={cn("h-2 w-2 rounded-full", getStateColor(bundle.state))} />
                                                {bundle.state}
                                            </Badge>
                                        </td>
                                        <td className="p-4">
                                            <div className="flex flex-col gap-1">
                                                {bundle.isWatchingForChanges && (
                                                    <Badge variant="secondary" className="text-xs">Dev Mode</Badge>
                                                )}
                                                {bundle.needsReload && (
                                                    <Badge variant="destructive" className="text-xs">Needs Reload</Badge>
                                                )}
                                            </div>
                                        </td>
                                        <td className="p-4">
                                            <div className="flex items-center gap-2">
                                                {bundle.state === 'ACTIVE' ? (
                                                    <Button
                                                        onClick={() => handleStop(bundle.id)}
                                                        variant="outline"
                                                        size="sm"
                                                    >
                                                        <Square className="h-3 w-3 mr-1" />
                                                        Stop
                                                    </Button>
                                                ) : (
                                                    <Button
                                                        onClick={() => handleStart(bundle.id)}
                                                        variant="outline"
                                                        size="sm"
                                                    >
                                                        <Play className="h-3 w-3 mr-1" />
                                                        Start
                                                    </Button>
                                                )}
                                                <Button
                                                    onClick={() => handleRestart(bundle.id)}
                                                    variant="outline"
                                                    size="sm"
                                                >
                                                    <RotateCw className="h-3 w-3 mr-1" />
                                                    Restart
                                                </Button>
                                                {bundle.isWatchingForChanges && (
                                                    <Button
                                                        onClick={() => handleReload(bundle.symbolicName)}
                                                        variant="outline"
                                                        size="sm"
                                                        disabled={!bundle.needsReload}
                                                    >
                                                        <RefreshCw className="h-3 w-3 mr-1" />
                                                        Reload FS
                                                    </Button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                        {filteredBundles.length === 0 && (
                            <div className="p-8 text-center text-muted-foreground">
                                {searchTerm ? 'No bundles match your search' : 'No bundles found'}
                            </div>
                        )}
                    </div>
                </CardContent>
            </Card>
        </div>
    )
}
