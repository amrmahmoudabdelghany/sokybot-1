package org.sokybot.runtime;

import java.util.regex.Pattern;

/**
 * Validates group and machine names so {@code fullName} ({@code group.machine}) cannot contain ambiguous
 * delimiters. Allowed: letters, digits, underscore, hyphen.
 */
public final class RuntimeEntityNames {

    private static final Pattern VALID = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private RuntimeEntityNames() {
    }

    public static boolean isValid(String name) {
        return name != null && !name.isBlank() && VALID.matcher(name.trim()).matches();
    }

    public static String validateGroupOrThrow(String groupName) {
        if (!isValid(groupName)) {
            throw new IllegalArgumentException(
                    "Invalid group name: use only letters, digits, underscore, hyphen (regex ^[a-zA-Z0-9_-]+$): "
                            + groupName);
        }
        return groupName.trim();
    }

    public static String validateMachineOrThrow(String machineName) {
        if (!isValid(machineName)) {
            throw new IllegalArgumentException(
                    "Invalid machine name: use only letters, digits, underscore, hyphen (regex ^[a-zA-Z0-9_-]+$): "
                            + machineName);
        }
        return machineName.trim();
    }
}
