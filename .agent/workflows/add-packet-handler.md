---
description: How to add a new Packet Handler
---

# How to Add a New Packet Handler

To handle a new game server packet, you need to translate it into a high-level Event.

## 1. Identify Packet
Find the Opcode (e.g., `0x30D2`) and structure of the packet from documentation or packet sniffer.

## 2. Create Translator
Implement `IPacketTranslator` in `sokybot-game-events`.

```java
@Component(service = IPacketTranslator.class)
public class MyPacketTranslator implements IPacketTranslator {
    @Override
    public int getOpcode() { return 0x30D2; }
    
    @Override
    public List<IGameEvent> translate(String machine, ImmutablePacket packet) {
        // Parse packet
        int value = packet.getInt();
        return Collections.singletonList(new MyGameEvent(machine, value));
    }
}
```

## 3. Define Game Event
Create a simple POJO for the event in `sokybot-game-events-api` (or `sokybot-game-events` if not shared API).

```java
@Data
@AllArgsConstructor
public class MyGameEvent implements IGameEvent {
    private String machineName;
    private int value;
    // ... implementation ...
}
```

## 4. Handle Event (Subscriber)
The preferred way to react to game events is using the `IReactiveEventBus`.

```java
@Component(service = MyComponent.class)
public class MyComponent {

    @Reference
    private IReactiveEventBus eventBus;
    
    @Activate
    public void start() {
        eventBus.on(MyGameEvent.class)
                .subscribe(this::handleEvent);
    }
}
```

**Note**: Raw `EventHandler` service registration is still supported but discouraged for application logic.
