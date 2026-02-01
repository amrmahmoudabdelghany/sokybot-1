package org.sokybot.engine.scripting;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.engine.api.scripting.IScriptEngine;
import org.sokybot.engine.api.scripting.ScriptException;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

/**
 * Groovy implementation of IScriptEngine.
 */
@Component(service = IScriptEngine.class)
public class GroovyScriptEngine implements IScriptEngine {

    private ExecutorService executor;
    private CompilerConfiguration config;

    @Activate
    protected void activate() {
        this.executor = Executors.newCachedThreadPool();
        this.config = new CompilerConfiguration();
        this.config.setScriptBaseClass(null); // Use default
    }

    @Deactivate
    protected void deactivate() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public Object execute(String script, Map<String, Object> context) throws ScriptException {
        try {
            Binding binding = new Binding();
            if (context != null) {
                context.forEach(binding::setVariable);
            }

            GroovyShell shell = new GroovyShell(binding, config);
            return shell.evaluate(script);
        } catch (Exception e) {
            throw new ScriptException("Script execution failed", e);
        }
    }

    @Override
    public CompletableFuture<Object> executeAsync(String script, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> execute(script, context), executor);
    }

    @Override
    public String getLanguageName() {
        return "Groovy";
    }
}
