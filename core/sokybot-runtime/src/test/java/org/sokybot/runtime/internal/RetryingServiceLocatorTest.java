package org.sokybot.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.sokybot.runtime.test.util.OSGiTestUtils;

class RetryingServiceLocatorTest {

    @Test
    void returnsNullWhenBundleContextNull() {
        Logger log = mock(Logger.class);
        assertNull(RetryingServiceLocator.get(null, Runnable.class, 2, 1L, log));
    }

    @Test
    void returnsServiceWhenRegistered() {
        Logger log = mock(Logger.class);
        OSGiTestUtils.MockBundleContext ctx = OSGiTestUtils.createMockBundleContext();
        Runnable svc = mock(Runnable.class);
        ctx.registerMockService(Runnable.class, svc, null);

        Runnable found = RetryingServiceLocator.get(ctx, Runnable.class, 3, 1L, log);

        assertNotNull(found);
    }
}
