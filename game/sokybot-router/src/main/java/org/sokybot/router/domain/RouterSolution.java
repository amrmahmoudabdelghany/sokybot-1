package org.sokybot.router.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

/**
 * Planning solution: optimal assignment of item stacks to bots and designation of the mule.
 */
@PlanningSolution
public class RouterSolution {

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "botRange")
    private List<SwarmBotEntity> botList = new ArrayList<>();

    @PlanningEntityCollectionProperty
    private List<FieldItemEntity> itemList = new ArrayList<>();

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "booleanRange")
    private List<Boolean> booleanList = new ArrayList<>(Arrays.asList(Boolean.FALSE, Boolean.TRUE));

    @PlanningScore
    private HardSoftScore score = HardSoftScore.ZERO;

    public RouterSolution() {
    }

    public List<SwarmBotEntity> getBotList() {
        return botList;
    }

    public void setBotList(List<SwarmBotEntity> botList) {
        this.botList = botList != null ? botList : new ArrayList<>();
    }

    public List<FieldItemEntity> getItemList() {
        return itemList;
    }

    public void setItemList(List<FieldItemEntity> itemList) {
        this.itemList = itemList != null ? itemList : new ArrayList<>();
    }

    public List<Boolean> getBooleanList() {
        return booleanList;
    }

    public void setBooleanList(List<Boolean> booleanList) {
        this.booleanList = booleanList != null ? booleanList : new ArrayList<>();
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
