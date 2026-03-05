import { useState, useEffect } from 'react'
import { rsocketService } from './RSocketClient'
import { useSokybotStore } from './store'
import { Layout } from './Layout'
import { MachineView } from './MachineView'
import { useExtensionRegistry } from './extensions/ExtensionRegistry'
import { ExtensionView } from './extensions/ExtensionView'
import './App.css'

function App() {
  const { selectedMachineId, selectedPageId, fetchInitialData, lastError } = useSokybotStore();
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const extensionRegistry = useExtensionRegistry();

  const connectToBackend = async () => {
    setError(null);
    try {
      await rsocketService.connect();
      setIsConnected(true);
      fetchInitialData();
    } catch (err: any) {
      console.error("Failed to connect to RSocket", err);
      const msg = err?.message || "Connection closed";
      setError(msg.includes("Connection closed") ? "Connection closed" : msg);
    }
  };

  useEffect(() => {
    connectToBackend();
  }, [fetchInitialData]);

  const displayError = error || lastError;

  if (displayError) {
    return (
      <div className="flex items-center justify-center h-screen bg-background text-destructive text-lg flex flex-col gap-4 p-6">
        <p>Error: {displayError}. Make sure Sokybot backend is running.</p>
        <p className="text-sm text-muted-foreground max-w-md text-center">
          If using Docker, ensure the backend container is up and Karaf has finished starting (RSocket on port 8182).
          If you open via sokybot.local, ensure api.sokybot.local resolves to the same host and port 8182 is reachable.
        </p>
        <button
          type="button"
          onClick={() => connectToBackend()}
          className="px-4 py-2 rounded bg-primary text-primary-foreground hover:opacity-90"
        >
          Retry connection
        </button>
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

  const renderMainContent = () => {
    if (selectedMachineId) {
      return <MachineView machineId={selectedMachineId} />;
    }

    if (selectedPageId && extensionRegistry.pages[selectedPageId]) {
      const page = extensionRegistry.pages[selectedPageId];
      return (
        <ExtensionView
          pageId={page.pageId}
          componentType={page.componentType || 'declarative'}
          machineId={''}
          props={page.props}
        />
      );
    }

    return (
      <div className="flex items-center justify-center h-full text-slate-500">
        Select a machine or log page from the sidebar to view details
      </div>
    );
  };

  return (
    <Layout>
      {renderMainContent()}
    </Layout>
  )
}

export default App
