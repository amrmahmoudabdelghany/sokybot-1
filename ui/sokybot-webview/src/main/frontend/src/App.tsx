import { useState, useEffect } from 'react'
import { rsocketService } from './RSocketClient'
import { useSokybotStore } from './store'
import { Layout } from './Layout'
import { MachineView } from './MachineView'
import './App.css'

function App() {
  const { selectedMachineId, fetchInitialData, lastError } = useSokybotStore();
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const initConnection = async () => {
      try {
        await rsocketService.connect();
        setIsConnected(true);
        // Load initial data after connection
        fetchInitialData();
      } catch (err: any) {
        console.error("Failed to connect to RSocket", err);
        setError(err.message || "Failed to connect to backend");
      }
    };

    initConnection();
  }, [fetchInitialData]);

  const displayError = error || lastError;

  if (displayError) {
    return (
      <div className="flex items-center justify-center h-screen bg-background text-destructive text-lg">
        Error: {displayError}. Make sure Sokybot backend is running.
      </div>
    );
  }

  if (!isConnected) {
    return (
      <div className="flex items-center justify-center h-screen bg-background text-foreground animate-pulse">
        Connecting to Sokybot...
      </div>
    );
  }

  return (
    <Layout>
      {selectedMachineId ? (
        <MachineView machineId={selectedMachineId} />
      ) : (
        <div className="flex items-center justify-center h-full text-slate-500">
          Select a machine from the sidebar to view details
        </div>
      )}
    </Layout>
  )
}

export default App
