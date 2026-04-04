package org.sokybot.runtime.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.gameevents.ChunkedPacketManagerRegistry;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;

/**
 * Periodically drops {@link ChunkedPacketManagerRegistry} entries that no longer correspond to a live machine
 * (e.g. crash paths that skipped normal destroy).
 */
@Component(immediate = true)
public class ChunkedPacketManagerReaper {

    private static final Logger log = LoggerFactory.getLogger(ChunkedPacketManagerReaper.class);

    private ScheduledExecutorService scheduler;

    private volatile ISokybotContext sokybotContext;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
    }

    protected void unsetSokybotContext(ISokybotContext ctx) {
        this.sokybotContext = null;
    }

    @Activate
    protected void activate() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "chunk-manager-reaper");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::tick, 30L, 30L, TimeUnit.SECONDS);
    }

    @Deactivate
    protected void deactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    void tick() {
        ISokybotContext ctx = this.sokybotContext;
        if (ctx == null) {
            return;
        }
        try {
            Set<String> live = new HashSet<>();
            for (IGroupContext g : ctx.getGroups()) {
                for (IMachineContext m : g.getMachines()) {
                    live.add(m.fullName());
                }
            }
            int before = ChunkedPacketManagerRegistry.getInstance().size();
            ChunkedPacketManagerRegistry.getInstance().pruneEntriesNotIn(live);
            int after = ChunkedPacketManagerRegistry.getInstance().size();
            if (after < before) {
                log.debug("ChunkedPacketManagerRegistry reaper removed {} stale entries", before - after);
            }
        } catch (Exception e) {
            log.warn("ChunkedPacketManagerRegistry reaper tick failed", e);
        }
    }
}
