package org.sokybot.social.projections.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.chat.ChatMessageEvent;
import org.sokybot.gameevents.events.entity.EntitySpawnEvent;
import org.sokybot.gameevents.events.world.GameNotifyEvent;
import org.sokybot.social.api.ChatLine;
import org.sokybot.social.api.IGmRecognizer;
import org.sokybot.social.api.ISocialModel;
import org.sokybot.social.api.ISocialSnapshot;
import org.sokybot.social.api.SocialAlert;
import org.sokybot.social.api.SocialChannel;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component(service = ISocialModel.class, immediate = true)
@Slf4j
public class SocialModelComponent implements ISocialModel {

    @Reference
    private IReactiveEventBus eventBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IGmRecognizer recognizer;

    private final ConcurrentHashMap<String, MachineSocialState> states = new ConcurrentHashMap<>();
    private final reactor.core.Disposable.Composite disposables = reactor.core.Disposables.composite();

    private final Sinks.Many<ChatLine> allChatSink =
            Sinks.many().multicast().onBackpressureBuffer(1024, false);
    private final Sinks.Many<SocialAlert> allAlertSink =
            Sinks.many().multicast().onBackpressureBuffer(256, false);

    @Activate
    public void activate() {
        disposables.add(eventBus.on(ChatMessageEvent.class).subscribe(this::onChat));
        disposables.add(eventBus.on(GameNotifyEvent.class).subscribe(this::onNotify));
        disposables.add(eventBus.on(EntitySpawnEvent.class).subscribe(this::onEntitySpawn));
    }

    @Deactivate
    public void deactivate() {
        disposables.dispose();
        states.values().forEach(s -> {
            s.chatSink.tryEmitComplete();
            s.alertSink.tryEmitComplete();
            s.snapshotSink.tryEmitComplete();
        });
        states.clear();
        allChatSink.tryEmitComplete();
        allAlertSink.tryEmitComplete();
    }

    private void onChat(ChatMessageEvent event) {
        String machineId = event.getMachineName();
        MachineSocialState state = states.computeIfAbsent(machineId, k -> new MachineSocialState());
        
        boolean isGm = recognizer != null && recognizer.isGameMaster(event.getSenderName());
        SocialChannel channel = parseChannel(event.getChatType());
        
        ChatLine line = new ChatLine(
            machineId, 
            event.getTimestamp(), 
            channel, 
            event.getSenderName(), 
            event.getMessage(), 
            isGm, 
            false // fromSelf usually derived if sender == self, but we keep false for now as default
        );
        
        synchronized(state) {
            var deque = state.ring.get(channel);
            if (deque.size() >= 256) deque.pollFirst();
            deque.addLast(line);
            
            if (isGm) {
                state.lastGmSeenAtMs = event.getTimestamp();
                SocialAlert.Kind kind = (channel == SocialChannel.NOTICE || channel == SocialChannel.GLOBAL) 
                                        ? SocialAlert.Kind.NOTICE_GM_BROADCAST 
                                        : SocialAlert.Kind.GM_WHISPER;
                SocialAlert alert = new SocialAlert(
                    machineId, event.getTimestamp(), kind, event.getSenderName(),
                    Map.of("message", event.getMessage(), "channel", channel.name())
                );
                if (state.alerts.size() >= 64) state.alerts.pollFirst();
                state.alerts.addLast(alert);
                state.alertSink.tryEmitNext(alert);
                allAlertSink.tryEmitNext(alert);
            }
        }
        
        state.chatSink.tryEmitNext(line);
        allChatSink.tryEmitNext(line);
        publishSnapshot(machineId, state);
    }

