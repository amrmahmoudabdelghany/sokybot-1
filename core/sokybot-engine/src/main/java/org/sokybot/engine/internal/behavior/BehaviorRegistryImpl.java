package org.sokybot.engine.internal.behavior;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.behavior.IBehavior;

@Component(service = IBehaviorRegistry.class, immediate = true)
final class BehaviorRegistryImpl implements IBehaviorRegistry {
    private static final Logger log = LoggerFactory.getLogger(BehaviorRegistryImpl.class);
    private final CopyOnWriteArrayList<IBehavior<?>> behaviors = new CopyOnWriteArrayList<IBehavior<?>>();

    @Reference(
            service = IBehavior.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    void bindBehavior(IBehavior<?> behavior, Map<String, Object> props) {
        behaviors.addIfAbsent(behavior);
        log.debug("Bound behavior {} ({})", behavior.id(), behavior.getClass().getName());
    }

    void unbindBehavior(IBehavior<?> behavior) {
        behaviors.remove(behavior);
        log.debug("Unbound behavior {} ({})", behavior.id(), behavior.getClass().getName());
    }

    @Override
    public List<IBehavior<?>> forCycle(String cycleId) {
        List<IBehavior<?>> selected = new ArrayList<IBehavior<?>>(behaviors.size());
        for (IBehavior<?> behavior : behaviors) {
            if (behavior.appliesTo(cycleId)) {
                selected.add(behavior);
            }
        }
        return selected.stream()
                .sorted(Comparator.comparingInt(IBehavior::order))
                .collect(Collectors.toList());
    }
}
