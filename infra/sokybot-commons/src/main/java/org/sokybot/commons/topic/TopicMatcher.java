package org.sokybot.commons.topic;

public interface TopicMatcher {
    TopicMatcher DEFAULT = new DefaultTopicMatcher();

    boolean matches(Topic pattern, Topic topic);

    default boolean matches(String pattern, String topic) {
        return matches(Topic.parse(pattern), Topic.parse(topic));
    }
}