    private void onNotify(GameNotifyEvent event) {
        String machineId = event.getMachineName();
        MachineSocialState state = states.computeIfAbsent(machineId, k -> new MachineSocialState());
        
        // Very basic mapping, full GameDataLookup mapping could be added if optional IGameDataLookup exists
        SocialAlert.Kind kind = null;
        if (event.getType().name().equals("UNIQUE_SPAWNED")) {
            kind = SocialAlert.Kind.UNIQUE_SPAWNED;
        } else if (event.getType().name().equals("UNIQUE_KILLED")) {
            kind = SocialAlert.Kind.UNIQUE_KILLED;
        }
        
        if (kind != null) {
            String modelId = String.valueOf(event.getModelId());
            SocialAlert alert = new SocialAlert(
                machineId, event.getTimestamp(), kind, "refId=" + modelId,
                Map.of("modelId", modelId)
            );
            
            synchronized(state) {
                if (state.alerts.size() >= 64) state.alerts.pollFirst();
                state.alerts.addLast(alert);
            }
            state.alertSink.tryEmitNext(alert);
            allAlertSink.tryEmitNext(alert);
            publishSnapshot(machineId, state);
        }
    }

    private void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityName() == null || event.getEntityName().isEmpty()) return;
        
        if (recognizer != null && recognizer.isGameMaster(event.getEntityName())) {
            String machineId = event.getMachineName();
            MachineSocialState state = states.computeIfAbsent(machineId, k -> new MachineSocialState());
            
            synchronized(state) {
                state.lastGmSeenAtMs = event.getTimestamp();
                SocialAlert alert = new SocialAlert(
                    machineId, event.getTimestamp(), SocialAlert.Kind.GM_NEARBY, event.getEntityName(),
                    Map.of("entityId", String.valueOf(event.getEntityId()))
                );
                if (state.alerts.size() >= 64) state.alerts.pollFirst();
                state.alerts.addLast(alert);
                state.alertSink.tryEmitNext(alert);
                allAlertSink.tryEmitNext(alert);
            }
            publishSnapshot(machineId, state);
        }
    }

    private void publishSnapshot(String machineId, MachineSocialState state) {
        var gmNames = recognizer != null ? recognizer.getKnownGmNames() : Collections.<String>emptySet();
        ISocialSnapshot snap;
        synchronized(state) {
            snap = new SocialSnapshotImpl(state, gmNames);
        }
        state.snapshotSink.tryEmitNext(snap);
    }

    private SocialChannel parseChannel(ChatMessageEvent.ChatType type) {
        if (type == null) return SocialChannel.UNKNOWN;
        try {
            return SocialChannel.valueOf(type.name());
        } catch (IllegalArgumentException e) {
            return SocialChannel.UNKNOWN;
        }
    }

    @Override
    public Optional<ISocialSnapshot> snapshot(String machineId) {
        MachineSocialState state = states.get(machineId);
        if (state == null) return Optional.empty();
        var gmNames = recognizer != null ? recognizer.getKnownGmNames() : Collections.<String>emptySet();
        synchronized (state) {
            return Optional.of(new SocialSnapshotImpl(state, gmNames));
        }
    }

    @Override
    public Flux<ChatLine> observeChat(String machineId) {
        return states.computeIfAbsent(machineId, k -> new MachineSocialState()).chatSink.asFlux();
    }

    @Override
    public Flux<SocialAlert> observeAlerts(String machineId) {
        return states.computeIfAbsent(machineId, k -> new MachineSocialState()).alertSink.asFlux();
    }

    @Override
    public Flux<ISocialSnapshot> observe(String machineId) {
        return states.computeIfAbsent(machineId, k -> new MachineSocialState()).snapshotSink.asFlux()
            .sample(java.time.Duration.ofMillis(50));
    }

    @Override
    public Flux<ChatLine> observeAllChat() {
        return allChatSink.asFlux();
    }

    @Override
    public Flux<SocialAlert> observeAllAlerts() {
        return allAlertSink.asFlux();
    }

    @Override
    public Set<String> knownMachineIds() {
        return Collections.unmodifiableSet(new HashSet<>(states.keySet()));
    }
}
