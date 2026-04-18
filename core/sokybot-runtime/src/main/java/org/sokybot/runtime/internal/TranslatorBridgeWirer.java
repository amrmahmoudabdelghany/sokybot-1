package org.sokybot.runtime.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.event.EventAdmin;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.commons.lifecycle.ISubscriptionScope;
import org.sokybot.commons.osgi.OsgiEventTopics;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gamemodel.spi.IGameModelMutator;
import org.sokybot.network.IPacketPublisher;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscribes packet translators to the publisher and forwards events to the game model
 * and OSGi/reactive buses.
 */
final class TranslatorBridgeWirer {

    private static final Logger log = LoggerFactory.getLogger(TranslatorBridgeWirer.class);

    private final ChunkedPacketManager chunkManager;
    private final IGameModelMutator gameModelMutator;
    private final ISubscriptionScope subscriptionScope;

    TranslatorBridgeWirer(ChunkedPacketManager chunkManager,
            IGameModelMutator gameModelMutator,
            ISubscriptionScope subscriptionScope) {
        this.chunkManager = chunkManager;
        this.gameModelMutator = gameModelMutator;
        this.subscriptionScope = subscriptionScope;
    }

    void wireTranslatorChains(IPacketPublisher publisher,
            Map<Integer, List<IPacketTranslator>> translatorsToWire,
            String machineId,
            EventAdmin eventAdmin,
            IReactiveEventBus reactiveBus) {
        if (publisher == null || translatorsToWire == null || translatorsToWire.isEmpty()) {
            return;
        }
        translatorsToWire.forEach((opcode, chain) -> {
            if (chain == null) {
                return;
            }
            for (IPacketTranslator translator : chain) {
                wireOne(publisher, opcode, translator, machineId, eventAdmin, reactiveBus);
            }
        });
    }

    private void wireOne(IPacketPublisher publisher, Integer opcode, IPacketTranslator translator, String machineId,
            EventAdmin eventAdmin,
            IReactiveEventBus reactiveBus) {
        int op = opcode == null ? translator.getOpcode() : opcode.intValue();
        org.sokybot.network.IPacketSubscription sub = publisher.subscribe((packet) -> {
            try {
                List<IGameEvent> events = translator.translate(machineId, packet, chunkManager);
                if (log.isDebugEnabled() && op == 0xA101) {
                    int eventCount = events != null ? events.size() : 0;
                    log.debug("Translator bridge machine={} opcode=0xA101 translator={} produced {} events", machineId,
                            translator.getClass().getName(), Integer.valueOf(eventCount));
                }
                if (op == 0xA101 && (events == null || events.isEmpty())) {
                    log.warn(
                            "Agent list packet (0xA101) produced no events for machine {} (size={}) via {}",
                            machineId, packet != null ? Integer.valueOf(packet.getPacketSize()) : Integer.valueOf(-1),
                            translator.getClass().getName());
                }
                if (events != null) {
                    for (IGameEvent event : events) {
                        if (event == null) {
                            continue;
                        }
                        gameModelMutator.dispatchGameEvent(event);
                        Map<String, Object> props = new HashMap<>();
                        props.put("event", event);
                        props.put("machineId", machineId);
                        props.put("fullName", machineId);
                        String topic = OsgiEventTopics.gameTopic(machineId, event.getClass().getSimpleName());
                        if (log.isDebugEnabled()) {
                            log.debug("Posting game event machine={} topic={} type={}", machineId, topic,
                                    event.getClass().getName());
                        }
                        if (eventAdmin != null) {
                            eventAdmin.postEvent(new Event(topic, props));
                        }
                        if (reactiveBus != null) {
                            reactiveBus.publish(event);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error translating packet opcode 0x{} for machine {}",
                        Integer.toHexString(op).toUpperCase(), machineId, e);
            }
        }, op);
        subscriptionScope.register(sub);
    }
}
