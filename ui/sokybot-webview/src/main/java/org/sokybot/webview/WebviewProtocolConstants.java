package org.sokybot.webview;

/**
 * Shared RSocket JSON protocol version for webview UI ↔ backend alignment.
 * <p>
 * Bump when introducing breaking SETUP or bootstrap contract changes; keep in sync with frontend
 * {@code SOKYBOT_PROTOCOL_API_VERSION}.
 */
public final class WebviewProtocolConstants {

    /** Server-supported protocol API level (integer, monotonic). */
    public static final int PROTOCOL_API_VERSION = 1;

    /** JSON key in SETUP payload: client declares minimum server API level required. */
    public static final String SETUP_KEY_MIN_PROTOCOL_API = "minProtocolApi";

    /** JSON key in SETUP payload: UI build identifier (git SHA, CI id, or "dev"). */
    public static final String SETUP_KEY_UI_BUILD_ID = "uiBuildId";

    private WebviewProtocolConstants() {
    }
}
