package org.sokybot.engine.api.login;

import java.util.Objects;

/**
 * Immutable gateway credentials payload for login protocol emission.
 */
public final class GatewayCredentials {

    private final String username;
    private final String password;

    public GatewayCredentials(String username, String password) {
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GatewayCredentials)) {
            return false;
        }
        GatewayCredentials that = (GatewayCredentials) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toString() {
        return "GatewayCredentials{username='" + username + "', password=<redacted>}";
    }
}
