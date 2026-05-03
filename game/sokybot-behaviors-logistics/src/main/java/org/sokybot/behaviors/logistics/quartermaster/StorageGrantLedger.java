package org.sokybot.behaviors.logistics.quartermaster;

import java.util.concurrent.ConcurrentHashMap;

/**
 * JVM-local latch so farmers in {@link org.sokybot.behaviors.logistics.StorageDumpBehavior} WAIT phase can observe granted
 * tokens without prototype-scoped reactive subscriptions. Updated by {@link QuartermasterCoordinator} atomically with publish.
 */
public final class StorageGrantLedger {

    private static final ConcurrentHashMap<String, String> SESSION_TO_TOKEN = new ConcurrentHashMap<>();

    private StorageGrantLedger() {
    }

    public static void offerGrant(String storageSessionId, String grantedToken) {
        if (storageSessionId == null || grantedToken == null) {
            return;
        }
        String sid = storageSessionId.trim();
        String tok = grantedToken.trim();
        if (sid.isEmpty() || tok.isEmpty()) {
            return;
        }
        SESSION_TO_TOKEN.put(sid, tok);
    }

    public static String peekToken(String storageSessionId) {
        if (storageSessionId == null) {
            return null;
        }
        return SESSION_TO_TOKEN.get(storageSessionId.trim());
    }

    public static void clearSession(String storageSessionId) {
        if (storageSessionId != null) {
            SESSION_TO_TOKEN.remove(storageSessionId.trim());
        }
    }
}
