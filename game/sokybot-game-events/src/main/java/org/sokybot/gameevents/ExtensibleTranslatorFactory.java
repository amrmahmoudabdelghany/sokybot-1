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
        log.info("Registered translator provider: {} (priority: {}, opcodes: {})", 
                 provider.getClass().getName(), 
                 provider.getPriority(),
                 provider.getSupportedOpcodes() != null ? 
                     provider.getSupportedOpcodes().size() : "unknown");
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
    public Map<Integer, IPacketTranslator> createTranslators(IGameDataLookup lookup, 
                                                              IPacketPublisher publisher) {
        Map<Integer, IPacketTranslator> translators = new HashMap<>();
        
        // Collect all unique opcodes from all providers
        Set<Integer> allOpcodes = collectAllOpcodes(lookup);
        String gamePath = lookup != null ? lookup.getGamePath() : "(no game data lookup)";
        String version = lookup != null ? Integer.toString(lookup.getVersion()) : "n/a";
        log.debug("Creating translators for game: {} (version: {}), discovered {} opcodes", 
                 gamePath, version, allOpcodes.size());
        
        // Create translator for each opcode using highest priority provider
        int created = 0;
        for (int opcode : allOpcodes) {
            IPacketTranslator translator = createTranslatorForOpcode(opcode, lookup);
            if (translator != null) {
                translators.put(opcode, translator);
                created++;
            }
        }
        
        log.info("Created {} translators for game: {} (version: {}) from {} providers", 
                created, gamePath, version, providers.size());
        
        return translators;
    }
    
    /**
     * Collect all opcodes supported by all providers.
     */
    private Set<Integer> collectAllOpcodes(IGameDataLookup lookup) {
        Set<Integer> allOpcodes = new HashSet<>();
        
        // Query each provider for supported opcodes
        for (ITranslatorProvider provider : providers) {
            Set<Integer> supported = provider.getSupportedOpcodes();
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
    
    /**
     * Create translator for a specific opcode using highest priority provider.
     */
    private IPacketTranslator createTranslatorForOpcode(int opcode, IGameDataLookup lookup) {
        // Use providers in priority order (highest first)
        for (ITranslatorProvider provider : providers) {
            if (provider.supports(opcode, lookup)) {
                IPacketTranslator translator = provider.createTranslator(opcode, lookup);
                if (translator != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Created translator for opcode 0x{} using provider {} (priority: {})",
                                 String.format("%04X", opcode),
                                 provider.getClass().getSimpleName(),
                                 provider.getPriority());
                    }
                    return translator;
                }
            }
        }
        
        log.warn("No translator provider found for opcode: 0x{} (game: {})",
                String.format("%04X", opcode), lookup != null ? lookup.getGamePath() : "(no lookup)");
        return null;
    }
}
