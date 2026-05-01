package org.sokybot.behaviors.navigation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmLureCycleEvent;

import reactor.core.Disposable;

@Component(service = SwarmLureDispatchListener.class, immediate = true)
public final class SwarmLureDispatchListener {

    @Reference
    private ISwarmEventBus swarmBus;

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<SwarmLureCycleEvent>> queueByMachine =
            new ConcurrentHashMap<>();

    private Disposable lureSub;

    @Activate
    void activate() {
        lureSub = swarmBus.observe(SwarmLureCycleEvent.class).subscribe(this::onEvent);
    }

    @Deactivate
    void deactivate() {
        if (lureSub != null && !lureSub.isDisposed()) {
            lureSub.dispose();
        }
        queueByMachine.clear();
    }

    private void onEvent(SwarmLureCycleEvent e) {
        if (e == null) {
            return;
        }
        for (String machineId : e.getAssignedLurers()) {
            queueByMachine
                    .computeIfAbsent(machineId, k -> new ConcurrentLinkedQueue<>())
                    .offer(e);
        }
    }

    public SwarmLureCycleEvent poll(String machineId) {
        if (machineId == null) {
            return null;
        }
        ConcurrentLinkedQueue<SwarmLureCycleEvent> q = queueByMachine.get(machineId.trim());
        return q == null ? null : q.poll();
    }

    public boolean hasPending(String machineId) {
        if (machineId == null) {
            return false;
        }
        ConcurrentLinkedQueue<SwarmLureCycleEvent> q = queueByMachine.get(machineId.trim());
        return q != null && !q.isEmpty();
    }
}
