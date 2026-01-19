import { useState, useEffect } from 'react'
import { rsocketService } from './RSocketClient'
import { Layout } from './Layout'
import { MachineView } from './MachineView'
import './App.css'

function App() {
  const [selectedMachine, setSelectedMachine] = useState<string>('');
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const initConnection = async () => {
      try {
        await rsocketService.connect();
        setIsConnected(true);
      } catch (err: any) {
        console.error("Failed to connect to RSocket", err);
        setError(err.message || "Failed to connect to backend");
      }
    };

    initConnection();
  }, []);

  if (error) {
    return (
      <div className="flex items-center justify-center h-screen bg-background text-destructive text-lg">
        Error: {error}. Make sure Sokybot backend is running.
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
    <Layout
      selectedMachine={selectedMachine}
      onMachineSelect={setSelectedMachine}
    >
      {selectedMachine ? (
        <MachineView machineId={selectedMachine} />
      ) : (
        <div className="flex items-center justify-center h-full text-slate-500">
          Select a machine from the sidebar to view details
        </div>
      )}
    </Layout>
  )
}

export default App
