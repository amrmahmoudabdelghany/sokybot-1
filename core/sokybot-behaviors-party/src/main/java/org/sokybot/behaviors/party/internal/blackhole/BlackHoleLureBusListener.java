package org.sokybot.behaviors.party.internal.blackhole;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmLureCycleEvent;

import reactor.core.Disposable;

@Component(service = BlackHoleLureBusListener.class, immediate = true)
public final class BlackHoleLureBusListener {

    @Reference
    private ISwarmEventBus swarmBus;

    private final ConcurrentHashMap<String, SwarmLureCycleEvent> latestByAnchorMachineId = new ConcurrentHashMap<>();

    private Disposable lureSub;

    @Activate
    void activate() {
        lureSub = swarmBus.observe(SwarmLureCycleEvent.class).subscribe(this::onLureEvent);
    }

    @Deactivate
    void deactivate() {
        if (lureSub != null && !lureSub.isDisposed()) {
            lureSub.dispose();
        }
        latestByAnchorMachineId.clear();
    }

    private void onLureEvent(SwarmLureCycleEvent e) {
        if (e == null) {
            return;
        }
        latestByAnchorMachineId.put(e.getAnchorMachineId(), e);
    }

    /**
     * Latest published lure cycle event for the given anchor machine id (full name).
     */
    public Optional<SwarmLureCycleEvent> latestForAnchor(String anchorMachineId) {
        if (anchorMachineId == null) {
            return Optional.empty();
        }
        SwarmLureCycleEvent ev = latestByAnchorMachineId.get(anchorMachineId.trim());
        return Optional.ofNullable(ev);
    }
}
