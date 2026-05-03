package org.sokybot.behaviors.swarm.treasury;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmResourceDistressEvent;
import org.sokybot.swarm.api.SwarmResourceOfferEvent;
import org.sokybot.swarm.api.SwarmTradeDispatchEvent;

import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Treasury Mesh (Epic #19): collects bids after distress, picks a winner, and publishes rendezvous dispatches.
 */
@Component(service = TreasuryMeshDispatchCoordinator.class, immediate = true)
public final class TreasuryMeshDispatchCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TreasuryMeshDispatchCoordinator.class);

    private static final long RENDEZVOUS_TTL_MS = 60_000L;

    @Reference
    private ISwarmEventBus swarmBus;

    private volatile Disposable distressSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            log.warn("TreasuryMeshDispatchCoordinator: ISwarmEventBus unavailable");
            return;
        }
        distressSub = bus.observe(SwarmResourceDistressEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "TreasuryMeshDispatchCoordinator distress stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onDistress);
    }

    @Deactivate
    void deactivate() {
        Disposable d = distressSub;
        distressSub = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onDistress(SwarmResourceDistressEvent event) {
        if (event == null || swarmBus == null) {
            return;
        }
        String distressId = event.getDistressId();
        WorldPoint distressedPosition = event.getDistressedPosition();
        long waitMs = Math.max(0L, event.getOfferDeadlineEpochMs() - System.currentTimeMillis());
        if (waitMs == 0L) {
            log.debug(
                    "TreasuryMeshDispatchCoordinator: bid window elapsed for distressId={}, skipping",
                    distressId);
            return;
        }

        swarmBus.observe(SwarmResourceOfferEvent.class)
                .filter(offer -> distressId.equals(offer.getDistressId()))
                .take(Duration.ofMillis(waitMs))
                .collectList()
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
                        offers -> dispatchAfterBids(event, distressedPosition, offers),
                        err -> log.warn(
                                "TreasuryMeshDispatchCoordinator offer collection distressId={}: {}",
                                distressId,
                                err != null ? err.getMessage() : "unknown"));
    }

    private void dispatchAfterBids(
            SwarmResourceDistressEvent distress,
            WorldPoint distressedPosition,
            List<SwarmResourceOfferEvent> offers) {
        if (offers == null || offers.isEmpty()) {
            log.debug(
                    "TreasuryMeshDispatchCoordinator: no offers for distressId={}",
                    distress.getDistressId());
            return;
        }

        SwarmResourceOfferEvent winner = offers.stream()
                .max(Comparator.comparingInt(SwarmResourceOfferEvent::getOfferedAmount))
                .orElse(null);
        if (winner == null) {
            return;
        }

        WorldPoint offererPosition = winner.getOffererPosition();
        WorldPoint midpoint = midpoint(distressedPosition, offererPosition);

        String tradeSessionId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long rendezvousDeadlineEpochMs = now + RENDEZVOUS_TTL_MS;

        String distressedMachineId = distress.getDistressedMachineId();
        String supplierMachineId = winner.getOffererMachineId();
        int itemRefId = distress.getItemRefId();
        int committedAmount = winner.getOfferedAmount();
        String distressId = distress.getDistressId();

        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            return;
        }

        bus.publish(new SwarmTradeDispatchEvent(
                supplierMachineId,
                now,
                tradeSessionId,
                distressId,
                midpoint,
                supplierMachineId,
                distressedMachineId,
                itemRefId,
                committedAmount,
                rendezvousDeadlineEpochMs));

        bus.publish(new SwarmTradeDispatchEvent(
                distressedMachineId,
                now,
                tradeSessionId,
                distressId,
                midpoint,
                supplierMachineId,
                distressedMachineId,
                itemRefId,
                committedAmount,
                rendezvousDeadlineEpochMs));

        log.info(
                "TreasuryMeshDispatchCoordinator: dispatch tradeSessionId={} supplier={} distressed={} itemRef={} qty={}",
                tradeSessionId,
                supplierMachineId,
                distressedMachineId,
                itemRefId,
                committedAmount);
    }

    private static WorldPoint midpoint(WorldPoint a, WorldPoint b) {
        float x = (a.getX() + b.getX()) / 2.0f;
        float y = (a.getY() + b.getY()) / 2.0f;
        float z = (a.getZ() + b.getZ()) / 2.0f;
        return new WorldPoint(x, y, z);
    }
}
