package org.sokybot.engine.api.login;

import java.util.Optional;

/**
 * Built-in mapping for gateway 0xA102 failure bytes (used by the default
 * {@link IGatewayErrorPolicy} and by {@link GatewayErrorClassifier} when the
 * whiteboard is not populated).
 */
public final class GatewayLoginErrorDefaults {

    private GatewayLoginErrorDefaults() {
    }

    /**
     * Maps known gateway failure bytes to classifications; unknown codes yield empty.
     *
     * @param gatewayCode raw gateway result (masked to 8 bits internally)
     */
    public static Optional<GatewayFailureClassification> classify(int gatewayCode) {
        int c = gatewayCode & 0xFF;
        switch (c) {
        case 1:
        case 2:
        case 0x0B:
        case 0x0C:
        case 0x0D:
            return Optional.of(GatewayFailureClassification.CREDENTIAL);
        case 4:
            return Optional.of(GatewayFailureClassification.GHOST_COOLDOWN);
        case 6:
            return Optional.of(GatewayFailureClassification.NETWORK);
        default:
            return Optional.empty();
        }
    }
}
