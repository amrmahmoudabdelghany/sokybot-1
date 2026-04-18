package org.sokybot.engine.internal.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.sokybot.engine.test.util.mocks.MockGameModel;
import org.sokybot.proxy.IProxyConnection;

class NetworkTransitionJournalTest {

    @Test
    void appendRetainsAtMostFiveEntries() {
        MockGameModel gameModel = new MockGameModel();
        IDispatcher dispatcher = Mockito.mock(IDispatcher.class);
        IProxyConnection proxy = Mockito.mock(IProxyConnection.class);
        WorkflowContextImpl ctx = new WorkflowContextImpl(gameModel, dispatcher, proxy, "g1", "m1", null);
        NetworkTransitionJournal journal = new NetworkTransitionJournal(ctx);

        for (int i = 0; i < 6; i++) {
            journal.append("t" + i, null, null, null);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) ctx.getPersistentData().get("networkTransitions");
        assertEquals(5, list.size());
        assertEquals("t1", list.get(0).get("transition"));
        assertEquals("t5", list.get(4).get("transition"));
    }
}
