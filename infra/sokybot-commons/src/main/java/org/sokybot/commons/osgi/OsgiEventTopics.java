package org.sokybot.commons.osgi;

/**
 * OSGi Event Admin topic names are validated strictly; characters such as {@code '.'}
 * (common in machine full names {@code Group.Machine}) are rejected.
 * <p>
 * Sanitize path segments for topic strings only; keep the real machine id in event properties
 * ({@code machineId}, {@code fullName}, etc.).
 */
public final class OsgiEventTopics {

    private OsgiEventTopics() {
    }

    /**
     * One slash-separated segment for an {@link org.osgi.service.event.Event} topic.
     */
    public static String segment(String s) {
        if (s == null || s.isEmpty()) {
            return "_";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    public static String networkTopic(String machineId, String transition) {
        return "sokybot/network/" + segment(machineId) + "/" + segment(transition);
    }

    public static String gameTopic(String machineId, String eventSimpleName) {
        return "sokybot/game/" + segment(machineId) + "/" + segment(eventSimpleName);
    }
}
