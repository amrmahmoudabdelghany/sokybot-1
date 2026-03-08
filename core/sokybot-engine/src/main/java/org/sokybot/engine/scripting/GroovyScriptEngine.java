package org.sokybot.engine.scripting;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sokybot.engine.api.scripting.IScriptEngine;
import org.sokybot.engine.api.scripting.ScriptException;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

/**
 * Groovy implementation of IScriptEngine.
 *
 * <p>Star imports can be configured via the system property
 * {@code sokybot.groovy.imports} (comma-separated package names).
 * Single-class imports can be configured via
 * {@code sokybot.groovy.imports.classes} (comma-separated FQCNs).
 * When not set, a built-in default list is used.
 */
@Component(service = IScriptEngine.class)
public class GroovyScriptEngine implements IScriptEngine {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptEngine.class);

    private static final List<String> DEFAULT_STAR_IMPORTS = List.of(
            "org.sokybot.engine.api.extension",
            "org.sokybot.engine.core.workflow.builder",
            "org.sokybot.engine.api.workflow",
            "org.sokybot.network.packet",
            "org.sokybot.network",
            "org.sokybot.settings.api",
            "org.sokybot.machinepages.api",
            "org.sokybot.runtime",
            "org.sokybot.gamemodel.model",
            "org.sokybot.gameevents.enums",
            "org.sokybot.gameevents.dto",
            "org.osgi.service.event",
            "org.slf4j"
    );

    private static final List<String> DEFAULT_CLASS_IMPORTS = List.of(
            "reactor.core.publisher.Flux",
            "reactor.core.publisher.Sinks"
    );

    private ExecutorService executor;
    private CompilerConfiguration config;
    private GroovyClassLoader sharedClassLoader;
    private final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    @Activate
    protected void activate() {
        this.executor = Executors.newCachedThreadPool();
        this.config = new CompilerConfiguration();

        ImportCustomizer imports = new ImportCustomizer();

        List<String> starImports = loadConfiguredList("sokybot.groovy.imports", DEFAULT_STAR_IMPORTS);
        imports.addStarImports(starImports.toArray(String[]::new));

        List<String> classImports = loadConfiguredList("sokybot.groovy.imports.classes", DEFAULT_CLASS_IMPORTS);
        imports.addImports(classImports.toArray(String[]::new));

        config.addCompilationCustomizers(imports);

        this.sharedClassLoader = new GroovyClassLoader(getClass().getClassLoader(), config);

        log.info("GroovyScriptEngine activated with {} star imports, {} class imports",
                starImports.size(), classImports.size());
    }

    @Deactivate
    protected void deactivate() {
        if (executor != null) {
            executor.shutdownNow();
        }
        classCache.clear();
        if (sharedClassLoader != null) {
            try { sharedClassLoader.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public Object execute(String script, Map<String, Object> context) throws ScriptException {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            if (context != null) {
                Object scriptName = context.get("scriptName");
                if (scriptName != null) {
                    MDC.put("sokybot.log.scriptName", scriptName.toString());
                }
                Object feature = context.get("feature");
                if (feature != null) {
                    MDC.put("sokybot.log.feature", feature.toString());
                }
            }

            Binding binding = new Binding();
            if (context != null) {
                context.forEach(binding::setVariable);
            }

            String hash = sha256(script);
            Class<?> scriptClass = classCache.computeIfAbsent(hash,
                    h -> sharedClassLoader.parseClass(script));

            Script instance = (Script) scriptClass.getDeclaredConstructor().newInstance();
            instance.setBinding(binding);
            return instance.run();
        } catch (Exception e) {
            throw new ScriptException("Script execution failed", e);
        } finally {
            MDC.remove("sokybot.log.scriptName");
            MDC.remove("sokybot.log.feature");
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    public void invalidateCache() {
        classCache.clear();
    }

    @Override
    public CompletableFuture<Object> executeAsync(String script, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> execute(script, context), executor);
    }

    @Override
    public java.util.List<String> validate(String script) {
        try {
            sharedClassLoader.parseClass(script);
            return java.util.Collections.emptyList();
        } catch (org.codehaus.groovy.control.CompilationFailedException e) {
            return java.util.List.of(e.getMessage());
        } catch (Exception e) {
            return java.util.List.of("Unexpected error: " + e.getMessage());
        }
    }

    @Override
    public String getLanguageName() {
        return "Groovy";
    }

    /**
     * Load a comma-separated list from a system property, falling back to the
     * given defaults when the property is absent or empty.
     */
    private static List<String> loadConfiguredList(String property, List<String> defaults) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            return defaults;
        }
        List<String> result = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return result.isEmpty() ? defaults : result;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
