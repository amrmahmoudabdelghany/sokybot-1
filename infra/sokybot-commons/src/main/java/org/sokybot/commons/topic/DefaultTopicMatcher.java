package org.sokybot.commons.topic;

import java.util.List;

public final class DefaultTopicMatcher implements TopicMatcher {
    @Override
    public boolean matches(Topic pattern, Topic topic) {
        if (pattern == null || topic == null) {
            return true;
        }
        List<String> p = pattern.getSegments();
        List<String> t = topic.getSegments();
        return matchesSegments(p, 0, t, 0);
    }

    private boolean matchesSegments(List<String> pattern, int pIndex, List<String> topic, int tIndex) {
        while (pIndex < pattern.size()) {
            String segment = pattern.get(pIndex);
            if ("**".equals(segment)) {
                if (pIndex == pattern.size() - 1) {
                    return true;
                }
                for (int i = tIndex; i <= topic.size(); i++) {
                    if (matchesSegments(pattern, pIndex + 1, topic, i)) {
                        return true;
                    }
                }
                return false;
            }
            if (tIndex >= topic.size()) {
                return false;
            }
            if (!"*".equals(segment) && !segment.equals(topic.get(tIndex))) {
                return false;
            }
            pIndex++;
            tIndex++;
        }
        return tIndex == topic.size();
    }
}
