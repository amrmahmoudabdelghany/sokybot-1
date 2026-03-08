package org.sokybot.machinepages.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Base class for Groovy UI page scripts.
 * Eliminates the repetitive boilerplate of implementing IScriptedPage directly:
 * Logger, Sinks, settings lookup, state streaming, and settings update helpers.
 *
 * <pre>
 * class HealingPage extends BasePage {
 *     HealingPage() { super("Healing", "Heart") }
 *     def trainingSettings
 *
 *     void setup() {
 *         trainingSettings = settingsProvider("training", Object)
 *         trainingSettings.subscribe { emitStateUpdate() }
 *     }
 *
 *     Map handleAction(String action, Map data) {
 *         switch (action) {
 *             case "save": trainingSettings?.save(); break
 *             case "update": applyFrom(trainingSettings, data, ["hpPotionThreshold", "mpPotionThreshold"]); break
 *         }
 *         return withSuccess(getInitialState())
 *     }
 *
 *     Map getInitialState() { [settings: trainingSettings?.get()] }
 * }
 * new HealingPage()
 * </pre>
 */
public abstract class BasePage implements IScriptedPage {

    protected final Logger log;
    protected final String title;
    protected final String icon;
    protected IMachineContext machineContext;
    protected final Sinks.Many<Map<String, Object>> stateSink =
            Sinks.many().multicast().onBackpressureBuffer(100);

    protected BasePage(String title, String icon) {
        this.title = title;
        this.icon = icon;
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public final void init(IMachineContext context) {
        this.machineContext = context;
        log.info("{} page initialized for {}", title, context.fullName());
        try {
            setup();
        } catch (Exception e) {
            log.error("Failed to initialize {} page: {}", title, e.getMessage(), e);
        }
    }

    /**
     * Override to perform initialization (settings lookup, subscriptions, etc.).
     * Called after machineContext is available.
     */
    protected void setup() {
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public Map<String, Object> getSchema() {
        return Collections.emptyMap();
    }

    @Override
    public Flux<Map<String, Object>> streamData(String streamId, Map<String, Object> params) {
        return Flux.concat(
                Flux.just(getInitialState()),
                stateSink.asFlux()
        );
    }

    @Override
    public void shutdown() {
        stateSink.tryEmitComplete();
    }

    /**
     * Emit the current state to all stream subscribers.
     */
    protected void emitStateUpdate() {
        stateSink.tryEmitNext(getInitialState());
    }

    /**
     * Shorthand for getting an OSGi service from the machine context.
     */
    protected <T> T service(Class<T> type) {
        return machineContext.getService(type);
    }

    /**
     * Get a settings provider for the given scope and type, scoped to this machine.
     */
    protected <T> ISettingsProvider<T> settingsProvider(String scope, Class<T> type) {
        ISettingsRegistry registry = service(ISettingsRegistry.class);
        if (registry == null) {
            log.warn("ISettingsRegistry not available for {} on {}", title, machineContext.fullName());
            return null;
        }
        return registry.getProvider(
                machineContext.getGroupName(),
                machineContext.getMachineName(),
                scope, type);
    }

    /**
     * Apply matching fields from action data to a settings provider,
     * replacing repetitive data.containsKey() boilerplate.
     *
     * <pre>
     * applyFrom(loginSettings, data, ["username", "password", "autoLogin"])
     * </pre>
     */
    @SuppressWarnings("unchecked")
    protected void applyFrom(ISettingsProvider<?> provider, Map<String, Object> data, List<String> fields) {
        if (provider == null || data == null) return;
        ((ISettingsProvider<Object>) provider).update(settings -> {
            for (String field : fields) {
                if (data.containsKey(field)) {
                    try {
                        java.lang.reflect.Field f = settings.getClass().getDeclaredField(field);
                        f.setAccessible(true);
                        f.set(settings, data.get(field));
                    } catch (Exception e) {
                        log.warn("Cannot set field '{}' on {}: {}", field, settings.getClass().getSimpleName(), e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Convenience: wrap a state map with success=true.
     */
    protected Map<String, Object> withSuccess(Map<String, Object> state) {
        Map<String, Object> result = new HashMap<>(state);
        result.put("success", true);
        return result;
    }

    /**
     * Convenience: return an error response.
     */
    protected Map<String, Object> withError(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        return result;
    }
}
