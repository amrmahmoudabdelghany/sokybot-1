package org.sokybot.persistence.internal;

import org.junit.jupiter.api.Test;
import org.sokybot.persistence.service.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PersistenceException.
 */
class PersistenceExceptionTest {

    @Test
    void testPersistenceExceptionWithMessage() {
        String message = "Test exception message";
        PersistenceException exception = new PersistenceException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testPersistenceExceptionWithMessageAndCause() {
        String message = "Test exception message";
        Throwable cause = new RuntimeException("Root cause");
        PersistenceException exception = new PersistenceException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testPersistenceExceptionWithNullMessage() {
        PersistenceException exception = new PersistenceException((String) null);
        
        assertNull(exception.getMessage());
    }

    @Test
    void testPersistenceExceptionWithNullCause() {
        String message = "Test message";
        PersistenceException exception = new PersistenceException(message, null);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testPersistenceExceptionIsRuntimeException() {
        PersistenceException exception = new PersistenceException("Test");
        
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testPersistenceExceptionStackTrace() {
        PersistenceException exception = new PersistenceException("Test", new IllegalStateException());
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }
}