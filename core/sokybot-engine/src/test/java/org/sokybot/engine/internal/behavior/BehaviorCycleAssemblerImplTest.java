package org.sokybot.engine.internal.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.behavior.BehaviorCycleSpec;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.ICycleDefinition;
import org.sokybot.engine.api.workflow.IDelayState;
import org.sokybot.engine.api.workflow.StateId;

class BehaviorCycleAssemblerImplTest {

    @Test
    void assemblerShouldEmitTriadStatesAndCompositeInterruptionGuard() {
        BehaviorCycleAssemblerImpl assembler = new BehaviorCycleAssemblerImpl();
        assembler.setBehaviorRegistry(cycleId -> Arrays.asList(
                new StubBehavior("combat", 20, false, 0, false),
                new StubBehavior("potion", 10, true, 500, true)));

        BehaviorCycleSpec<String> spec = BehaviorCycleSpec.builder(String.class)
                .cycleId("training-cycle")
                .priority(300)
                .settingsSupplier(() -> "ok")
                .build();

        ICycleDefinition cycle = assembler.assemble(spec);
        List<String> stateNames = Arrays.asList(
                cycle.getStates().get(0).getStateId().asString(),
                cycle.getStates().get(1).getStateId().asString(),
                cycle.getStates().get(2).getStateId().asString(),
                cycle.getStates().get(3).getStateId().asString());

        assertEquals(Arrays.asList("CHECK_COMBAT", "WAIT_AFTER_COMBAT", "CHECK_POTION", "WAIT_AFTER_POTION"), stateNames);
        assertEquals("CHECK_COMBAT", cycle.getEntryState().asString());
        assertEquals("WAIT_AFTER_COMBAT", cycle.getState(StateId.of("CHECK_COMBAT")).getNextState().asString());
        assertEquals("CHECK_POTION", cycle.getState(StateId.of("CHECK_COMBAT")).getTargetState().asString());
        assertEquals("CHECK_COMBAT", cycle.getState(StateId.of("CHECK_POTION")).getTargetState().asString());
        assertEquals("CHECK_POTION", cycle.getState(StateId.of("WAIT_AFTER_COMBAT")).getNextState().asString());
        assertEquals(500, cycle.getState(StateId.of("WAIT_AFTER_POTION")).getAs(IDelayState.class).getDelayMs());
        assertEquals(300, cycle.getInterruptionPriority());
        assertTrue(cycle.getInterruptionGuard().evaluate(new NoopWorkflowContext()));
    }

    @Test
    void assemblerShouldFailWhenNoBehaviorsBound() {
        BehaviorCycleAssemblerImpl assembler = new BehaviorCycleAssemblerImpl();
        assembler.setBehaviorRegistry(cycleId -> Collections.emptyList());

        BehaviorCycleSpec<String> spec = BehaviorCycleSpec.builder(String.class)
                .cycleId("training-cycle")
                .priority(300)
                .settingsSupplier(() -> "ok")
                .build();

        assertThrows(IllegalStateException.class, () -> assembler.assemble(spec));
    }

    private static final class StubBehavior implements IBehavior<String> {
        private final String id;
        private final int order;
        private final boolean canInterrupt;
        private final long delayMs;
        private final boolean applies;

        private StubBehavior(String id, int order, boolean canInterrupt, long delayMs, boolean applies) {
            this.id = id;
            this.order = order;
            this.canInterrupt = canInterrupt;
            this.delayMs = delayMs;
            this.applies = applies;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public Class<String> settingsType() {
            return String.class;
        }

        @Override
        public boolean applies(org.sokybot.engine.api.workflow.IWorkflowContext context, String settings) {
            return applies;
        }

        @Override
        public BehaviorStatus execute(org.sokybot.engine.api.workflow.IWorkflowContext context, String settings) {
            return BehaviorStatus.EXECUTED;
        }

        @Override
        public long postDelayMs() {
            return delayMs;
        }

        @Override
        public boolean canInterrupt() {
            return canInterrupt;
        }
    }

    private static final class NoopWorkflowContext implements org.sokybot.engine.api.workflow.IWorkflowContext {
        @Override
        public org.sokybot.gamemodel.IGameModel getGameModel() {
            return null;
        }

        @Override
        public org.sokybot.engine.api.IDispatcher getDispatcher() {
            return null;
        }

        @Override
        public org.sokybot.proxy.IProxyConnection getProxyConnection() {
            return null;
        }

        @Override
        public String getCurrentStateName() {
            return null;
        }

        @Override
        public java.util.Map<String, Object> getStateData() {
            return java.util.Collections.emptyMap();
        }

        @Override
        public java.util.Map<String, Object> getPersistentData() {
            return java.util.Collections.emptyMap();
        }

        @Override
        public void log(String level, String message, Object... args) {
        }

        @Override
        public String getMachineId() {
            return "m1";
        }

        @Override
        public String getGroupName() {
            return "g1";
        }

        @Override
        public String getMachineName() {
            return "m1";
        }

        @Override
        public <T> T getService(Class<T> serviceClass) {
            return null;
        }

        @Override
        public <T> java.util.Optional<T> getServiceOptional(Class<T> serviceClass) {
            return java.util.Optional.empty();
        }
    }
}
