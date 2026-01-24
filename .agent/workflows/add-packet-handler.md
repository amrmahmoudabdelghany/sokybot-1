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

## 4. Handle Event
Create an `EventHandler` to react to it.

```java
@Component(property = EventConstants.EVENT_TOPIC + "=sokybot/game/*/MyGameEvent")
public class MyHandler implements EventHandler {
    @Override
    public void handleEvent(Event event) {
        MyGameEvent myEvent = (MyGameEvent) event.getProperty("event");
        // Logic
    }
}
```
