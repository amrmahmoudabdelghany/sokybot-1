package org.sokybot.behaviors.logistics.quartermaster;

/**
 * Mutable coordination record for one {@code storageSessionId}.
 */
final class LockState {

    private volatile LockStatus status = LockStatus.IDLE;
    private volatile String grantedToken = "";
    private volatile long expiresAtMs;

    LockStatus getStatus() {
        return status;
    }

    void setStatus(LockStatus status) {
        this.status = status != null ? status : LockStatus.IDLE;
    }

    String getGrantedToken() {
        return grantedToken;
    }

    void setGrantedToken(String grantedToken) {
        this.grantedToken = grantedToken != null ? grantedToken : "";
    }

    long getExpiresAtMs() {
        return expiresAtMs;
    }

    void setExpiresAtMs(long expiresAtMs) {
        this.expiresAtMs = expiresAtMs;
    }
}
