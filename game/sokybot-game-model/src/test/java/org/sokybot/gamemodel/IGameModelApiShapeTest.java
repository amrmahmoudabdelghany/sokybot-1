package org.sokybot.gamemodel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class IGameModelApiShapeTest {

    @Test
    void shouldExposeSnapshotApiAndRemoveFindAll() {
        Method[] methods = IGameModel.class.getMethods();
        assertTrue(Arrays.stream(methods).anyMatch(m -> m.getName().equals("findLive")));
        assertTrue(Arrays.stream(methods).anyMatch(m -> m.getName().equals("snapshot")));
        assertTrue(Arrays.stream(methods).anyMatch(m -> m.getName().equals("snapshotAll")));
        assertFalse(Arrays.stream(methods).anyMatch(m -> m.getName().equals("findAll")));
    }
}
