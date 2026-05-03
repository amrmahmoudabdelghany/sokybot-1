package org.sokybot.router.constraints;

import org.sokybot.router.domain.FieldItemEntity;
import org.sokybot.router.domain.SwarmBotEntity;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

/**
 * Epic #24 Silk Road Router: capacity, single mule, farmer value, and transfer penalty.
 */
public final class RouterConstraintProvider implements ConstraintProvider {

    private static final Boolean GLOBAL_KEY = Boolean.TRUE;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                capacityLimit(factory),
                maximumOneMule(factory),
                minimumOneMule(factory),
                maximizeFarmerValue(factory),
                minimizeTransfers(factory)
        };
    }

    private Constraint capacityLimit(ConstraintFactory factory) {
        return factory.forEach(FieldItemEntity.class)
                .filter(item -> item.getAssignedBot() != null)
                .groupBy(FieldItemEntity::getAssignedBot, ConstraintCollectors.sum(FieldItemEntity::getSlotsTaken))
                .filter((bot, totalSlots) -> totalSlots > bot.getMaxCapacity())
                .penalize(HardSoftScore.ONE_HARD, (bot, totalSlots) -> totalSlots - bot.getMaxCapacity())
                .asConstraint("Capacity limit");
    }

    private Constraint maximumOneMule(ConstraintFactory factory) {
        return factory.forEach(SwarmBotEntity.class)
                .filter(bot -> Boolean.TRUE.equals(bot.getIsMule()))
                .groupBy(bot -> GLOBAL_KEY, ConstraintCollectors.count())
                .filter((key, count) -> count > 1)
                .penalize(HardSoftScore.ONE_HARD, (key, count) -> count - 1)
                .asConstraint("Maximum one mule");
    }

    private Constraint minimumOneMule(ConstraintFactory factory) {
        return factory.forEach(SwarmBotEntity.class)
                .ifNotExists(
                        SwarmBotEntity.class,
                        Joiners.filtering((SwarmBotEntity b1, SwarmBotEntity muleBot) ->
                                Boolean.TRUE.equals(muleBot.getIsMule())))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Minimum one mule");
    }

    private Constraint maximizeFarmerValue(ConstraintFactory factory) {
        return factory.forEach(FieldItemEntity.class)
                .filter(item -> item.getAssignedBot() != null
                        && Boolean.FALSE.equals(item.getAssignedBot().getIsMule()))
                .reward(HardSoftScore.ONE_SOFT, item -> (int) item.getGoldValue())
                .asConstraint("Maximize farmer value");
    }

    private Constraint minimizeTransfers(ConstraintFactory factory) {
        return factory.forEach(FieldItemEntity.class)
                .filter(item -> item.getAssignedBot() != null
                        && !java.util.Objects.equals(
                                item.getAssignedBot().getMachineId(),
                                item.getOriginalBotMachineId()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Minimize transfers");
    }
}
