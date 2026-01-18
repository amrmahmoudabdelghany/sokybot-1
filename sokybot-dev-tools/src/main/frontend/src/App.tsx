import { useEffect, useState } from 'react'

import { rsocketService } from './lib/rsocket-client'
import { BundlesView } from './views/BundlesView'
import { ServicesView } from './views/ServicesView'
import { MetricsView } from './views/MetricsView'
import { LogsView } from './views/LogsView'
import { AlertCircle } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Sidebar } from './components/layout/sidebar'
import { cn } from './lib/utils'
import './App.css'

function App() {
  const [connected, setConnected] = useState(false)
  const [connectionError, setConnectionError] = useState<string | null>(null)
  const [activeView, setActiveView] = useState("bundles")

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
    <div className="flex h-screen bg-background text-foreground transition-colors duration-300">
      {/* Sidebar Navigation */}
      <Sidebar activeView={activeView} onViewChange={setActiveView} />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="border-b bg-card p-4 shadow-sm">
          <div className="flex items-center justify-between">
            <h1 className="text-xl font-bold capitalize">{activeView}</h1>
            <div className="flex items-center gap-2">
              <div className={cn("h-2.5 w-2.5 rounded-full", connected ? "bg-green-500 animate-pulse" : "bg-red-500")} />
              <span className="text-sm text-muted-foreground font-medium">
                {connected ? "Connected" : "Disconnected"}
              </span>
            </div>
          </div>
        </header>

        {/* View Content */}
        <main className="flex-1 overflow-auto p-6 bg-muted/20">
          <div className="mx-auto max-w-6xl space-y-6">
            {activeView === 'bundles' && <BundlesView />}
            {activeView === 'services' && <ServicesView />}
            {activeView === 'metrics' && <MetricsView />}
            {activeView === 'logs' && <LogsView />}
          </div>
        </main>
      </div>
    </div>
  )
}

export default App
