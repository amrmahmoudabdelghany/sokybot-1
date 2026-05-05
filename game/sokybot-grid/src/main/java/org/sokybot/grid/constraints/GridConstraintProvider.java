package org.sokybot.grid.constraints;

import org.sokybot.grid.domain.GridBotEntity;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.*;

/**
 * Epic #25 Hunting Grid: node uniqueness, anchor proximity, and attack-range spacing.
 */
public final class GridConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                oneBotPerNode(factory),
                anchorProximity(factory),
                maximizeSpacing(factory)
        };
    }

    private double calculateDistance(GridBotEntity b1, GridBotEntity b2) {
        return Math.hypot(
                b1.getAssignedNode().getX() - b2.getAssignedNode().getX(),
                b1.getAssignedNode().getY() - b2.getAssignedNode().getY());
    }

    private Constraint oneBotPerNode(ConstraintFactory factory) {
        return factory.forEachUniquePair(GridBotEntity.class, Joiners.equal(GridBotEntity::getAssignedNode))
                .filter((bot1, bot2) -> bot1.getAssignedNode() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("One bot per node");
    }

    private Constraint anchorProximity(ConstraintFactory factory) {
        return factory.forEach(GridBotEntity.class)
                .filter(bot -> bot.getAssignedNode() != null && !bot.isBuffer())
                .join(GridBotEntity.class,
                        Joiners.filtering((bot, anchor) -> anchor.isBuffer() && anchor.getAssignedNode() != null))
                .filter((bot, anchor) -> calculateDistance(bot, anchor) > 45.0)
                .penalize(HardSoftScore.ONE_HARD,
                        (bot, anchor) -> (int) Math.ceil(calculateDistance(bot, anchor) - 45.0))
                .asConstraint("Anchor proximity");
    }

    private Constraint maximizeSpacing(ConstraintFactory factory) {
        return factory.forEachUniquePair(GridBotEntity.class)
                .filter((bot1, bot2) -> bot1.getAssignedNode() != null && bot2.getAssignedNode() != null)
                .filter((bot1, bot2) -> {
                    double dist = calculateDistance(bot1, bot2);
                    double minRange = Math.min(bot1.getAttackRange(), bot2.getAttackRange());
                    return dist < minRange;
                })
                .penalize(HardSoftScore.ONE_SOFT, (bot1, bot2) -> {
                    double dist = calculateDistance(bot1, bot2);
                    double minRange = Math.min(bot1.getAttackRange(), bot2.getAttackRange());
                    return (int) Math.ceil(minRange - dist);
                })
                .asConstraint("Maximize spacing");
    }
}
