package org.sokybot.warroom.domain;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

/**
 * Complete roster assignment problem for the War Room solver.
 */
@PlanningSolution
public class PartyRosterSolution {

    @ValueRangeProvider(id = "partyRange")
    @ProblemFactCollectionProperty
    private List<WarRoomParty> partyList = new ArrayList<>();

    @PlanningEntityCollectionProperty
    private List<SwarmBotEntity> botList = new ArrayList<>();

    @PlanningScore
    private HardSoftScore score = HardSoftScore.ZERO;

    public PartyRosterSolution() {
    }

    public List<WarRoomParty> getPartyList() {
        return partyList;
    }

    public void setPartyList(List<WarRoomParty> partyList) {
        this.partyList = partyList != null ? partyList : new ArrayList<>();
    }

    public List<SwarmBotEntity> getBotList() {
        return botList;
    }

    public void setBotList(List<SwarmBotEntity> botList) {
        this.botList = botList != null ? botList : new ArrayList<>();
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
