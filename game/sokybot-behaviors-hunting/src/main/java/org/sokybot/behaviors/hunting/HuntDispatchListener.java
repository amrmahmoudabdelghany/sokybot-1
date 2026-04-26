package org.sokybot.behaviors.hunting;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.swarm.api.HuntDispatchedEvent;
import org.sokybot.swarm.api.ISwarmEventBus;

import reactor.core.Disposable;

@Component(service = HuntDispatchListener.class, immediate = true)
public final class HuntDispatchListener {

    @Reference
    private ISwarmEventBus swarmBus;

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<HuntDispatchedEvent>> queueByMachine =
            new ConcurrentHashMap<>();

    private Disposable dispatchSub;

    @Activate
    void activate() {
        dispatchSub = swarmBus.observe(HuntDispatchedEvent.class).subscribe(this::onDispatch);
    }

    @Deactivate
    void deactivate() {
        if (dispatchSub != null && !dispatchSub.isDisposed()) {
            dispatchSub.dispose();
        }
        queueByMachine.clear();
    }

    private void onDispatch(HuntDispatchedEvent e) {
        queueByMachine
                .computeIfAbsent(e.getHunterMachineId(), k -> new ConcurrentLinkedQueue<>())
                .offer(e);
    }

    public HuntDispatchedEvent poll(String machineId) {
        ConcurrentLinkedQueue<HuntDispatchedEvent> q = queueByMachine.get(machineId);
        return q == null ? null : q.poll();
    }

    public boolean hasPending(String machineId) {
        ConcurrentLinkedQueue<HuntDispatchedEvent> q = queueByMachine.get(machineId);
        return q != null && !q.isEmpty();
    }
}
