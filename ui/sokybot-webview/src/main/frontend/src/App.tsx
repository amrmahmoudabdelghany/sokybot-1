import { useState, useEffect } from 'react'
import { rsocketService } from './RSocketClient'
import { useSokybotStore } from './store'
import { Layout } from './Layout'
import { MachineView } from './MachineView'
import { useExtensionRegistry } from './extensions/ExtensionRegistry'
import { ExtensionView } from './extensions/ExtensionView'
import { ErrorBoundary } from './ErrorBoundary'
import {
  useMachinesQuery,
  useGroupsQuery,
  useExtensionRegistryQuery,
} from './query/sokybotQueries'
import { extensionUiEventDataSchema } from './schemas/extensionEvents'
import { Button } from '@sokybot/frontend-shared'
import './App.css'

function App() {
  const {
    selectedMachineId,
    selectedPageId,
    addExtensionPage,
    removeExtensionPage,
    setExtensionRegistry,
  } = useSokybotStore();
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const extensionRegistry = useExtensionRegistry();

  const machinesQuery = useMachinesQuery(isConnected);
  const groupsQuery = useGroupsQuery(isConnected);
  const extensionRegistryQuery = useExtensionRegistryQuery(isConnected);

  useEffect(() => {
    if (extensionRegistryQuery.data) {
      setExtensionRegistry(
        extensionRegistryQuery.data as {
          pages: Record<string, unknown>;
          toolbarActions: Record<string, unknown>;
        }
      );
    }
  }, [extensionRegistryQuery.data, setExtensionRegistry]);

  const connectToBackend = async () => {
    setError(null);
    try {
      await rsocketService.connect();
      setIsConnected(true);
    } catch (err: unknown) {
      console.error("Failed to connect to RSocket", err);
      const msg = err instanceof Error ? err.message : "Connection closed";
      setError(msg.includes("Connection closed") ? "Connection closed" : msg);
    }
  };

  useEffect(() => {
    connectToBackend();
  }, []);

  useEffect(() => {
    if (!isConnected) return;

    const subscription = rsocketService.subscribeToExtensionEvents(
      (event) => {
        if (event.type === 'ui.extension' && event.data) {
          const parsed = extensionUiEventDataSchema.safeParse(event.data);
          if (!parsed.success) {
            console.warn('Invalid extension UI event payload', parsed.error.flatten());
            return;
          }
          const data = parsed.data;
          if (data.type === 'extension.page.added') {
            addExtensionPage({
              pageId: data.pageId,
              title: data.title,
              iconPath: data.iconPath,
              schema: data.schema,
              componentType: 'declarative',
              props: {},
            });
          } else if (data.type === 'extension.page.removed') {
            removeExtensionPage(data.pageId);
          }
        }
      },
      (err) => console.error("Extension event error", err)
    );

    return () => subscription?.unsubscribe();
  }, [isConnected, addExtensionPage, removeExtensionPage]);

  const bootstrapFailed =
    isConnected && (machinesQuery.isError || groupsQuery.isError);
  const bootstrapErr = machinesQuery.error ?? groupsQuery.error;
  const queryError = bootstrapFailed
    ? (bootstrapErr instanceof Error ? bootstrapErr.message : 'Failed to load machines or groups')
    : null;

  const displayError = error || queryError;

  if (displayError) {
    return (
      <div className="flex items-center justify-center h-screen bg-background text-destructive text-lg flex flex-col gap-4 p-6">
        <p>Error: {displayError}. Make sure Sokybot backend is running.</p>
        <p className="text-sm text-muted-foreground max-w-md text-center">
          If using Docker, ensure the backend container is up and Karaf has finished starting (RSocket on port 8182).
          If you open via sokybot.local, ensure api.sokybot.local resolves to the same host and port 8182 is reachable.
        </p>
        <Button type="button" onClick={() => connectToBackend()}>
          Retry connection
        </Button>
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
      const page = extensionRegistry.pages[selectedPageId] as {
        pageId: string;
        componentType?: string;
        props?: Record<string, unknown>;
      };
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
      <div className="flex flex-1 min-h-0 items-center justify-center px-4 md:px-6 text-sm text-muted-foreground text-center">
        Select a machine or log page from the sidebar to view details
      </div>
    );
  };

  return (
    <Layout isBackendConnected={isConnected}>
      <ErrorBoundary>
        <div className="h-full min-h-0 min-w-0 flex flex-col">
          {renderMainContent()}
        </div>
      </ErrorBoundary>
    </Layout>
  )
}

export default App
