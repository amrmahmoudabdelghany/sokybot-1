import { useState, useEffect } from "react"
import { rsocketService } from "@/lib/rsocket-client"
import { Button } from "@sokybot/frontend-shared"
import { Card, CardContent, CardHeader, CardTitle } from "@sokybot/frontend-shared"
import { Input } from "@sokybot/frontend-shared"
import { Database, Search, Table as TableIcon, RefreshCw, AlertCircle } from "lucide-react"

interface DatabaseViewProps { }

export function DatabaseView({ }: DatabaseViewProps) {
    const [contexts, setContexts] = useState<string[]>([])
    const [selectedContext, setSelectedContext] = useState<string>("")
    const [tables, setTables] = useState<string[]>([])
    const [selectedTable, setSelectedTable] = useState<string>("")
    const [query, setQuery] = useState<string>("")
    const [results, setResults] = useState<any[]>([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        loadContexts()
    }, [])

    const loadContexts = async () => {
        try {
            setLoading(true)
            const response = await rsocketService.requestResponse("devtools:database.contexts")
            const json = JSON.parse(response)
            if (json.success) {
                setContexts(json.data)
                if (json.data.length > 0 && !selectedContext) {
                    // Don't auto-select context, let user choose
                }
            } else {
                setError(json.error)
            }
        } catch (err: any) {
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }

    const loadTables = async (context: string) => {
        try {
            setLoading(true)
            const response = await rsocketService.requestResponse(`devtools:database.tables:${context}`)
            const json = JSON.parse(response)
            if (json.success) {
                setTables(json.data)
            } else {
                setError(json.error)
            }
        } catch (err: any) {
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }

    const handleContextChange = (value: string) => {
        setSelectedContext(value)
        setTables([])
        setSelectedTable("")
        setResults([])
        loadTables(value)
    }

    const handleTableSelect = (table: string) => {
        setSelectedTable(table)
        setQuery(`SELECT * FROM ${table} LIMIT 100`)
        handleExecuteQuery(`SELECT * FROM ${table} LIMIT 100`, selectedContext)
    }

    const handleExecuteQuery = async (sql: string, context: string) => {
        if (!context || !sql) return

        try {
            setLoading(true)
            setError(null)
            const response = await rsocketService.requestResponse(`devtools:database.query:${context}:${sql}`)
            const json = JSON.parse(response)
            if (json.success) {
                setResults(json.data)
            } else {
                setError(json.error)
            }
        } catch (err: any) {
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="h-full flex flex-col gap-4">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <Database className="h-5 w-5" />
                    <h2 className="text-xl font-semibold">Database Explorer</h2>
                </div>
                <Button onClick={loadContexts} variant="outline" size="sm">
                    <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                    Refresh Contexts
                </Button>
            </div>

            <div className="grid grid-cols-12 gap-4 h-full overflow-hidden">
                {/* Sidebar: Contexts & Tables */}
                <div className="col-span-3 flex flex-col gap-4 h-full overflow-hidden">
                    <Card>
                        <CardHeader className="py-3">
                            <CardTitle className="text-sm font-medium">Data Source</CardTitle>
                        </CardHeader>
                        <CardContent className="py-2">
                            <select
                                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                value={selectedContext}
                                onChange={(e) => handleContextChange(e.target.value)}
                            >
                                <option value="" disabled>Select Game Context</option>
                                {contexts.map(ctx => (
                                    <option key={ctx} value={ctx}>{ctx}</option>
                                ))}
                            </select>
                        </CardContent>
                    </Card>

                    <Card className="flex-1 overflow-hidden flex flex-col">
                        <CardHeader className="py-3">
                            <CardTitle className="text-sm font-medium">Tables</CardTitle>
                        </CardHeader>
                        <CardContent className="flex-1 overflow-auto py-2 px-0">
                            {tables.length === 0 ? (
                                <div className="text-center text-muted-foreground p-4 text-sm">
                                    No tables found
                                </div>
                            ) : (
                                <div className="flex flex-col">
                                    {tables.map(table => (
                                        <button
                                            key={table}
                                            onClick={() => handleTableSelect(table)}
                                            className={`flex items-center gap-2 px-4 py-2 text-sm text-left hover:bg-muted ${selectedTable === table ? 'bg-muted font-medium' : ''}`}
                                        >
                                            <TableIcon className="h-3 w-3 text-muted-foreground" />
                                            <span className="truncate">{table}</span>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </div>

                {/* Main: Query & Results */}
                <div className="col-span-9 flex flex-col gap-4 h-full overflow-hidden">
                    {/* Query Editor */}
                    <Card>
                        <CardContent className="p-4 flex gap-2">
                            <div className="flex-1 relative">
                                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                                <Input
                                    value={query}
                                    onChange={(e) => setQuery(e.target.value)}
                                    className="pl-8 font-mono"
                                    placeholder="SELECT * FROM ..."
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter' && !e.shiftKey) {
                                            e.preventDefault()
                                            handleExecuteQuery(query, selectedContext)
                                        }
                                    }}
                                />
                            </div>
                            <Button
                                onClick={() => handleExecuteQuery(query, selectedContext)}
                                disabled={!selectedContext || loading}
                            >
                                Execute
                            </Button>
                        </CardContent>
                    </Card>

                    {/* Results Table */}
                    <Card className="flex-1 overflow-hidden flex flex-col">
                        {error && (
                            <div className="bg-destructive/10 text-destructive p-2 text-sm flex items-center gap-2 border-b border-destructive/20">
                                <AlertCircle className="h-4 w-4" />
                                {error}
                            </div>
                        )}
                        <CardContent className="flex-1 overflow-auto p-0">
                            {results.length === 0 ? (
                                <div className="h-full flex items-center justify-center text-muted-foreground">
                                    No results to display
                                </div>
                            ) : (
                                <div className="relative w-full overflow-auto">
                                    <table className="w-full caption-bottom text-sm text-left">
                                        <thead className="[&_tr]:border-b sticky top-0 bg-card z-10 shadow-sm">
                                            <tr className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                                                {Object.keys(results[0]).map(key => (
                                                    <th key={key} className="h-12 px-4 text-left align-middle font-medium text-muted-foreground [&:has([role=checkbox])]:pr-0 whitespace-nowrap">
                                                        {key}
                                                    </th>
                                                ))}
                                            </tr>
                                        </thead>
                                        <tbody className="[&_tr:last-child]:border-0">
                                            {results.map((row, i) => (
                                                <tr key={i} className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                                                    {Object.values(row).map((val: any, j) => (
                                                        <td key={j} className="p-4 align-middle [&:has([role=checkbox])]:pr-0 whitespace-nowrap max-w-xs truncate" title={String(val)}>
                                                            {val === null ? <span className="text-muted-foreground italic">null</span> : String(val)}
                                                        </td>
                                                    ))}
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </CardContent>
                        {results.length > 0 && (
                            <div className="p-2 border-t text-xs text-muted-foreground text-right bg-muted/20">
                                {results.length} rows returned
                            </div>
                        )}
                    </Card>
                </div>
            </div>
        </div>
    )
}
