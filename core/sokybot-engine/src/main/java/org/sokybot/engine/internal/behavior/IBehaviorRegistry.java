package org.sokybot.engine.internal.behavior;

import java.util.List;

import org.sokybot.engine.api.behavior.IBehavior;

interface IBehaviorRegistry {

    List<IBehavior<?>> forCycle(String cycleId);
}
