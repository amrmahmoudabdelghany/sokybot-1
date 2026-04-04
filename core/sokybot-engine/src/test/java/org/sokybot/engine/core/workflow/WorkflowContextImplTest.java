package org.sokybot.engine.core.workflow;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.sokybot.engine.test.util.mocks.MockDispatcher;
import org.sokybot.engine.test.util.mocks.MockGameModel;
import org.sokybot.engine.test.util.mocks.MockProxyConnection;

class WorkflowContextImplTest {

    @Test
    void getProxyConnection_returnsConstructorInstance() {
        MockGameModel gm = new MockGameModel();
        MockDispatcher disp = new MockDispatcher();
        MockProxyConnection proxy = new MockProxyConnection();
        BundleContext bc = Mockito.mock(BundleContext.class);

        WorkflowContextImpl ctx = new WorkflowContextImpl(
                gm, disp, proxy, "g", "m", bc);

        assertSame(proxy, ctx.getProxyConnection());
        assertSame(disp, ctx.getDispatcher());
    }
}
