package org.sokybot.behaviors.logistics.internal.quartermaster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.sokybot.behaviors.logistics.internal.settings.quartermaster.QuartermasterSettings;
import org.sokybot.behaviors.logistics.internal.settings.quartermaster.StorageRule;
import org.sokybot.behaviors.logistics.internal.settings.quartermaster.StorageRuleMatchKind;
import org.sokybot.storage.api.IGuildStorageSnapshot;
import org.sokybot.storage.api.StorageStack;

/**
 * Epic #15: pure planning — misplaced stacks and merge candidates from policy rules and slot→tab geometry.
 */
public final class QuartermasterSortPlanner {

    /** Stand-in max stack until catalogue exposes per-item stack caps in {@link StorageStack}. */
    public static final int ASSUMED_MAX_STACK = 250;

    public enum MoveKind {
        RELOCATE,
        MERGE
    }

    public static final class PlannedMove {

        public final MoveKind kind;
        public final int fromSlot;
        public final int toSlot;
        public final int itemRefId;
        public final int quantity;

        public PlannedMove(MoveKind kind, int fromSlot, int toSlot, int itemRefId, int quantity) {
            this.kind = kind;
            this.fromSlot = fromSlot;
            this.toSlot = toSlot;
            this.itemRefId = itemRefId;
            this.quantity = quantity;
        }
    }

    private QuartermasterSortPlanner() {
    }

    /**
     * Builds an ordered list of planned moves (relocates first, then merges). Caller applies up to a budget per tick.
     */
    public static List<PlannedMove> plan(IGuildStorageSnapshot snap, QuartermasterSettings settings) {
        List<PlannedMove> out = new ArrayList<>();
        if (snap == null || settings == null) {
            return out;
        }
        int maxTabs = Math.max(1, settings.getMaxTabs());
        int totalSlots = Math.max(1, snap.getTotalSlots());
        int slotsPerTab = Math.max(1, (totalSlots + maxTabs - 1) / maxTabs);

        List<StorageRule> rules = settings.getRules();
        List<StorageRule> ordered = new ArrayList<>();
        if (rules != null) {
            ordered.addAll(rules);
        }
        ordered.sort(Comparator.comparingInt(StorageRule::getPriority));

        List<StorageStack> stacks = snap.getStacks();
        if (stacks == null || stacks.isEmpty()) {
            return out;
        }

        Set<Integer> occupied = new HashSet<>();
        for (StorageStack s : stacks) {
            if (s != null && s.getQuantity() > 0 && s.getItemRefId() > 0) {
                occupied.add(Integer.valueOf(s.getSlotIndex()));
            }
        }

        for (StorageStack st : stacks) {
            if (st == null || st.getQuantity() <= 0 || st.getItemRefId() <= 0) {
                continue;
            }
            int curTab = tabForSlot(st.getSlotIndex(), slotsPerTab, maxTabs, totalSlots);
            int wantTab = resolveTargetTab(st.getItemRefId(), ordered, maxTabs);
            if (curTab != wantTab) {
                Optional<Integer> dest = firstFreeSlotInTab(wantTab, slotsPerTab, maxTabs, totalSlots, occupied);
                if (dest.isPresent()) {
                    int to = dest.get().intValue();
                    out.add(new PlannedMove(MoveKind.RELOCATE, st.getSlotIndex(), to, st.getItemRefId(), st.getQuantity()));
                    occupied.remove(Integer.valueOf(st.getSlotIndex()));
                    occupied.add(Integer.valueOf(to));
                }
            }
        }

        Map<String, List<StorageStack>> byTabItem = new HashMap<>();
        for (StorageStack st : stacks) {
            if (st == null || st.getQuantity() <= 0 || st.getItemRefId() <= 0) {
                continue;
            }
            int tab = tabForSlot(st.getSlotIndex(), slotsPerTab, maxTabs, totalSlots);
            String k = tab + ":" + st.getItemRefId();
            byTabItem.computeIfAbsent(k, x -> new ArrayList<>()).add(st);
        }
        for (List<StorageStack> group : byTabItem.values()) {
            if (group.size() < 2) {
                continue;
            }
            group.sort(Comparator.comparingInt(StorageStack::getQuantity));
            StorageStack donor = group.get(0);
            StorageStack target = group.get(group.size() - 1);
            if (donor.getSlotIndex() == target.getSlotIndex()) {
                continue;
            }
            if (donor.getQuantity() + target.getQuantity() <= ASSUMED_MAX_STACK) {
                out.add(new PlannedMove(
                        MoveKind.MERGE,
                        donor.getSlotIndex(),
                        target.getSlotIndex(),
                        target.getItemRefId(),
                        donor.getQuantity()));
            }
        }

        return out;
    }

    static int tabForSlot(int slotIndex, int slotsPerTab, int maxTabs, int totalSlots) {
        if (slotIndex < 0) {
            return 0;
        }
        int tab = slotIndex / slotsPerTab;
        return Math.min(Math.max(0, tab), Math.max(0, maxTabs - 1));
    }

    static Optional<Integer> firstFreeSlotInTab(
            int tabIndex, int slotsPerTab, int maxTabs, int totalSlots, Set<Integer> occupied) {
        int start = tabIndex * slotsPerTab;
        int end = Math.min(totalSlots, (tabIndex + 1) * slotsPerTab);
        for (int s = start; s < end; s++) {
            if (!occupied.contains(Integer.valueOf(s))) {
                return Optional.of(Integer.valueOf(s));
            }
        }
        return Optional.empty();
    }

    static int resolveTargetTab(int itemRefId, List<StorageRule> orderedRules, int maxTabs) {
        StorageRule fallback = null;
        for (StorageRule r : orderedRules) {
            if (r == null) {
                continue;
            }
            if (r.getKind() == StorageRuleMatchKind.DEFAULT_FALLBACK) {
                if (fallback == null || r.getPriority() < fallback.getPriority()) {
                    fallback = r;
                }
                continue;
            }
            if (ruleMatches(r, itemRefId)) {
                return clampTab(r.getTabIndex(), maxTabs);
            }
        }
        return fallback != null ? clampTab(fallback.getTabIndex(), maxTabs) : 0;
    }

    static boolean ruleMatches(StorageRule r, int itemRefId) {
        switch (r.getKind()) {
            case ITEM_REF_ID:
                return r.getItemRefId() == itemRefId;
            case ITEM_CATEGORY:
                try {
                    int line = Integer.parseInt(r.getItemCategoryOrLine().trim());
                    return line == itemRefId;
                } catch (NumberFormatException ex) {
                    return false;
                }
            case NAME_PATTERN:
                // Item display names are not present on StorageStack in this API — reserved for future catalogue join.
                return false;
            case DEFAULT_FALLBACK:
            default:
                return false;
        }
    }

    static int clampTab(int tabIndex, int maxTabs) {
        int mt = Math.max(1, maxTabs);
        if (tabIndex < 0) {
            return 0;
        }
        return Math.min(tabIndex, mt - 1);
    }

    /** Optional hook when item names become available on stacks or via catalogue service. */
    @SuppressWarnings("unused")
    static boolean nameMatches(String itemName, String regex) {
        if (itemName == null || regex == null || regex.trim().isEmpty()) {
            return false;
        }
        try {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(itemName.toLowerCase(Locale.ROOT))
                    .find();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
