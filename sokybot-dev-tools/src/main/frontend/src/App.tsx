import { useEffect, useState } from 'react'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { rsocketService } from './lib/rsocket-client'
import { BundlesView } from './views/BundlesView'
import { ServicesView } from './views/ServicesView'
import { MetricsView } from './views/MetricsView'
import { LogsView } from './views/LogsView'
import { Settings, AlertCircle } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import './App.css'

function App() {
  const [connected, setConnected] = useState(false)
  const [connectionError, setConnectionError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true
    let retryTimeout: NodeJS.Timeout | null = null
    
    const connect = () => {
      rsocketService.connect()
        .then(() => {
          if (mounted) {
            setConnected(true)
            setConnectionError(null)
          }
        })
        .catch((error) => {
          if (mounted) {
            setConnected(false)
            setConnectionError(error.message || 'Failed to connect to RSocket server on port 7002')
            console.error('Failed to connect to RSocket server', error)
            // Retry connection after 3 seconds
            retryTimeout = setTimeout(() => {
              if (mounted) {
                connect()
              }
            }, 3000)
          }
        })
    }
    
    connect()

    return () => {
      mounted = false
      if (retryTimeout) {
        clearTimeout(retryTimeout)
      }
      rsocketService.disconnect()
    }
  }, [])

  if (!connected) {
    return (
      <div className="flex items-center justify-center h-screen bg-background">
        <Card className="p-8 max-w-md">
          <div className="flex flex-col items-center gap-4">
            <AlertCircle className="h-12 w-12 text-muted-foreground" />
            <div className="text-center">
              <h2 className="text-xl font-semibold mb-2">Connecting to Dev Tools...</h2>
              {connectionError && (
                <p className="text-sm text-destructive mb-4">{connectionError}</p>
              )}
              <p className="text-sm text-muted-foreground">
                Ensure the application is running and RSocket server is available on port 7002
              </p>
            </div>
            <Button onClick={() => window.location.reload()}>Retry</Button>
          </div>
        </Card>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-screen bg-background">
      {/* Header */}
      <div className="border-b bg-card p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Settings className="h-6 w-6 text-primary" />
            <h1 className="text-2xl font-bold">Sokybot Dev Tools</h1>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-2 w-2 bg-green-500 rounded-full"></div>
            <span className="text-sm text-muted-foreground">Connected</span>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-hidden p-6">
        <Tabs defaultValue="bundles" className="h-full flex flex-col">
          <TabsList className="mb-4">
            <TabsTrigger value="bundles">Bundles</TabsTrigger>
            <TabsTrigger value="services">Services</TabsTrigger>
            <TabsTrigger value="metrics">Metrics</TabsTrigger>
            <TabsTrigger value="logs">Logs</TabsTrigger>
          </TabsList>

          <TabsContent value="bundles" className="flex-1 overflow-auto">
            <BundlesView />
          </TabsContent>

          <TabsContent value="services" className="flex-1 overflow-auto">
            <ServicesView />
          </TabsContent>

          <TabsContent value="metrics" className="flex-1 overflow-auto">
            <MetricsView />
          </TabsContent>

          <TabsContent value="logs" className="flex-1 overflow-auto">
            <LogsView />
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}

export default App
