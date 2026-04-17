package org.sokybot.commons.topic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TopicMatcherTest {
    @Test
    void shouldMatchDoubleStarWildcard() {
        assertTrue(TopicMatcher.DEFAULT.matches(Topic.parse("sokybot.game.**"), Topic.parse("sokybot.game.A.B.C")));
    }

    @Test
    void shouldMatchSingleSegmentWildcard() {
        assertTrue(TopicMatcher.DEFAULT.matches(Topic.parse("sokybot.*.machine.Event"), Topic.parse("sokybot.game.machine.Event")));
        assertFalse(TopicMatcher.DEFAULT.matches(Topic.parse("sokybot.*.Event"), Topic.parse("sokybot.game.machine.Event")));
    }

    @Test
    void shouldNormalizeSlashAndDotSyntax() {
        assertTrue(TopicMatcher.DEFAULT.matches("sokybot/game/**", "sokybot.game.machine.Event"));
        assertTrue(TopicMatcher.DEFAULT.matches("sokybot.game.**", "sokybot/game/machine/Event"));
    }
}
