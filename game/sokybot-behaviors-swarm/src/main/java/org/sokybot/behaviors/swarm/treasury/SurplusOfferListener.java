package org.sokybot.behaviors.swarm.treasury;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.behaviors.logistics.LogisticsSettings;
import org.sokybot.behaviors.logistics.ResourceBaselineSettings;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmResourceDistressEvent;
import org.sokybot.swarm.api.SwarmResourceOfferEvent;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Treasury Mesh (Epic #19): replies to distress with {@link SwarmResourceOfferEvent} when local surplus exists.
 */
@Component(service = SurplusOfferListener.class, immediate = true)
public final class SurplusOfferListener {

    private static final Logger log = LoggerFactory.getLogger(SurplusOfferListener.class);

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    private volatile Disposable distressSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            log.warn("SurplusOfferListener: ISwarmEventBus unavailable");
            return;
        }
        distressSub = bus.observe(SwarmResourceDistressEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "SurplusOfferListener distress stream: {}",
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
        if (event == null) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        ISwarmEventBus bus = swarmBus;
        ITownModel town = townModel;
        ISettingsRegistry registry = settingsRegistry;
        if (ctx == null || bus == null || town == null || registry == null) {
            return;
        }

        String distressedId = event.getDistressedMachineId();
        for (IGroupContext group : ctx.getGroups()) {
            if (group == null) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null) {
                    continue;
                }
                try {
                    maybeOffer(bus, registry, town, event, distressedId, machine);
                } catch (Exception ex) {
                    log.trace(
                            "SurplusOfferListener skip machine {}: {}",
                            machine.fullName(),
                            ex.getMessage());
                }
            }
        }
    }

    private static void maybeOffer(
            ISwarmEventBus bus,
            ISettingsRegistry registry,
            ITownModel town,
            SwarmResourceDistressEvent event,
            String distressedId,
            IMachineContext machine) {
        String fullName = machine.fullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            return;
        }
        if (fullName.equals(distressedId)) {
            return;
        }
        if (!machine.isRunning()) {
            return;
        }

        LogisticsSettings settings = readLogistics(registry, machine);
        if (settings == null || !settings.getTreasuryBaseline().isEnabled()) {
            return;
        }

        ResourceBaselineSettings baseline = settings.getTreasuryBaseline();
        ITownSnapshot snap = town.snapshot(fullName).orElse(null);
        if (snap == null || snap.isDead()) {
            return;
        }

        int itemRefId = event.getItemRefId();
        int currentQty = snap.getInventory().countItemRef(itemRefId);
        int targetQty = baseline.getItemTargets().getOrDefault(itemRefId, 0);
        int surplus = currentQty - targetQty;
        if (surplus <= 0) {
            return;
        }

        int offerQty = Math.min(surplus, event.getDeficitAmount());
        long ts = System.currentTimeMillis();
        WorldPoint pos = new WorldPoint(snap.getSelfX(), snap.getSelfY(), snap.getSelfZ());

        bus.publish(new SwarmResourceOfferEvent(
                fullName,
                ts,
                event.getDistressId(),
                itemRefId,
                offerQty,
                pos));

        log.debug(
                "SurplusOfferListener offer distressId={} offerer={} itemRef={} qty={}",
                event.getDistressId(),
                fullName,
                itemRefId,
                offerQty);
    }

    private static LogisticsSettings readLogistics(ISettingsRegistry registry, IMachineContext machine) {
        try {
            ISettingsProvider<LogisticsSettings> provider = registry.getProvider(
                    machine.getGroupName(),
                    machine.getMachineName(),
                    "logistics",
                    LogisticsSettings.class);
            return provider != null ? provider.get() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
