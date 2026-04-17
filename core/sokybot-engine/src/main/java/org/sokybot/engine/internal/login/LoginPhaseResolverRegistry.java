package org.sokybot.engine.internal.login;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.engine.api.login.ILoginPhaseResolver;
import org.sokybot.engine.api.login.ILoginPhaseResolverRegistry;
import org.sokybot.gamemodel.LoginState;

@Component(service = ILoginPhaseResolverRegistry.class, immediate = true)
public class LoginPhaseResolverRegistry implements ILoginPhaseResolverRegistry {

    private final List<ILoginPhaseResolver> resolvers = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindResolver(ILoginPhaseResolver resolver) {
        synchronized (resolvers) {
            resolvers.add(resolver);
            resolvers.sort(Comparator.comparingInt(ILoginPhaseResolver::getPriority).reversed());
        }
    }

    protected void unbindResolver(ILoginPhaseResolver resolver) {
        resolvers.remove(resolver);
    }

    @Override
    public Optional<LoginState.Phase> resolve(String rawPhase) {
        if (rawPhase == null || rawPhase.isEmpty()) {
            return Optional.empty();
        }
        for (ILoginPhaseResolver resolver : resolvers) {
            Optional<LoginState.Phase> phase = resolver.resolve(rawPhase);
            if (phase.isPresent()) {
                return phase;
            }
        }
        return Optional.empty();
    }
}
