package org.sokybot.grid.domain;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

/**
 * Complete node assignment problem for the Hunting Grid solver.
 */
@PlanningSolution
public class GridSolution {

    @ValueRangeProvider(id = "nodeRange")
    @ProblemFactCollectionProperty
    private List<GridNode> nodeList = new ArrayList<>();

    @PlanningEntityCollectionProperty
    private List<GridBotEntity> botList = new ArrayList<>();

    @PlanningScore
    private HardSoftScore score = HardSoftScore.ZERO;

    public GridSolution() {
    }

    public List<GridNode> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<GridNode> nodeList) {
        this.nodeList = nodeList != null ? nodeList : new ArrayList<>();
    }

    public List<GridBotEntity> getBotList() {
        return botList;
    }

    public void setBotList(List<GridBotEntity> botList) {
        this.botList = botList != null ? botList : new ArrayList<>();
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
