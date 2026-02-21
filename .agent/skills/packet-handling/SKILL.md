---
name: Packet Handling & Events
description: How to add network packet handlers and reactive game events.
---

# Packet Handling & Events

Sokybot uses an Event-Driven Architecture bridging Netty packet parsing, OSGi EventAdmin, and Project Reactor Flux.

## 1. Network Layer & Packets

The network layer (`sokybot-proxy`) intercepts traffic between the Game Client and Server using Netty. 
- **Packet Mutability**: Treat received packets (`ImmutablePacket`) as read-only. Create a new `MutablePacket` to write/inject data.
- **Handling Strategy**: Do NOT block Netty handlers. Translate raw packets into high-level events using an `IPacketTranslator`.

## 2. Defining a New Packet Handler

To handle a new game server packet, translate it into a high-level Event.

### A. Define the Event DTO
Events should be structured POJOs in `sokybot-game-events-api` or `sokybot-game-events`.
```java
@Value
public class MyGameEvent implements IGameEvent {
    String machineName;
    int value;
}
```

### B. Create Translator
Implement `IPacketTranslator` in `sokybot-game-events`.
```java
@Component(service = IPacketTranslator.class)
public class MyPacketTranslator implements IPacketTranslator {
    @Override public int getOpcode() { return 0x30D2; }
    
    @Override public List<IGameEvent> translate(String machine, ImmutablePacket packet) {
        int value = packet.getInt();
        return Collections.singletonList(new MyGameEvent(machine, value));
    }
}
```

## 3. Emitting Custom Events to OSGi EventAdmin

If you're writing a completely custom proxy interceptor or internal module task that needs to emit an event manually:

```java
@Reference
private EventAdmin eventAdmin;

public void handleSomething() {
    MyGameEvent pojo = new MyGameEvent(machineName, 123);
    
    Map<String, Object> props = new HashMap<>();
    props.put("event", pojo);
    props.put("machineName", machineName);
    props.put("groupName", groupName);
    
    // REQUIRED: Topic Convention -> sokybot/game/{machineId}/{EventType}
    String topic = "sokybot/game/" + machineName + "/MyGameEvent";
    eventAdmin.postEvent(new Event(topic, props));
}
```

## 4. Consuming Events

The `ReactiveEventBusImpl` automatically bridges OSGi events to a reactive `Flux`. 

### Transient vs Persistent State
- **Transient Events** (e.g., chat, level up): Use `IReactiveEventBus` (`eventBus.on(MyGameEvent.class).subscribe(...)`).
- **Persistent State** (e.g., entity locations): Do not listen to raw events. Observe the `IGameModel` instead (`gameModel.observe(Character.class).subscribe(...)`).

## 5. Testing Handlers with CLI (`sokybot-dev-shell`)

When developing new packet handlers, do NOT restart the client/server repeatedly. Use the development CLI to inject fake packets and trigger your handler directly.

```bash
soky backend shell
# Inject a packet Hex string as if it came from the Server (e.g. to test a translator)
karaf@root()> dev:inject <groupName> <botName> "30D2 00 00 00 01 02"

# Inject a packet as if it came from the Client
karaf@root()> dev:inject <groupName> <botName> "30D2 00" --client
```
Use `dev:inspect` afterward to check if the internal `IGameModel` state updated correctly due to your handler processing the faked packet.
