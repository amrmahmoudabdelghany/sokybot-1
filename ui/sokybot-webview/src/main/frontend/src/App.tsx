import { useState, useEffect } from 'react'
import { rsocketService } from './RSocketClient'
import { useSokybotStore } from './store'
import { Layout } from './Layout'
import { MachineView } from './MachineView'
import { useExtensionRegistry } from './extensions/ExtensionRegistry'
import { ExtensionView } from './extensions/ExtensionView'
import { ErrorBoundary } from './ErrorBoundary'
import './App.css'

function App() {
  const {
    selectedMachineId,
    selectedPageId,
    fetchInitialData,
    fetchExtensionRegistry,
    addExtensionPage,
    removeExtensionPage,
    lastError
  } = useSokybotStore();
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const extensionRegistry = useExtensionRegistry();

  const connectToBackend = async () => {
    setError(null);
    try {
      await rsocketService.connect();
      setIsConnected(true);
      fetchInitialData();
      fetchExtensionRegistry().then(() => {
        console.log("Global extension registry initialized");
      });
    } catch (err: any) {
      console.error("Failed to connect to RSocket", err);
      const msg = err?.message || "Connection closed";
      setError(msg.includes("Connection closed") ? "Connection closed" : msg);
    }
  };

  useEffect(() => {
    connectToBackend();
  }, []);

  useEffect(() => {
    if (!isConnected) return;

    // Listen for extension events
    const subscription = rsocketService.subscribeToExtensionEvents(
      (event) => {
        if (event.type === 'ui.extension' && event.data) {
          const data = event.data;
          if (data.type === 'extension.page.added') {
            addExtensionPage({
              pageId: data.pageId as string,
              title: data.title as string,
              iconPath: data.iconPath as string,
              schema: data.schema,
              componentType: 'declarative',
              props: {}
            });
          } else if (data.type === 'extension.page.removed') {
            removeExtensionPage(data.pageId as string);
          }
        }
      },
      (error) => console.error("Extension event error", error)
    );

    return () => subscription?.unsubscribe();
  }, [isConnected, addExtensionPage, removeExtensionPage]);

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
      <ErrorBoundary>
        {renderMainContent()}
      </ErrorBoundary>
    </Layout>
  )
}

export default App
