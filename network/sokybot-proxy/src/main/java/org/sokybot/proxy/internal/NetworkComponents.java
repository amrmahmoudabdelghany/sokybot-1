package org.sokybot.proxy.internal;

import org.sokybot.security.Blowfish;
import org.sokybot.security.CRCSecurity;
import org.sokybot.security.CountSecurity;
import org.sokybot.security.IBlowfish;
import org.sokybot.security.ICRCSecurity;
import org.sokybot.security.ICountSecurity;

/**
 * Holds security and network component instances for a proxy connection.
 * These are shared across the pipeline handlers.
 */
public class NetworkComponents {

    private final IBlowfish blowfish;
    private final ICRCSecurity crcSecurity;
    private final ICountSecurity countSecurity;

    public NetworkComponents() {
        this.blowfish = new Blowfish();
        this.crcSecurity = new CRCSecurity();
        this.countSecurity = new CountSecurity();
    }

    public void reset() {
        // Instances are kept persistent to ensure Netty pipeline handlers
        // (which hold these references) remain in sync after reconnection.
        // Internal state is reset via re-configuration during handshake.
    }

    public IBlowfish getBlowfish() {
        return blowfish;
    }

    public ICRCSecurity getCrcSecurity() {
        return crcSecurity;
    }

    public ICountSecurity getCountSecurity() {
        return countSecurity;
    }
}
