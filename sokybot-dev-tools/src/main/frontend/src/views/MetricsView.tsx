import { useEffect, useState } from 'react'
import { devToolsService, Metrics } from '@/services/devtools-service'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { RefreshCw, Cpu, MemoryStick, GitBranch, Clock } from 'lucide-react'
import { cn } from '@/lib/utils'

export function MetricsView() {
    const [metrics, setMetrics] = useState<Metrics | null>(null)
    const [loading, setLoading] = useState(true)
    const [autoRefresh, setAutoRefresh] = useState(true)

    const loadMetrics = async () => {
        try {
            const data = await devToolsService.getMetrics()
            setMetrics(data)
            setLoading(false)
        } catch (err: any) {
            console.error('Failed to load metrics', err)
            setLoading(false)
        }
    }

    useEffect(() => {
        loadMetrics()

        // Subscribe to metrics stream
        if (autoRefresh) {
            const subscription = devToolsService.subscribeToMetrics(
                (event) => {
                    if (event.data) {
                        setMetrics(event.data)
                    }
                },
                (err) => console.error('Metrics stream error', err)
            )

            return () => {
                subscription.unsubscribe()
            }
        }
    }, [autoRefresh])

    const formatBytes = (bytes: number) => {
        if (bytes === 0) return '0 B'
        const k = 1024
        const sizes = ['B', 'KB', 'MB', 'GB']
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
    }

    const formatUptime = (ms: number) => {
        const seconds = Math.floor(ms / 1000)
        const minutes = Math.floor(seconds / 60)
        const hours = Math.floor(minutes / 60)
        const days = Math.floor(hours / 24)
        
        if (days > 0) return `${days}d ${hours % 24}h ${minutes % 60}m`
        if (hours > 0) return `${hours}h ${minutes % 60}m ${seconds % 60}s`
        if (minutes > 0) return `${minutes}m ${seconds % 60}s`
        return `${seconds}s`
    }

    if (loading && !metrics) {
        return (
            <div className="flex items-center justify-center h-full">
                <div className="text-center">
                    <RefreshCw className="h-8 w-8 animate-spin mx-auto mb-4 text-muted-foreground" />
                    <p className="text-muted-foreground">Loading metrics...</p>
                </div>
            </div>
        )
    }

    if (!metrics) {
        return (
            <div className="flex items-center justify-center h-full">
                <p className="text-muted-foreground">No metrics available</p>
            </div>
        )
    }

    return (
        <div className="space-y-4">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold">Runtime Metrics</h2>
                <div className="flex items-center gap-2">
                    <label className="flex items-center gap-2 text-sm">
                        <input
                            type="checkbox"
                            checked={autoRefresh}
                            onChange={(e) => setAutoRefresh(e.target.checked)}
                            className="rounded"
                        />
                        Auto-refresh
                    </label>
                    <button
                        onClick={loadMetrics}
                        className="p-2 hover:bg-muted rounded"
                    >
                        <RefreshCw className={cn("h-4 w-4", !autoRefresh && "animate-spin")} />
                    </button>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Memory Metrics */}
                <Card>
                    <CardHeader>
                        <div className="flex items-center gap-2">
                            <MemoryStick className="h-5 w-5" />
                            <CardTitle>Memory</CardTitle>
                        </div>
                        <CardDescription>Heap and non-heap memory usage</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div>
                            <div className="flex justify-between text-sm mb-1">
                                <span>Heap Memory</span>
                                <span className="font-medium">
                                    {formatBytes(metrics.memory.heapUsed)} / {formatBytes(metrics.memory.heapMax)}
                                </span>
                            </div>
                            <div className="w-full bg-secondary rounded-full h-2">
                                <div
                                    className="bg-primary h-2 rounded-full transition-all"
                                    style={{ width: `${metrics.memory.heapPercent}%` }}
                                />
                            </div>
                            <div className="text-xs text-muted-foreground mt-1">
                                {metrics.memory.heapPercent.toFixed(2)}% used
                            </div>
                        </div>

                        <div className="space-y-2 text-sm">
                            <div className="flex justify-between">
                                <span>Heap Committed:</span>
                                <span className="font-mono">{formatBytes(metrics.memory.heapCommitted)}</span>
                            </div>
                            <div className="flex justify-between">
                                <span>Non-Heap Used:</span>
                                <span className="font-mono">{formatBytes(metrics.memory.nonHeapUsed)}</span>
                            </div>
                            <div className="flex justify-between">
                                <span>Non-Heap Committed:</span>
                                <span className="font-mono">{formatBytes(metrics.memory.nonHeapCommitted)}</span>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Thread Metrics */}
                <Card>
                    <CardHeader>
                        <div className="flex items-center gap-2">
                            <GitBranch className="h-5 w-5" />
                            <CardTitle>Threads</CardTitle>
                        </div>
                        <CardDescription>Thread pool statistics</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="grid grid-cols-2 gap-4 text-sm">
                            <div>
                                <div className="text-muted-foreground">Current Threads</div>
                                <div className="text-2xl font-bold">{metrics.threads.threadCount}</div>
                            </div>
                            <div>
                                <div className="text-muted-foreground">Peak Threads</div>
                                <div className="text-2xl font-bold">{metrics.threads.peakThreadCount}</div>
                            </div>
                            <div>
                                <div className="text-muted-foreground">Daemon Threads</div>
                                <div className="text-2xl font-bold">{metrics.threads.daemonThreadCount}</div>
                            </div>
                            <div>
                                <div className="text-muted-foreground">Total Started</div>
                                <div className="text-2xl font-bold">{metrics.threads.totalStartedThreadCount}</div>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Runtime Info */}
                <Card className="md:col-span-2">
                    <CardHeader>
                        <div className="flex items-center gap-2">
                            <Clock className="h-5 w-5" />
                            <CardTitle>Runtime</CardTitle>
                        </div>
                        <CardDescription>JVM runtime information</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                            <div>
                                <div className="flex items-center gap-2 mb-2">
                                    <Cpu className="h-4 w-4 text-muted-foreground" />
                                    <span className="text-sm text-muted-foreground">Processors</span>
                                </div>
                                <div className="text-2xl font-bold">{metrics.runtime.availableProcessors}</div>
                            </div>
                            <div>
                                <div className="text-sm text-muted-foreground mb-2">Uptime</div>
                                <div className="text-2xl font-bold">{formatUptime(metrics.runtime.uptime)}</div>
                            </div>
                            <div>
                                <div className="text-sm text-muted-foreground mb-2">Start Time</div>
                                <div className="text-sm font-mono">
                                    {new Date(metrics.runtime.startTime).toLocaleString()}
                                </div>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>

            {metrics.timestamp && (
                <div className="text-xs text-muted-foreground text-center">
                    Last updated: {new Date(metrics.timestamp).toLocaleTimeString()}
                </div>
            )}
        </div>
    )
}
