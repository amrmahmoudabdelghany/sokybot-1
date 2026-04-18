package org.sokybot.login.policies.builtin;

final class LoginFailureReasonHeuristics {

    private LoginFailureReasonHeuristics() {
    }

    static boolean isCredentialReason(String reasonLowerCase) {
        return containsAny(reasonLowerCase,
                "password",
                "invalid account",
                "invalid credentials",
                "auth failed",
                "rejected",
                "denied");
    }

    static boolean isGhostCooldownReason(String reasonLowerCase) {
        return containsAny(reasonLowerCase, "already connected");
    }

    static boolean isCharacterNotFoundReason(String reasonLowerCase) {
        return containsAny(reasonLowerCase, "character not found", "character_selection_unavailable");
    }

    static boolean isServerInspectionReason(String reasonLowerCase) {
        return containsAny(reasonLowerCase, "inspection", "maintenance");
    }

    static boolean isNetworkReason(String reasonLowerCase) {
        return containsAny(reasonLowerCase,
                "timeout",
                "disconnect",
                "unreachable",
                "refused",
                "network");
    }

    static boolean isAgentBanReason(String reasonLowerCase) {
        return containsAny(reasonLowerCase, "too many requests", "6110");
    }

    static Integer parseGatewayFailureCode(String reason) {
        if (reason == null || reason.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)\\bcode\\s+(\\d+)\\b").matcher(reason);
        if (matcher.find()) {
            try {
                return Integer.valueOf(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean containsAny(String reasonLowerCase, String... needles) {
        if (reasonLowerCase == null || reasonLowerCase.isEmpty()) {
            return false;
        }
        for (String needle : needles) {
            if (reasonLowerCase.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
