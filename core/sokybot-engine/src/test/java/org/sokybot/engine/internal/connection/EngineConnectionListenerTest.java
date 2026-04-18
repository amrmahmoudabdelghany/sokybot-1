package org.sokybot.engine.internal.connection;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.sokybot.engine.internal.cycle.ICycleController;
import org.sokybot.engine.internal.journal.INetworkTransitionJournal;
import org.sokybot.engine.test.util.mocks.MockGameModel;

class EngineConnectionListenerTest {

    @Test
    void onAuthenticatedAppendsAndReconciles() {
        INetworkTransitionJournal journal = mock(INetworkTransitionJournal.class);
        ICycleController cycle = mock(ICycleController.class);
        MockGameModel gameModel = new MockGameModel();
        Runnable clear = mock(Runnable.class);

        EngineConnectionListener listener = new EngineConnectionListener("g.m", journal, cycle, gameModel, clear);

        listener.onAuthenticated();

        verify(journal).append(eq("Authenticated"), eq("AUTHENTICATED"), isNull(), isNull());
        verify(cycle).reconcileAfterAuthentication();
    }

    @Test
    void onDisconnectedClearsSession() {
        INetworkTransitionJournal journal = mock(INetworkTransitionJournal.class);
        ICycleController cycle = mock(ICycleController.class);
        MockGameModel gameModel = new MockGameModel();
        Runnable clear = mock(Runnable.class);

        EngineConnectionListener listener = new EngineConnectionListener("g.m", journal, cycle, gameModel, clear);

        listener.onDisconnected(null);

        verify(journal).append(eq("Disconnected"), eq("DISCONNECTED"), isNull(), isNull());
        verify(clear).run();
    }
}
