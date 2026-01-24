import { useEffect, useState, useRef } from 'react'
import { devToolsService } from '@/services/devtools-service'
import { Card, CardContent, CardHeader, CardTitle } from '@sokybot/frontend-shared'
import { Button } from '@sokybot/frontend-shared'
import { Input } from '@sokybot/frontend-shared'
import { Badge } from '@sokybot/frontend-shared'
import { FileText, Trash2, Search, RefreshCw, Download } from 'lucide-react'
import { cn } from '@sokybot/frontend-shared'

interface LogEntry {
    timestamp?: string;
    level?: string;
    thread?: string;
    logger?: string;
    message?: string;
    raw?: string;
}

export function LogsView() {
    const [searchTerm, setSearchTerm] = useState('')
    const [filterLevel, setFilterLevel] = useState<string>('ALL')
    const [logFiles, setLogFiles] = useState<any[]>([])
    const [selectedFile, setSelectedFile] = useState<string | null>(null)
    const [logs, setLogs] = useState<LogEntry[]>([])
    const [loading, setLoading] = useState(false)
    const [autoScroll, setAutoScroll] = useState(true)
    const [maxLines, setMaxLines] = useState(100)
    const logsEndRef = useRef<HTMLDivElement>(null)

    const loadLogFiles = async () => {
        try {
            const files = await devToolsService.getLogFiles()
            setLogFiles(files)
            if (files.length > 0 && !selectedFile) {
                setSelectedFile(files[0].path)
            }
        } catch (err: any) {
            console.error('Failed to load log files', err)
        }
    }

    const loadLogs = async () => {
        if (!selectedFile) return

        try {
            setLoading(true)
            const entries = await devToolsService.readLogEntries(
                selectedFile,
                maxLines,
                filterLevel,
                searchTerm || undefined
            )
            setLogs(entries)
            if (autoScroll) {
                setTimeout(() => {
                    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' })
                }, 100)
            }
        } catch (err: any) {
            console.error('Failed to load logs', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadLogFiles()
    }, [])

    useEffect(() => {
        if (selectedFile) {
            loadLogs()
        }
    }, [selectedFile, filterLevel, searchTerm, maxLines])

    useEffect(() => {
        if (!selectedFile) return

        // Subscribe to log stream
        const subscription = devToolsService.subscribeToLogs(
            (event) => {
                if (event.type === 'log' && event.data) {
                    setLogs(prev => {
                        const newLogs = [...prev, event.data]
                        // Keep only last maxLines
                        return newLogs.slice(-maxLines)
                    })
                    if (autoScroll) {
                        setTimeout(() => {
                            logsEndRef.current?.scrollIntoView({ behavior: 'smooth' })
                        }, 100)
                    }
                }
            },
            (err) => console.error('Log stream error', err)
        )

        return () => {
            subscription.unsubscribe()
        }
    }, [selectedFile, autoScroll, maxLines])

    const getLevelColor = (level: string = '') => {
        switch (level.toUpperCase()) {
            case 'ERROR': return 'text-red-400'
            case 'WARN': return 'text-yellow-400'
            case 'INFO': return 'text-green-400'
            case 'DEBUG': return 'text-blue-400'
            default: return 'text-gray-400'
        }
    }

    const formatBytes = (bytes: number) => {
        if (bytes === 0) return '0 B'
        const k = 1024
        const sizes = ['B', 'KB', 'MB', 'GB']
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
    }

    const clearLogs = () => {
        setLogs([])
    }

    const exportLogs = () => {
        const logText = logs.map(log => log.raw || log.message || JSON.stringify(log)).join('\n')
        const blob = new Blob([logText], { type: 'text/plain' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `logs-${new Date().toISOString()}.txt`
        a.click()
        URL.revokeObjectURL(url)
    }

    return (
        <div className="space-y-4 h-full flex flex-col">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <FileText className="h-5 w-5" />
                    <h2 className="text-xl font-semibold">Logs</h2>
                </div>
                <div className="flex items-center gap-2">
                    <Button onClick={loadLogs} variant="outline" size="sm" disabled={loading || !selectedFile}>
                        <RefreshCw className={cn("h-4 w-4 mr-2", loading && "animate-spin")} />
                        Refresh
                    </Button>
                    <Button onClick={exportLogs} variant="outline" size="sm" disabled={logs.length === 0}>
                        <Download className="h-4 w-4 mr-2" />
                        Export
                    </Button>
                    <Button onClick={clearLogs} variant="outline" size="sm">
                        <Trash2 className="h-4 w-4 mr-2" />
                        Clear
                    </Button>
                </div>
            </div>

            {/* Log File Selector */}
            {logFiles.length > 0 && (
                <div className="flex items-center gap-2">
                    <label className="text-sm font-medium">Log File:</label>
                    <select
                        value={selectedFile || ''}
                        onChange={(e) => setSelectedFile(e.target.value)}
                        className="h-10 rounded-md border border-input bg-background px-3 text-sm flex-1"
                    >
                        {logFiles.map(file => (
                            <option key={file.path} value={file.path}>
                                {file.name} ({formatBytes(file.size)})
                            </option>
                        ))}
                    </select>
                </div>
            )}

            {/* Filters */}
            <div className="flex items-center gap-2">
                <div className="relative flex-1">
                    <Search className="absolute left-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                        placeholder="Search logs..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-8"
                    />
                </div>
                <select
                    value={filterLevel}
                    onChange={(e) => setFilterLevel(e.target.value)}
                    className="h-10 rounded-md border border-input bg-background px-3 text-sm"
                >
                    <option value="ALL">All Levels</option>
                    <option value="ERROR">Errors</option>
                    <option value="WARN">Warnings</option>
                    <option value="INFO">Info</option>
                    <option value="DEBUG">Debug</option>
                </select>
                <select
                    value={maxLines}
                    onChange={(e) => setMaxLines(parseInt(e.target.value))}
                    className="h-10 rounded-md border border-input bg-background px-3 text-sm"
                >
                    <option value="50">50 lines</option>
                    <option value="100">100 lines</option>
                    <option value="200">200 lines</option>
                    <option value="500">500 lines</option>
                </select>
                <label className="flex items-center gap-2 text-sm">
                    <input
                        type="checkbox"
                        checked={autoScroll}
                        onChange={(e) => setAutoScroll(e.target.checked)}
                        className="rounded"
                    />
                    Auto-scroll
                </label>
            </div>

            {/* Log Viewer */}
            <Card className="flex-1 overflow-hidden">
                <CardHeader className="pb-3">
                    <CardTitle className="text-lg">
                        {selectedFile ? `Log: ${logFiles.find(f => f.path === selectedFile)?.name || selectedFile}` : 'Application Logs'}
                    </CardTitle>
                </CardHeader>
                <CardContent className="h-full overflow-auto p-0">
                    {!selectedFile && logFiles.length === 0 ? (
                        <div className="p-8 text-center text-muted-foreground">
                            <FileText className="h-12 w-12 mx-auto mb-4 opacity-50" />
                            <p>No log files found.</p>
                            <p className="text-sm mt-2">Log files will appear here once the application starts logging.</p>
                        </div>
                    ) : (
                        <div className="bg-black text-green-400 p-4 rounded font-mono text-sm h-full overflow-y-auto">
                            {logs.length === 0 ? (
                                <div className="text-muted-foreground">
                                    {loading ? 'Loading logs...' : 'No logs to display'}
                                </div>
                            ) : (
                                logs.map((log, index) => (
                                    <div key={index} className="mb-1 hover:bg-gray-900 rounded px-2 py-1">
                                        {log.timestamp && (
                                            <span className="text-gray-500 mr-2">{log.timestamp}</span>
                                        )}
                                        {log.level && (
                                            <Badge 
                                                variant="outline" 
                                                className={cn("mr-2 text-xs", getLevelColor(log.level))}
                                            >
                                                {log.level}
                                            </Badge>
                                        )}
                                        {log.logger && (
                                            <span className="text-blue-400 mr-2">{log.logger}</span>
                                        )}
                                        <span className={log.level ? getLevelColor(log.level) : 'text-gray-300'}>
                                            {log.message || log.raw}
                                        </span>
                                    </div>
                                ))
                            )}
                            <div ref={logsEndRef} />
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    )
}
