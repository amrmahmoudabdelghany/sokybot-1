package org.sokybot.town.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Planned purchase amounts for the next vendor visit.
 */
public final class RestockOrder {

    private final List<RestockItem> lines;
    private final VendorRef preferredVendor;

    private RestockOrder(Builder builder) {
        this.lines = Collections.unmodifiableList(new ArrayList<>(builder.lines));
        this.preferredVendor = builder.preferredVendor;
    }

    public List<RestockItem> getLines() {
        return lines;
    }

    public Optional<VendorRef> getPreferredVendor() {
        return Optional.ofNullable(preferredVendor);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<RestockItem> lines = new ArrayList<>();
        private VendorRef preferredVendor;

        public Builder addLine(RestockItem line) {
            if (line != null) {
                this.lines.add(line);
            }
            return this;
        }

        public Builder lines(List<RestockItem> lines) {
            this.lines.clear();
            if (lines != null) {
                this.lines.addAll(lines);
            }
            return this;
        }

        public Builder preferredVendor(VendorRef preferredVendor) {
            this.preferredVendor = preferredVendor;
            return this;
        }

        public RestockOrder build() {
            return new RestockOrder(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RestockOrder)) {
            return false;
        }
        RestockOrder that = (RestockOrder) o;
        return lines.equals(that.lines) && Objects.equals(preferredVendor, that.preferredVendor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lines, preferredVendor);
    }
}
