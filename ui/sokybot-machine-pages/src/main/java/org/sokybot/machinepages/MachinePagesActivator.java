package org.sokybot.machinepages;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.machinepages.api.IScriptedPage;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ContextLifecycleEvents;

/**
 * Lifecycle manager for scripted machine pages.
 * Reacts to machine creation/destruction events and coordinates
 * page loading and webview registration via {@link PageWebviewRegistrar}.
 */
@Component(service = { EventHandler.class, MachinePagesActivator.class }, property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED,
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED
}, immediate = true)
public class MachinePagesActivator implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(MachinePagesActivator.class);

    private ISokybotContext appCtx;
    private ScriptPageLoader pageLoader;
    private PageWebviewRegistrar webviewRegistrar;
    private BundleContext bundleContext;

    private final Map<String, MachineServices> machineServices = new HashMap<>();

    @Reference
    public void setAppCtx(ISokybotContext appCtx) {
        this.appCtx = appCtx;
    }

    @Reference
    public void setPageLoader(ScriptPageLoader pageLoader) {
        this.pageLoader = pageLoader;
    }

    @Reference
    public void setWebviewRegistrar(PageWebviewRegistrar webviewRegistrar) {
        this.webviewRegistrar = webviewRegistrar;
    }

    @Activate
    public void activate(ComponentContext componentContext) {
        log.info("Starting Machine Pages Bundle");
        this.bundleContext = componentContext.getBundleContext();

        this.pageLoader.addPageListener(this::reloadPageForAllMachines);

        if (appCtx != null) {
            installExistingMachines(appCtx);
        }
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopping Machine Pages Bundle");
        uninstallAllMachines();

        if (pageLoader != null) {
            pageLoader.removePageListener(this::reloadPageForAllMachines);
        }

        machineServices.clear();
    }

    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();

        if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED.equals(topic)) {
            IMachineContext machineContext = (IMachineContext) event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
            if (machineContext != null) {
                installMachinePages(machineContext);
            }
        } else if (ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_DESTROYED.equals(topic)) {
            String fullName = (String) event.getProperty(ContextLifecycleEvents.PROP_FULL_NAME);
            if (fullName != null) {
                uninstallMachinePages(fullName);
            }
        }
    }

    // ---- Lifecycle ----

    private void installExistingMachines(ISokybotContext ctx) {
        IGroupContext[] groups = ctx.getGroups();
        log.info("Machine Pages: Found {} existing groups to scan", groups.length);
        Stream.of(groups)
                .flatMap(g -> {
                    IMachineContext[] machines = g.getMachines();
                    log.debug("Machine Pages: Group {} has {} machines", g.name(), machines.length);
                    return Stream.of(machines);
                })
                .forEach(this::installMachinePages);
    }

    private void uninstallAllMachines() {
        new ArrayList<>(machineServices.keySet()).forEach(this::uninstallMachinePages);
    }

    private void installMachinePages(IMachineContext ctx) {
        String machineFullName = ctx.fullName();
        if (machineServices.containsKey(machineFullName)) {
            return;
        }

        log.info("Installing Machine Pages for: {}", machineFullName);

        MachineServices services = new MachineServices(machineFullName, ctx);
        machineServices.put(machineFullName, services);

        for (String pageName : pageLoader.getAvailablePages()) {
            registerScriptedPage(services, pageName);
        }
    }

    private void uninstallMachinePages(String machineFullName) {
        MachineServices services = machineServices.remove(machineFullName);
        if (services != null) {
            services.shutdown();
        }
    }

    private void reloadPageForAllMachines(String pageName) {
        log.debug("Reloading scripted page: {}", pageName);
        for (MachineServices services : machineServices.values()) {
            services.unregisterPage(pageName);
            registerScriptedPage(services, pageName);
        }
    }

    private void registerScriptedPage(MachineServices services, String pageName) {
        var pageOpt = pageLoader.createPage(pageName, services.machineContext);
        if (pageOpt.isEmpty()) {
            log.warn("Page '{}' could not be created for machine {} (script missing, failed to compile, or did not return IScriptedPage – check logs above for details)", pageName, services.machineFullName);
            return;
        }
        var page = pageOpt.get();
        String pageId = pageName + "_" + services.machineFullName;
        log.debug("Registering scripted page {}", pageId);

        services.addPage(pageName, page);

        webviewRegistrar.registerPage(pageId, pageName, services.machineFullName, page);

        registerEventHandler(services, pageName, page);
    }

    private void registerEventHandler(MachineServices services, String pageName, IScriptedPage page) {
        if (bundleContext == null || !(page instanceof EventHandler)) return;

        String[] topics = page.getEventTopics(services.machineFullName);
        if (topics == null || topics.length == 0) return;

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(EventConstants.EVENT_TOPIC, topics);

        ServiceRegistration<EventHandler> reg = bundleContext.registerService(
                EventHandler.class, (EventHandler) page, props);
        services.registerHandler(pageName, reg);
    }

    // ---- Per-machine service container ----

    private class MachineServices {
        final String machineFullName;
        final IMachineContext machineContext;
        final Map<String, IScriptedPage> pages = new HashMap<>();
        final Map<String, ServiceRegistration<EventHandler>> handlerRegistrations = new HashMap<>();

        MachineServices(String machineFullName, IMachineContext machineContext) {
            this.machineFullName = machineFullName;
            this.machineContext = machineContext;
        }

        void addPage(String name, IScriptedPage page) {
            pages.put(name, page);
        }

        void registerHandler(String name, ServiceRegistration<EventHandler> reg) {
            handlerRegistrations.put(name, reg);
        }

        void unregisterPage(String name) {
            IScriptedPage page = pages.remove(name);
            if (page != null) {
                page.shutdown();
                webviewRegistrar.unregisterPage(name + "_" + machineFullName);
            }
            ServiceRegistration<EventHandler> reg = handlerRegistrations.remove(name);
            if (reg != null) {
                try { reg.unregister(); } catch (IllegalStateException ignored) {}
            }
        }

        void shutdown() {
            new ArrayList<>(pages.keySet()).forEach(this::unregisterPage);
        }
    }
}
