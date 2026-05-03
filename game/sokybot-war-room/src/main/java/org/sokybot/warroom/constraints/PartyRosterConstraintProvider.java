package org.sokybot.warroom.constraints;

import java.util.function.Function;

import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;
import org.sokybot.warroom.domain.SwarmBotEntity;
import org.sokybot.warroom.domain.WarRoomParty;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

/**
 * Epic #23 War Room: roster feasibility (capacity, level spread, role quotas) and DPS objective.
 */
public final class PartyRosterConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                maxCapacity(factory),
                levelGap(factory),
                minimumOneHealer(factory),
                maximumOneHealer(factory),
                minimumOneBuffer(factory),
                maximumOneBuffer(factory),
                maximizeDps(factory)
        };
    }

    private Constraint maxCapacity(ConstraintFactory factory) {
        return factory.forEach(SwarmBotEntity.class)
                .filter(bot -> bot.getAssignedParty() != null)
                .groupBy(SwarmBotEntity::getAssignedParty, ConstraintCollectors.count())
                .filter((party, count) -> count > party.getCapacity())
                .penalize(HardSoftScore.ONE_HARD, (party, count) -> count - party.getCapacity())
                .asConstraint("Max capacity");
    }

    private Constraint levelGap(ConstraintFactory factory) {
        return factory.forEach(SwarmBotEntity.class)
                .filter(bot -> bot.getAssignedParty() != null)
                .groupBy(
                        SwarmBotEntity::getAssignedParty,
                        ConstraintCollectors.max(SwarmBotEntity::getLevel),
                        ConstraintCollectors.min(SwarmBotEntity::getLevel))
                .filter((party, maxLevel, minLevel) -> maxLevel - minLevel > 5)
                .penalize(HardSoftScore.ONE_HARD, (party, maxLevel, minLevel) -> maxLevel - minLevel - 5)
                .asConstraint("Level gap");
    }

    private Constraint maximumOneHealer(ConstraintFactory factory) {
        return factory.forEach(SwarmBotEntity.class)
                .filter(bot -> bot.getAssignedParty() != null && bot.getRole() == SwarmTacticalRole.HEALER)
                .groupBy(SwarmBotEntity::getAssignedParty, ConstraintCollectors.count())
                .filter((party, count) -> count > 1)
                .penalize(HardSoftScore.ONE_HARD, (party, count) -> count - 1)
                .asConstraint("Maximum one healer");
    }

    private Constraint maximumOneBuffer(ConstraintFactory factory) {
        return factory.forEach(SwarmBotEntity.class)
                .filter(bot -> bot.getAssignedParty() != null && bot.getRole() == SwarmTacticalRole.BUFFER)
                .groupBy(SwarmBotEntity::getAssignedParty, ConstraintCollectors.count())
                .filter((party, count) -> count > 1)
                .penalize(HardSoftScore.ONE_HARD, (party, count) -> count - 1)
                .asConstraint("Maximum one buffer");
    }

    private Constraint minimumOneHealer(ConstraintFactory factory) {
        return factory.forEach(WarRoomParty.class)
                .ifNotExists(
                        SwarmBotEntity.class,
                        Joiners.equal(Function.identity(), SwarmBotEntity::getAssignedParty),
                        Joiners.filtering((WarRoomParty party, SwarmBotEntity bot) ->
                                bot.getRole() == SwarmTacticalRole.HEALER))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Minimum one healer");
    }

    private Constraint minimumOneBuffer(ConstraintFactory factory) {
        return factory.forEach(WarRoomParty.class)
                .ifNotExists(
                        SwarmBotEntity.class,
                        Joiners.equal(Function.identity(), SwarmBotEntity::getAssignedParty),
                        Joiners.filtering((WarRoomParty party, SwarmBotEntity bot) ->
                                bot.getRole() == SwarmTacticalRole.BUFFER))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Minimum one buffer");
    }

    private Constraint maximizeDps(ConstraintFactory factory) {
        return factory.forEach(SwarmBotEntity.class)
                .filter(bot -> bot.getAssignedParty() != null)
                .reward(HardSoftScore.ONE_SOFT, SwarmBotEntity::getDpsScore)
                .asConstraint("Maximize DPS");
    }
}
