package org.sokybot.engine.api.behavior;

import org.sokybot.engine.api.workflow.ICycleDefinition;

public interface IBehaviorCycleAssembler {

    <S> ICycleDefinition assemble(BehaviorCycleSpec<S> spec);
}
