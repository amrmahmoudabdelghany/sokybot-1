package org.sokybot.behaviors.logistics.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.router.api.IRouterSolver;
import org.sokybot.router.domain.FieldItemEntity;
import org.sokybot.router.domain.RouterSolution;
import org.sokybot.router.domain.SwarmBotEntity;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.router.SwarmInventoryCriticalEvent;
import org.sokybot.swarm.api.router.SwarmMuleDispatchEvent;
import org.sokybot.swarm.api.router.SwarmTradeCommandEvent;
import org.sokybot.town.api.IInventorySnapshot;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.ItemStackSnapshot;
import org.sokybot.town.projections.api.ITownModel;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #24 Phase 4: reacts to critical inventory, solves router assignment, publishes trade and mule commands.
 */
@Component(immediate = true)
public final class RouterOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RouterOrchestrator.class);

    private static final String REQUESTER = "router-orchestrator";

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private IRouterSolver routerSolver;

    @Reference
    private ITownModel townModel;

    private volatile Disposable criticalSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmEventBus;
        if (bus == null) {
            log.warn("RouterOrchestrator: ISwarmEventBus unavailable");
            return;
        }
        criticalSub = bus.observe(SwarmInventoryCriticalEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "RouterOrchestrator critical stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onInventoryCritical);
    }

    @Deactivate
    void deactivate() {
        if (criticalSub != null && !criticalSub.isDisposed()) {
            criticalSub.dispose();
        }
        criticalSub = null;
    }

    private void onInventoryCritical(SwarmInventoryCriticalEvent event) {
        if (event == null) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        IRouterSolver solver = routerSolver;
        ISwarmEventBus bus = swarmEventBus;
        ITownModel town = townModel;
        if (ctx == null || solver == null || bus == null || town == null) {
            return;
        }

        String swarmGroupId = event.getSwarmGroupId();
        IGroupContext group = null;
        try {
            for (IGroupContext g : ctx.getGroups()) {
                if (g != null && Objects.equals(g.name().trim(), swarmGroupId)) {
                    group = g;
                    break;
                }
            }
        } catch (Exception ex) {
            log.warn("RouterOrchestrator: resolve group failed: {}", ex.getMessage());
            return;
        }
        if (group == null) {
            log.debug("RouterOrchestrator: no group named {}", swarmGroupId);
            return;
        }

        List<SwarmBotEntity> bots = new ArrayList<>();
        List<FieldItemEntity> items = new ArrayList<>();

        try {
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null || !machine.isRunning()) {
                    continue;
                }
                try {
                    mapMachineInventory(machine, town, bots, items);
                } catch (Exception ex) {
                    log.warn(
                            "RouterOrchestrator: skip machine {}: {}",
                            machine.fullName(),
                            ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("RouterOrchestrator: enumerate machines failed: {}", ex.getMessage());
            return;
        }

        if (bots.isEmpty()) {
            log.debug("RouterOrchestrator: no bots mapped for {}", swarmGroupId);
            return;
        }

        solver.calculateFieldLogistics(bots, items)
                .doOnError(err -> log.warn("RouterOrchestrator: solver failed: {}", err.toString()))
                .subscribe(
                        solution -> {
                            try {
                                publishRouterOutcome(bus, ctx, town, event, solution);
                            } catch (Exception ex) {
                                log.warn("RouterOrchestrator: publish outcome failed: {}", ex.getMessage());
                            }
                        },
                        err -> log.warn("RouterOrchestrator: solver subscribe error: {}", err.toString()));
    }

    private static void mapMachineInventory(
            IMachineContext machine,
            ITownModel townModel,
            List<SwarmBotEntity> bots,
            List<FieldItemEntity> items) {
        String fullName = machine.fullName();
        IInventorySnapshot inv = null;
        try {
            java.util.Optional<ITownSnapshot> snap = townModel.snapshot(fullName);
            if (snap.isPresent()) {
                inv = snap.get().getInventory();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("town snapshot: " + ex.getMessage(), ex);
        }
        if (inv == null) {
            throw new IllegalStateException("missing inventory snapshot");
        }

        SwarmBotEntity bot = new SwarmBotEntity();
        bot.setMachineId(fullName);
        bot.setMaxCapacity(inv.getTotalSlots());
        bot.setIsMule(null);
        bots.add(bot);

        List<ItemStackSnapshot> stacks = inv.listStacks();
        if (stacks == null) {
            return;
        }
        for (ItemStackSnapshot stack : stacks) {
            if (stack == null) {
                continue;
            }
            if (stack.getQuantity() <= 0 || stack.getItemRefId() == 0) {
                continue;
            }
            FieldItemEntity item = new FieldItemEntity();
            item.setUniqueItemId(fullName + "_" + stack.getSlotIndex());
            item.setSlotsTaken(1);
            int qty = stack.getQuantity();
            item.setGoldValue(qty > 0 ? (long) qty * 10L : 100L);
            item.setOriginalBotMachineId(fullName);
            item.setSlotIndex(stack.getSlotIndex());
            item.setStackQuantity(stack.getQuantity());
            item.setItemRefId(stack.getItemRefId());
            item.setAssignedBot(null);
            items.add(item);
        }
    }

    private void publishRouterOutcome(
            ISwarmEventBus bus,
            ISokybotContext sokybotContext,
            ITownModel townModel,
            SwarmInventoryCriticalEvent trigger,
            RouterSolution solution) {
        if (solution == null || bus == null) {
            return;
        }
        long ts = System.currentTimeMillis();
        String swarmGroupId = trigger.getSwarmGroupId();

        List<SwarmBotEntity> botList = solution.getBotList();
        if (botList != null) {
            SwarmBotEntity mule = null;
            for (SwarmBotEntity b : botList) {
                if (b != null && Boolean.TRUE.equals(b.getIsMule())) {
                    mule = b;
                    break;
                }
            }
            if (mule != null && mule.getMachineId() != null && !mule.getMachineId().isEmpty()) {
                SwarmMuleDispatchEvent muleEv = new SwarmMuleDispatchEvent(
                        REQUESTER,
                        ts,
                        UUID.randomUUID().toString(),
                        mule.getMachineId(),
                        swarmGroupId,
                        "Router logistics optimized");
                bus.publish(muleEv);
            }
        }

        List<FieldItemEntity> itemList = solution.getItemList();
        if (itemList == null) {
            return;
        }
        for (FieldItemEntity item : itemList) {
            if (item == null || item.getAssignedBot() == null) {
                continue;
            }
            String from = item.getOriginalBotMachineId();
            String to = item.getAssignedBot().getMachineId();
            if (from == null || to == null) {
                continue;
            }
            if (from.equals(to)) {
                continue;
            }
            try {
                if (townModel != null && !townModel.snapshot(to).isPresent()) {
                    log.debug("RouterOrchestrator: skip trade — no town snapshot for {}", to);
                    continue;
                }
            } catch (Exception ex) {
                log.warn("RouterOrchestrator: town lookup for {} failed: {}", to, ex.getMessage());
                continue;
            }
            String targetCharacterName = resolveTrainerCharacterName(sokybotContext, to);
            if (targetCharacterName.isEmpty()) {
                log.warn("RouterOrchestrator: skip trade — could not resolve character name for {}", to);
                continue;
            }
            int qty = item.getStackQuantity() > 0 ? item.getStackQuantity() : 1;
            SwarmTradeCommandEvent trade = new SwarmTradeCommandEvent(
                    REQUESTER,
                    ts,
                    UUID.randomUUID().toString(),
                    from,
                    to,
                    item.getUniqueItemId(),
                    item.getItemRefId(),
                    qty,
                    item.getSlotIndex(),
                    targetCharacterName,
                    UUID.randomUUID().toString());
            bus.publish(trade);
        }
    }

    private static String resolveTrainerCharacterName(ISokybotContext ctx, String machineFullName) {
        if (ctx == null || machineFullName == null || machineFullName.isEmpty()) {
            return "";
        }
        try {
            for (IGroupContext g : ctx.getGroups()) {
                if (g == null) {
                    continue;
                }
                for (IMachineContext m : g.getMachines()) {
                    if (m == null || !machineFullName.equals(m.fullName())) {
                        continue;
                    }
                    IEngine engine = m.getEngine();
                    if (engine == null) {
                        continue;
                    }
                    Optional<IWorkflowContext> wctx = engine.optionalWorkflowContext();
                    if (!wctx.isPresent()) {
                        continue;
                    }
                    IGameModel gm = wctx.get().getGameModel();
                    if (gm == null || gm.getTrainer() == null) {
                        continue;
                    }
                    String name = gm.getTrainer().getName();
                    return name != null ? name.trim() : "";
                }
            }
        } catch (Exception ex) {
            return "";
        }
        return "";
    }
}
