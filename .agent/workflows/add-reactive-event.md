---
description: How to add a new reactive game event
---

# How to Add a New Reactive Game Event

This workflow describes how to define a new game event and ensure it is published through the `IReactiveEventBus`.

## 1. Define Event DTO
Events should be structured POJOs in `sokybot-game-events`.

```java
package org.sokybot.gameevents.events.core;

import lombok.Value;
import org.sokybot.game.dto.GamePosition;

@Value
public class MonsterSpawnEvent {
    int uniqueId;
    int refId;
    GamePosition position;
}
```

## 2. Implement Packet Translator
If the event comes from a game packet, update/implement a translator in `sokybot-game-events`.

## 3. Bridge to OSGi EventAdmin
In the component receiving the event (usually a packet handler or proxy listener), publish to OSGi `EventAdmin`.

```java
@Reference
private EventAdmin eventAdmin;

public void handle(byte[] data) {
    MonsterSpawnEvent pojo = // ... parse
    
    Map<String, Object> props = new HashMap<>();
    props.put("event", pojo);
    props.put("machineName", machineName);
    props.put("groupName", groupName);
    
    String topic = "sokybot/game/" + groupName + "." + machineName + "/MonsterSpawnEvent";
    eventAdmin.postEvent(new Event(topic, props));
}
```

## 4. Verification
The `ReactiveEventBusImpl` in `sokybot-commons` will automatically bridge this to the reactive flux. 

Verify by subscribing in `GameModelImpl` or a UI handler:
```java
eventBus.on(MonsterSpawnEvent.class).subscribe(e -> ...);
```
