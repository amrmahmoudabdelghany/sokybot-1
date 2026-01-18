import { useEffect, useState } from 'react'
import { devToolsService, Service } from '@/services/devtools-service'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { RefreshCw, Search, ChevronDown, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'

export function ServicesView() {
    const [services, setServices] = useState<Service[]>([])
    const [loading, setLoading] = useState(true)
    const [searchTerm, setSearchTerm] = useState('')
    const [interfaceFilter, setInterfaceFilter] = useState('')
    const [expandedServices, setExpandedServices] = useState<Set<number>>(new Set())
    const [error, setError] = useState<string | null>(null)

    const loadServices = async () => {
        try {
            setLoading(true)
            setError(null)
            const data = await devToolsService.getServices()
            setServices(data)
        } catch (err: any) {
            setError(err.message || 'Failed to load services')
            console.error('Failed to load services', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadServices()
    }, [])

    const handleSearchByInterface = async () => {
        if (!interfaceFilter.trim()) {
            loadServices()
            return
        }
        
        try {
            setLoading(true)
            setError(null)
            const data = await devToolsService.findServicesByInterface(interfaceFilter.trim())
            setServices(data)
        } catch (err: any) {
            setError(err.message || 'Failed to find services')
        } finally {
            setLoading(false)
        }
    }

    const toggleExpand = (serviceId: number) => {
        const newExpanded = new Set(expandedServices)
        if (newExpanded.has(serviceId)) {
            newExpanded.delete(serviceId)
        } else {
            newExpanded.add(serviceId)
        }
        setExpandedServices(newExpanded)
    }

    const filteredServices = services.filter(service => {
        if (!searchTerm) return true
        const searchLower = searchTerm.toLowerCase()
        return service.interfaces.some(iface => iface.toLowerCase().includes(searchLower)) ||
               service.bundleSymbolicName.toLowerCase().includes(searchLower)
    })

    if (loading && services.length === 0) {
        return (
            <div className="flex items-center justify-center h-full">
                <div className="text-center">
                    <RefreshCw className="h-8 w-8 animate-spin mx-auto mb-4 text-muted-foreground" />
                    <p className="text-muted-foreground">Loading services...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="space-y-4">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold">OSGi Services</h2>
                <Button onClick={loadServices} variant="outline" size="sm">
                    <RefreshCw className={cn("h-4 w-4 mr-2", loading && "animate-spin")} />
                    Refresh
                </Button>
            </div>

            {/* Filters */}
            <div className="flex items-center gap-2">
                <div className="relative flex-1">
                    <Search className="absolute left-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                        placeholder="Search services..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-8"
                    />
                </div>
                <div className="flex items-center gap-2">
                    <Input
                        placeholder="Filter by interface..."
                        value={interfaceFilter}
                        onChange={(e) => setInterfaceFilter(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleSearchByInterface()}
                        className="w-64"
                    />
                    <Button onClick={handleSearchByInterface} variant="outline" size="sm">
                        Filter
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

            {/* Services List */}
            <div className="space-y-2">
                {filteredServices.map((service) => (
                    <Card key={service.serviceId}>
                        <CardContent className="p-4">
                            <div className="flex items-start justify-between">
                                <div className="flex-1">
                                    <div className="flex items-center gap-2 mb-2">
                                        <button
                                            onClick={() => toggleExpand(service.serviceId)}
                                            className="p-1 hover:bg-muted rounded"
                                        >
                                            {expandedServices.has(service.serviceId) ? (
                                                <ChevronDown className="h-4 w-4" />
                                            ) : (
                                                <ChevronRight className="h-4 w-4" />
                                            )}
                                        </button>
                                        <div className="flex flex-wrap gap-1">
                                            {service.interfaces.map((iface, idx) => (
                                                <Badge key={idx} variant="outline">{iface}</Badge>
                                            ))}
                                        </div>
                                    </div>
                                    <div className="text-sm text-muted-foreground ml-6">
                                        Bundle: {service.bundleSymbolicName} (ID: {service.bundleId})
                                    </div>
                                    
                                    {expandedServices.has(service.serviceId) && (
                                        <div className="mt-4 ml-6 space-y-2">
                                            <div className="text-sm font-medium">Properties:</div>
                                            <div className="bg-muted rounded p-3 text-sm font-mono">
                                                <pre className="whitespace-pre-wrap break-all">
                                                    {JSON.stringify(service.properties, null, 2)}
                                                </pre>
                                            </div>
                                        </div>
                                    )}
                                </div>
                                <Badge variant="secondary">ID: {service.serviceId}</Badge>
                            </div>
                        </CardContent>
                    </Card>
                ))}
                {filteredServices.length === 0 && (
                    <Card>
                        <CardContent className="p-8 text-center text-muted-foreground">
                            {searchTerm || interfaceFilter ? 'No services match your filters' : 'No services found'}
                        </CardContent>
                    </Card>
                )}
            </div>
        </div>
    )
}
