package org.sokybot.behaviors.training.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sokybot.behaviors.training.api.TrainingSettings;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.gamemodel.model.ITrainer;

class PotionBehaviorTest {

    @Test
    void appliesShouldReturnTrueWhenLowHpAndPotionExists() {
        PotionBehavior behavior = new PotionBehavior();
        TrainingSettings settings = new TrainingSettings();
        settings.setHpPotionThreshold(60);
        settings.setUseHpPotion(true);

        IWorkflowContext context = contextWith(
                trainerProxy(100, 40, 100, 100),
                Collections.singletonList(itemProxy((byte) 1, 12345, "ITEM_HP_POTION_X")),
                Mockito.mock(IDispatcher.class));

        assertTrue(behavior.applies(context, settings));
    }

    @Test
    void executeShouldDispatchPotionPacket() {
        PotionBehavior behavior = new PotionBehavior();
        TrainingSettings settings = new TrainingSettings();
        settings.setHpPotionThreshold(60);
        settings.setUseHpPotion(true);

        IDispatcher dispatcher = Mockito.mock(IDispatcher.class);
        IWorkflowContext context = contextWith(
                trainerProxy(100, 30, 100, 100),
                Arrays.asList(itemProxy((byte) 3, 9001, "ITEM_HP_POTION_Y"), itemProxy((byte) 9, 2002, "ITEM_MP_POTION_Z")),
                dispatcher);

        BehaviorStatus status = behavior.execute(context, settings);
        assertEquals(BehaviorStatus.EXECUTED, status);
        verify(dispatcher, times(1)).sendToServer(any());
    }

    private static IWorkflowContext contextWith(ITrainer trainer, java.util.List<IItem> items, IDispatcher dispatcher) {
        IGameModel gameModel = (IGameModel) Proxy.newProxyInstance(
                IGameModel.class.getClassLoader(),
                new Class<?>[] { IGameModel.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getTrainer".equals(name)) {
                        return trainer;
                    }
                    if ("snapshotAll".equals(name)) {
                        return items;
                    }
                    if ("find".equals(name)) {
                        return java.util.Optional.empty();
                    }
                    return defaultValue(method.getReturnType());
                });

        return (IWorkflowContext) Proxy.newProxyInstance(
                IWorkflowContext.class.getClassLoader(),
                new Class<?>[] { IWorkflowContext.class },
                (proxy, method, args) -> {
                    if ("getGameModel".equals(method.getName())) {
                        return gameModel;
                    }
                    if ("getDispatcher".equals(method.getName())) {
                        return dispatcher;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static ITrainer trainerProxy(int maxHp, int currentHp, int maxMp, int currentMp) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("getMaxHP", maxHp);
        values.put("getCurrentHP", currentHp);
        values.put("getMaxMP", maxMp);
        values.put("getCurrentMP", currentMp);
        return (ITrainer) Proxy.newProxyInstance(
                ITrainer.class.getClassLoader(),
                new Class<?>[] { ITrainer.class },
                (proxy, method, args) -> values.getOrDefault(method.getName(), defaultValue(method.getReturnType())));
    }

    private static IItem itemProxy(byte slot, int refId, String longId) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("getSlot", slot);
        values.put("getRefId", refId);
        values.put("getLongId", longId);
        return (IItem) Proxy.newProxyInstance(
                IItem.class.getClassLoader(),
                new Class<?>[] { IItem.class },
                (proxy, method, args) -> values.getOrDefault(method.getName(), defaultValue(method.getReturnType())));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0f;
        }
        if (returnType == Double.TYPE) {
            return 0d;
        }
        if (returnType == java.util.Optional.class) {
            return java.util.Optional.empty();
        }
        return null;
    }
}
