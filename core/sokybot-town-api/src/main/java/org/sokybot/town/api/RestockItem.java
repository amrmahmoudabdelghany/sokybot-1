package org.sokybot.town.api;

import java.util.Objects;

/**
 * Desired inventory level for one catalogue ref (template row for {@link RestockOrder} lines).
 */
public final class RestockItem {

    private final int itemRefId;
    private final int targetQuantity;

    private RestockItem(Builder builder) {
        this.itemRefId = builder.itemRefId;
        this.targetQuantity = builder.targetQuantity;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public int getTargetQuantity() {
        return targetQuantity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int itemRefId;
        private int targetQuantity;

        public Builder itemRefId(int itemRefId) {
            this.itemRefId = itemRefId;
            return this;
        }

        public Builder targetQuantity(int targetQuantity) {
            this.targetQuantity = targetQuantity;
            return this;
        }

        public RestockItem build() {
            return new RestockItem(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RestockItem)) {
            return false;
        }
        RestockItem that = (RestockItem) o;
        return itemRefId == that.itemRefId && targetQuantity == that.targetQuantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemRefId, targetQuantity);
    }
}
