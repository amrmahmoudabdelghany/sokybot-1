package org.sokybot.gameevents.framework;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.ChunkedPacketManagerRegistry;

/**
 * Helper class for testing chunked packet translators.
 * Manages chunk manager registration/unregistration for tests.
 */
public class ChunkedPacketManagerTestHelper {
    
    private final String machineName;
    private final ChunkedPacketManager chunkManager;
    private boolean registered = false;
    
    /**
     * Create a helper for a specific machine.
     */
    public ChunkedPacketManagerTestHelper(String machineName) {
        this.machineName = machineName;
        this.chunkManager = new ChunkedPacketManager();
    }
    
    /**
     * Register the chunk manager in the registry.
     */
    public ChunkedPacketManagerTestHelper register() {
        if (!registered) {
            ChunkedPacketManagerRegistry.getInstance().register(machineName, chunkManager);
            registered = true;
        }
        return this;
    }
    
    /**
     * Unregister the chunk manager from the registry.
     */
    public void unregister() {
        if (registered) {
            ChunkedPacketManagerRegistry.getInstance().unregister(machineName);
            registered = false;
        }
    }
    
    /**
     * Get the chunk manager.
     */
    public ChunkedPacketManager getChunkManager() {
        return chunkManager;
    }
    
    /**
     * Assert that a transaction is active for the given begin opcode.
     */
    public void assertTransactionActive(int beginOpcode) {
        assertNotNull(chunkManager, "Chunk manager should not be null");
        assertTrue(chunkManager.isActive(beginOpcode), 
                  "Transaction should be active for opcode 0x" + Integer.toHexString(beginOpcode));
    }
    
    /**
     * Assert that no transaction is active for the given begin opcode.
     */
    public void assertTransactionNotActive(int beginOpcode) {
        assertNotNull(chunkManager, "Chunk manager should not be null");
        assertFalse(chunkManager.isActive(beginOpcode), 
                   "Transaction should not be active for opcode 0x" + Integer.toHexString(beginOpcode));
    }
    
    /**
     * Assert that chunks have been accumulated for a transaction.
     */
    public void assertHasChunks(int beginOpcode) {
        assertTransactionActive(beginOpcode);
        // Note: ChunkedPacketManager doesn't expose chunk count directly
        // This is a placeholder for future enhancement
    }
    
    /**
     * AutoCloseable support for try-with-resources.
     */
    public static class AutoCloseableHelper implements AutoCloseable {
        private final ChunkedPacketManagerTestHelper helper;
        
        public AutoCloseableHelper(ChunkedPacketManagerTestHelper helper) {
            this.helper = helper;
            helper.register();
        }
        
        public ChunkedPacketManager getChunkManager() {
            return helper.getChunkManager();
        }
        
        @Override
        public void close() {
            helper.unregister();
        }
    }
    
    /**
     * Create an auto-closeable wrapper for try-with-resources.
     */
    public AutoCloseableHelper autoCloseable() {
        return new AutoCloseableHelper(this);
    }
}
