package org.sokybot.town.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Ack-style outcome for NPC facade operations that complete asynchronously after server packets.
 */
public final class NpcInteractionResult {

    private final boolean success;
    private final String detailMessage;

    private NpcInteractionResult(Builder builder) {
        this.success = builder.success;
        this.detailMessage = builder.detailMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public Optional<String> getDetailMessage() {
        return Optional.ofNullable(detailMessage);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static NpcInteractionResult ok() {
        return builder().success(true).build();
    }

    public static NpcInteractionResult failure(String message) {
        return builder().success(false).detailMessage(message).build();
    }

    public static final class Builder {
        private boolean success;
        private String detailMessage;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder detailMessage(String detailMessage) {
            this.detailMessage = detailMessage;
            return this;
        }

        public NpcInteractionResult build() {
            return new NpcInteractionResult(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NpcInteractionResult)) {
            return false;
        }
        NpcInteractionResult that = (NpcInteractionResult) o;
        return success == that.success && Objects.equals(detailMessage, that.detailMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, detailMessage);
    }
}
