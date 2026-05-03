package org.sokybot.behaviors.logistics.internal.settings.quartermaster;

import lombok.Data;

/**
 * Epic #15: one routing rule for guild/account storage tabs.
 */
@Data
public class StorageRule {

    private int priority = 100;

    private StorageRuleMatchKind kind = StorageRuleMatchKind.DEFAULT_FALLBACK;

    private int itemRefId = 0;

    private String itemCategoryOrLine = "";

    private String nameRegex = "";

    private int tabIndex = 0;
}
