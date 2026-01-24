---
trigger: always_on
glob: "**/*.java"
description: Guidelines for using the `resources/` directory to lookup game logic and data
---

# Game Data Access Rules

The workspace contains a comprehensive database of extracted game client data in the `resources/` directory.

## 1. Resource Location
**Path**: `/home/amr/sokybot-workspace/sokybot-workspace/resources`

## 2. Contents
This directory contains:
- **`codebase/Joymax Search`**: Extracted text data, script logic, and potentially decompiled client code references.
- **Strings and IDs**: Useful for mapping OpCodes, Item IDs, NPC names, and error messages.

## 3. Usage Guidelines
When analyzing packets, implementing game logic, or debugging unknown values:

1.  **Search First**: Use `grep_search` within `resources/` to find magic numbers, packet structure definitions, or text strings.
    ```bash
    grep -r "0x3095" resources/
    grep -r "ITEM_ETC_GOLD" resources/
    ```

2.  **Reverse Engineering**: Use the resource files to understand the *original* game client behavior and replicate it in the bot.

3.  **Consistency**: Match variable naming to the conventions found in these resources where applicable to maintain alignment with the game protocol.
