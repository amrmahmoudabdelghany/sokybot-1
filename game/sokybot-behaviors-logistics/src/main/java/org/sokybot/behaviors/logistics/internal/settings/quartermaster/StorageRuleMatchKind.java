package org.sokybot.behaviors.logistics.internal.settings.quartermaster;

/**
 * Epic #15 Grand Quartermaster: how a {@link StorageRule} matches item stacks.
 */
public enum StorageRuleMatchKind {

    ITEM_REF_ID,
    ITEM_CATEGORY,
    NAME_PATTERN,
    DEFAULT_FALLBACK
}
