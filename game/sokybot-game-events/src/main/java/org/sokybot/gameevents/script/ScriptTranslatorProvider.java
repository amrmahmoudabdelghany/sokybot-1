package org.sokybot.gameevents.script;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.*;
import org.sokybot.commons.script.AbstractScriptWatcher;
import org.sokybot.engine.api.scripting.IScriptEngine;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.events.core.ITranslatorProvider;
import org.sokybot.persistence.service.IGameDataLookup;

import groovy.lang.Closure;

/**
 * Loads packet translator definitions from Groovy scripts under
 * {@code scripts/translators/}. Each script uses a DSL to register
 * opcode handlers:
 *
 * <pre>
 * translator(0x3054) { machine, packet -&gt;
 *     def reader = packet.streamReader
 *     singleEvent(new LevelUpEvent(machine, reader.getInt()))
 * }
 * </pre>
 *
 * This provider plugs into the existing {@code ExtensibleTranslatorFactory}
 * via the OSGi {@code ITranslatorProvider} service.
 */
@Component(service = ITranslatorProvider.class, immediate = true,
        property = { "priority=50", "type=script" })
public class ScriptTranslatorProvider extends AbstractScriptWatcher implements ITranslatorProvider {

    private static final String DEFAULT_SCRIPTS_DIR = "scripts/translators";

    private final Map<Integer, Closure<?>> handlers = new ConcurrentHashMap<>();
    private final Map<Path, Set<Integer>> fileOpcodes = new ConcurrentHashMap<>();

    private IScriptEngine scriptEngine;

    public ScriptTranslatorProvider() {
        super(".groovy");
    }

    @Reference
    protected void setScriptEngine(IScriptEngine engine) {
        this.scriptEngine = engine;
    }

    @Activate
    protected void activate() {
        Path dir = Paths.get(System.getProperty("sokybot.translators.dir", DEFAULT_SCRIPTS_DIR));
        startWatching(dir);
    }

    @Deactivate
    protected void deactivate() {
        stopWatching();
    }

    @Override
    protected void onShutdown() {
        handlers.clear();
        fileOpcodes.clear();
    }

    @Override
    protected void onFileChanged(Path path) {
        loadTranslatorScript(path);
    }

    @Override
    protected void onFileRemoved(Path path) {
        Set<Integer> opcodes = fileOpcodes.remove(path);
        if (opcodes != null) {
            opcodes.forEach(handlers::remove);
            log.info("Removed {} scripted translators from {}", opcodes.size(), path.getFileName());
        }
    }

    private void loadTranslatorScript(Path scriptFile) {
        try {
            String content = Files.readString(scriptFile);

            var errors = scriptEngine.validate(content);
            if (!errors.isEmpty()) {
                log.error("Compilation errors in translator script {}: {}", scriptFile.getFileName(), errors);
                return;
            }

            TranslatorDslRegistry dsl = new TranslatorDslRegistry();
            Map<String, Object> context = new HashMap<>();
            context.put("__dsl", dsl);

            String wrappedScript = buildDslWrapper(content);
            scriptEngine.execute(wrappedScript, context);

            // Remove old opcodes from this file
            Set<Integer> oldOpcodes = fileOpcodes.remove(scriptFile);
            if (oldOpcodes != null) {
                oldOpcodes.forEach(handlers::remove);
            }

            // Register new ones (store raw closures)
            for (Map.Entry<Integer, Closure<?>> e : dsl.getTranslators().entrySet()) {
                handlers.put(e.getKey(), e.getValue());
            }
            fileOpcodes.put(scriptFile, new HashSet<>(dsl.getOpcodes()));

            log.info("Loaded {} translators from {}", dsl.size(), scriptFile.getFileName());
        } catch (IOException e) {
            log.error("Failed to read translator script {}: {}", scriptFile.getFileName(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to load translator script {}: {}", scriptFile.getFileName(), e.getMessage(), e);
        }
    }

    /**
     * Wrap the user script so the {@code translator()} DSL method is available.
     * The closure is stored as-is; singleEvent/noEvents/lookup are provided via
     * delegate (TranslatorContext) when the closure is invoked.
     */
    private String buildDslWrapper(String userScript) {
        return "def translator(int opcode, Closure handler) {\n"
                + "    __dsl.translator(opcode, handler)\n"
                + "}\n"
                + "\n"
                + userScript;
    }

    // ---- ITranslatorProvider ----

    @Override
    public boolean supports(int opcode, IGameDataLookup lookup) {
        return handlers.containsKey(opcode);
    }

    @Override
    public Set<Integer> getSupportedOpcodes(IGameDataLookup lookup) {
        return Collections.unmodifiableSet(new HashSet<>(handlers.keySet()));
    }

    @Override
    public IPacketTranslator createTranslator(int opcode, IGameDataLookup lookup) {
        Closure<?> closure = handlers.get(opcode);
        if (closure == null) return null;
        return new GroovyTranslator(opcode, closure, lookup);
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
