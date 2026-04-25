import { useState, useEffect } from 'react'
import { useRSocketService } from './RSocketProvider'
import { useSokybotStore } from './store'
import { Layout } from './Layout'
import { MachineView } from './MachineView'
import { registerComponentType } from './extensions/registry'
import { SocialPage } from './extensions/SocialPage'
import { useExtensionRegistry } from './extensions/ExtensionRegistry'
import { ExtensionView } from './extensions/ExtensionView'
import { ErrorBoundary } from './ErrorBoundary'
import { useSocialAlerts } from './hooks/useSocialAlerts'
import {
  useMachinesQuery,
  useGroupsQuery,
  useExtensionRegistryQuery,
} from './query/sokybotQueries'
import { extensionUiEventDataSchema } from './schemas/extensionEvents'
import { Button } from '@sokybot/frontend-shared'
import './App.css'

registerComponentType('social', SocialPage)

function App() {
  const rsocketService = useRSocketService();
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
      const incoming = extensionRegistryQuery.data as {
        pages: Record<string, unknown>;
        toolbarActions: Record<string, unknown>;
      };
      const currentPages = useSokybotStore.getState().extensionRegistry.pages;
      const socialPages: Record<string, unknown> = {};
      Object.keys(currentPages).forEach((pid) => {
        if (pid.startsWith('social_')) {
          socialPages[pid] = currentPages[pid];
        }
      });
      setExtensionRegistry({
        pages: { ...incoming.pages, ...socialPages },
        toolbarActions: incoming.toolbarActions,
      });
    }
  }, [extensionRegistryQuery.data, setExtensionRegistry]);

  const connectToBackend = async () => {
    setError(null);
    try {
      await rsocketService.connect();
    } catch (err: unknown) {
      console.error("Failed to connect to RSocket", err);
      const msg = err instanceof Error ? err.message : "Connection closed";
      setError(msg.includes("Connection closed") ? "Connection closed" : msg);
    }
  };

  useEffect(() => {
    const unsubscribe = rsocketService.onConnectionStateChange((state) => {
      setIsConnected(state === 'connected');
      if (state === 'connecting') {
        setError(null);
      }
    });
    connectToBackend();
    return unsubscribe;
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

  useEffect(() => {
    if (!isConnected) return;
    void rsocketService.notifyClientConnected().catch(() => {});
  }, [isConnected, rsocketService]);

  useSocialAlerts(isConnected);

  useEffect(() => {
    if (!isConnected || machinesQuery.data === undefined) {
      return;
    }
    const machines = machinesQuery.data;
    const alive = new Set(machines.map((m) => m.machineId));
    const pages = useSokybotStore.getState().extensionRegistry.pages;
    Object.keys(pages).forEach((pid) => {
      if (!pid.startsWith('social_')) {
        return;
      }
      const mid = pid.slice('social_'.length);
      if (!alive.has(mid)) {
        removeExtensionPage(pid);
      }
    });
    if (machines.length === 0) {
      return;
    }
    machines.forEach((m) => {
      addExtensionPage({
        pageId: `social_${m.machineId}`,
        title: 'Social',
        componentType: 'social',
        props: {},
      });
    });
  }, [isConnected, machinesQuery.data, addExtensionPage, removeExtensionPage]);

  useEffect(() => {
    const onVis = () => {
      if (rsocketService.getConnectionState() !== 'connected') return;
      void rsocketService
        .fireAndForget('webview.client.visibility', { visibility: document.visibilityState })
        .catch(() => {});
    };
    document.addEventListener('visibilitychange', onVis);
    return () => document.removeEventListener('visibilitychange', onVis);
  }, [rsocketService]);

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

  const legacyWorkspaceBootstrap =
    isConnected &&
    machinesQuery.isSuccess &&
    groupsQuery.isSuccess &&
    rsocketService.getWorkspaceBootstrapMode() === 'legacy';

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
          {legacyWorkspaceBootstrap ? (
            <div
              className="shrink-0 border-b border-amber-500/40 bg-amber-500/10 px-3 py-2 text-center text-xs text-amber-100"
              role="status"
            >
              Workspace loaded via legacy API (group.list + machine.list). Deploy an updated sokybot-webview
              bundle so workspace.summary is available.
            </div>
          ) : null}
          {renderMainContent()}
        </div>
      </ErrorBoundary>
    </Layout>
  )
}

export default App
