package org.sokybot.engine.api.login;

import java.util.Objects;

/**
 * Immutable classification output used by retry/terminal failure flow.
 */
public final class LoginFailureVerdict {

    private final LoginFailureClass failureClass;
    private final Integer gatewayCode;
    private final String detail;
    private final boolean terminal;
    private final long retryDelayFloorMs;
    private final boolean haltRequired;

    public LoginFailureVerdict(
            LoginFailureClass failureClass,
            Integer gatewayCode,
            String detail,
            boolean terminal,
            long retryDelayFloorMs,
            boolean haltRequired
    ) {
        this.failureClass = failureClass != null ? failureClass : LoginFailureClass.UNKNOWN_RETRY;
        this.gatewayCode = gatewayCode;
        this.detail = detail != null ? detail : "";
        this.terminal = terminal;
        this.retryDelayFloorMs = retryDelayFloorMs;
        this.haltRequired = haltRequired;
    }

    public LoginFailureClass getFailureClass() {
        return failureClass;
    }

    public Integer getGatewayCode() {
        return gatewayCode;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public long getRetryDelayFloorMs() {
        return retryDelayFloorMs;
    }

    public boolean isHaltRequired() {
        return haltRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginFailureVerdict)) {
            return false;
        }
        LoginFailureVerdict that = (LoginFailureVerdict) o;
        return terminal == that.terminal
                && retryDelayFloorMs == that.retryDelayFloorMs
                && haltRequired == that.haltRequired
                && failureClass == that.failureClass
                && Objects.equals(gatewayCode, that.gatewayCode)
                && Objects.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(failureClass, gatewayCode, detail, terminal, retryDelayFloorMs, haltRequired);
    }

    @Override
    public String toString() {
        return "LoginFailureVerdict{"
                + "failureClass=" + failureClass
                + ", gatewayCode=" + gatewayCode
                + ", detail='" + detail + '\''
                + ", terminal=" + terminal
                + ", retryDelayFloorMs=" + retryDelayFloorMs
                + ", haltRequired=" + haltRequired
                + '}';
    }
}
