package org.sokybot.machinepages.test.mocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sokybot.engine.IEngine;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.LoginState;
import org.sokybot.gamemodel.model.ISpawn;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.runtime.IMachineContext;

import reactor.core.publisher.Flux;

/**
 * Mock IMachineContext for offline page script testing.
 * Provides configurable game model, proxy connection, and service registry.
 *
 * <pre>
 * def ctx = new MockMachineContext("test", "bot1")
 *     .withService(ISettingsRegistry, settingsRegistry)
 *
 * def page = new HealingPage()
 * page.init(ctx)
 * assert page.getTitle() == "Healing"
 * </pre>
 */
public class MockMachineContext implements IMachineContext {

    private final String groupName;
    private final String machineName;
    private final MockGameModel gameModel;
    private IProxyConnection proxyConnection;
    private IEngine engine;
    private boolean running = false;
    private final Map<Class<?>, Object> services = new HashMap<>();

    public MockMachineContext(String groupName, String machineName) {
        this.groupName = groupName;
        this.machineName = machineName;
        this.gameModel = new MockGameModel();
    }

    public <T> MockMachineContext withService(Class<T> type, T instance) {
        services.put(type, instance);
        return this;
    }

    public MockMachineContext withRunning(boolean running) {
        this.running = running;
        return this;
    }

    public MockMachineContext withProxyConnection(IProxyConnection connection) {
        this.proxyConnection = connection;
        return this;
    }

    @Override
    public String fullName() {
        return groupName + "." + machineName;
    }

    @Override
    public String name() {
        return machineName;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public IEngine getEngine() {
        return engine;
    }

    @Override
    public IProxyConnection getProxyConnection() {
        return proxyConnection;
    }

    @Override
    public IGameModel getGameModel() {
        return gameModel;
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public String getMachineName() {
        return machineName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }

    public MockGameModel getTestGameModel() {
        return gameModel;
    }

    /**
     * Simple in-memory IGameModel for testing.
     */
    public static class MockGameModel implements IGameModel {

        private ITrainer trainer;
        private final Map<Integer, ISpawn> spawns = new HashMap<>();
        private final LoginState loginState = new LoginState();

        public MockGameModel withTrainer(ITrainer trainer) {
            this.trainer = trainer;
            return this;
        }

        @Override
        public java.util.Optional<ISpawn> find(int id) {
            return java.util.Optional.ofNullable(spawns.get(id));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ISpawn> java.util.Optional<T> findLive(int id, Class<T> type) {
            ISpawn s = spawns.get(id);
            return (s != null && type.isInstance(s)) ? java.util.Optional.of((T) s) : java.util.Optional.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ISpawn> java.util.Optional<T> snapshot(int id, Class<T> type) {
            ISpawn s = spawns.get(id);
            return (s != null && type.isInstance(s)) ? java.util.Optional.of((T) s) : java.util.Optional.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ISpawn> List<T> snapshotAll(Class<T> type) {
            java.util.List<T> result = new java.util.ArrayList<>();
            for (var entry : spawns.entrySet()) {
                if (type.isInstance(entry.getValue())) {
                    result.add((T) entry.getValue());
                }
            }
            return result;
        }

        @Override
        public java.util.Optional<ISpawn> getSelected() {
            return java.util.Optional.empty();
        }

        @Override
        public ITrainer getTrainer() {
            return trainer;
        }

        @Override
        public LoginState getLoginState() {
            return loginState;
        }

        @Override
        public <T extends ISpawn> Flux<T> observe(int id, Class<T> type) {
            return Flux.empty();
        }

        @Override
        public <T extends ISpawn> Flux<org.sokybot.gamemodel.ModelUpdate<T>> observeAll(Class<T> type) {
            return Flux.empty();
        }
    }
}
