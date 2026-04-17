package org.sokybot.gameevents;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.gameevents.events.core.*;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.IPacketPublisher;

/**
 * Extensible translator factory that:
 * 1. Discovers translators via ITranslatorProvider (extensibility)
 * 2. Creates translators per-game (shared across bots, memory optimized)
 * 3. Follows Open/Closed Principle
 * 
 * <p>This factory dynamically discovers ITranslatorProvider services via OSGi
 * and creates translator instances per-game. Translators are shared across
 * all bots in the same game to optimize memory usage.
 * 
 * <p>Custom packets from private servers can be supported by implementing
 * ITranslatorProvider and registering it as an OSGi service.
 */
@Component(service = ITranslatorFactory.class, immediate = true)
public class ExtensibleTranslatorFactory implements ITranslatorFactory {
    
    private static final Logger log = LoggerFactory.getLogger(ExtensibleTranslatorFactory.class);
    
    // Discovered providers (sorted by priority, highest first)
    private final List<ITranslatorProvider> providers = new CopyOnWriteArrayList<>();
    
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindProvider(ITranslatorProvider provider) {
        synchronized (providers) {
            providers.add(provider);
            // Sort by priority (higher first)
            providers.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        }
        Set<Integer> supportedOpcodes = provider.getSupportedOpcodes(null);
        log.info("Registered translator provider: {} (priority: {}, opcodes: {})", 
                 provider.getClass().getName(), 
                 provider.getPriority(),
                 supportedOpcodes != null ? supportedOpcodes.size() : "unknown");
    }
    
    protected void unbindProvider(ITranslatorProvider provider) {
        providers.remove(provider);
        log.info("Unregistered translator provider: {}", provider.getClass().getName());
    }
    
    @Activate
    protected void activate() {
        log.info("ExtensibleTranslatorFactory activated with {} providers", providers.size());
    }
    
    @Deactivate
    protected void deactivate() {
        log.info("ExtensibleTranslatorFactory deactivated");
    }
    
    @Override
    public Map<Integer, List<IPacketTranslator>> createTranslators(IGameDataLookup lookup,
                                                              IPacketPublisher publisher) {
        Map<Integer, List<IPacketTranslator>> translators = new HashMap<>();
        
        // Collect all unique opcodes from all providers
        Set<Integer> allOpcodes = collectAllOpcodes(lookup);
        String gamePath = lookup != null ? lookup.getGamePath() : "(no game data lookup)";
        String version = lookup != null ? Integer.toString(lookup.getVersion()) : "n/a";
        log.debug("Creating translators for game: {} (version: {}), discovered {} opcodes", 
                 gamePath, version, allOpcodes.size());
        
        // Build translator chain for each opcode in priority order
        int created = 0;
        for (int opcode : allOpcodes) {
            List<IPacketTranslator> chain = createChainForOpcode(opcode, lookup);
            if (!chain.isEmpty()) {
                translators.put(opcode, chain);
                created++;
            }
        }
        
        log.info("Created {} translators for game: {} (version: {}) from {} providers", 
                created, gamePath, version, providers.size());
        
        return translators;
    }

    @Override
    @Deprecated
    public Map<Integer, IPacketTranslator> createTranslatorsSingle(IGameDataLookup lookup,
            IPacketPublisher publisher) {
        return ITranslatorFactory.super.createTranslatorsSingle(lookup, publisher);
    }
    
    /**
     * Collect all opcodes supported by all providers.
     */
    private Set<Integer> collectAllOpcodes(IGameDataLookup lookup) {
        Set<Integer> allOpcodes = new HashSet<>();
        
        // Query each provider for supported opcodes
        for (ITranslatorProvider provider : providers) {
            Set<Integer> supported = provider.getSupportedOpcodes(lookup);
            if (supported != null) {
                // Fast path: provider knows its opcodes
                allOpcodes.addAll(supported);
            } else {
                // Slow path: provider doesn't implement getSupportedOpcodes()
                // We could scan common range (0x0000 - 0xFFFF), but that's inefficient
                // For now, rely on providers implementing getSupportedOpcodes()
                log.warn("Provider {} does not implement getSupportedOpcodes(), " +
                        "some opcodes may be missed", provider.getClass().getName());
            }
        }
        
        // Also check for dynamic providers that might support opcodes based on lookup
        // (e.g., version-specific providers)
        // This is a fallback - providers should implement getSupportedOpcodes()
        
        return allOpcodes;
    }
    
    private List<IPacketTranslator> createChainForOpcode(int opcode, IGameDataLookup lookup) {
        List<IPacketTranslator> chain = new ArrayList<>();
        Set<IPacketTranslator> dedupe = Collections.newSetFromMap(new IdentityHashMap<>());

        for (ITranslatorProvider provider : providers) {
            if (!provider.supports(opcode, lookup)) {
                continue;
            }
            IPacketTranslator translator = provider.createTranslator(opcode, lookup);
            if (translator == null) {
                continue;
            }
            if (dedupe.add(translator)) {
                chain.add(translator);
                if (log.isDebugEnabled()) {
                    log.debug("Added translator to chain for opcode 0x{} using provider {} (priority: {})",
                            String.format("%04X", opcode),
                            provider.getClass().getSimpleName(),
                            provider.getPriority());
                }
            }
        }

        if (chain.isEmpty()) {
            log.warn("No translator provider found for opcode: 0x{} (game: {})",
                    String.format("%04X", opcode), lookup != null ? lookup.getGamePath() : "(no lookup)");
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(chain);
    }
}
