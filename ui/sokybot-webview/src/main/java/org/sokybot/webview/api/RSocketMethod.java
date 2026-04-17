package org.sokybot.webview.api;

import java.util.Objects;

/**
 * Method token with optional handler ranking metadata.
 */
public final class RSocketMethod {
    private final String name;
    private final int ranking;

    private RSocketMethod(String name, int ranking) {
        this.name = Objects.requireNonNull(name, "name");
        this.ranking = ranking;
    }

    public static RSocketMethod of(String name) {
        return new RSocketMethod(name, 0);
    }

    public static RSocketMethod of(String name, int ranking) {
        return new RSocketMethod(name, ranking);
    }

    public String getName() {
        return name;
    }

    public int getRanking() {
        return ranking;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RSocketMethod)) {
            return false;
        }
        RSocketMethod that = (RSocketMethod) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
