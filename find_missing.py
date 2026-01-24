import re

registered = set()
with open("registered_opcodes.txt", "r") as f:
    for line in f:
        registered.add(line.strip().upper())

missing_candidates = []
with open("resources/docs/SilkroadDoc.wiki/Agent-packets.md", "r") as f:
    for line in f:
        # Match pattern with possible None or Opcodes in first two columns
        parts = line.split("|")
        if len(parts) >= 3:
            col1 = parts[0].strip().upper()
            col2 = parts[1].strip().upper()
            name = parts[2].strip()
            
            # Check S -> C column (Column 2)
            if col2.startswith("0X") and col2 not in registered:
                missing_candidates.append(f"{col2} | {name}")
            # Check Both directions logic if applicable (though rare in this file)
            # Some might be only in Column 1 if it's a notification from Client? 
            # Usually S -> C is what we want for translators.

for c in missing_candidates:
    print(c)
