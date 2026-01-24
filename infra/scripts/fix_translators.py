import os

directory = "/home/amr/sokybot-workspace/sokybot-workspace/sokybot/sokybot-game-events/src/main/java/org/sokybot/gameevents/internal"

for filename in os.listdir(directory):
    if filename.endswith("Translator.java"):
        filepath = os.path.join(directory, filename)
        with open(filepath, 'r') as f:
            lines = f.readlines()
        
        needs_fix = False
        insert_idx = -1
        
        for i, line in enumerate(lines):
            stripped = line.strip()
            if "protected List<IGameEvent> translateInternal" in line:
                # Check previous non-empty lines
                prev_idx = i - 1
                while prev_idx >= 0 and not lines[prev_idx].strip():
                    prev_idx -= 1
                
                if prev_idx >= 0:
                    prev_line = lines[prev_idx].strip()
                    # If previous line is return statement (from opcode) and NOT "}", it's broken
                    # Also checking if it ends with ; to avoid multi-line returns although unlikely here
                    if prev_line.startswith("return") and prev_line.endswith(";") and "}" not in prev_line:
                         needs_fix = True
                         insert_idx = i
                         break
                    
                    # If previous line is specific annotation (Override)
                    if prev_line.startswith("@Override"):
                        # Check line before THAT
                         prev_idx2 = prev_idx - 1
                         while prev_idx2 >= 0 and not lines[prev_idx2].strip():
                             prev_idx2 -= 1
                         if prev_idx2 >= 0:
                             prev_line2 = lines[prev_idx2].strip()
                             if prev_line2.startswith("return") and prev_line2.endswith(";") and "}" not in prev_line2:
                                 needs_fix = True
                                 insert_idx = prev_idx # Insert before @Override
                                 break
        
        if needs_fix:
            print(f"Fixing {filename}")
            lines.insert(insert_idx, "    }\n")
            
            # Check if file ends with newline
            if lines[-1].strip() == "}":
                 # If it somehow ends with }, append another } (Class close)
                 # But usually it ends with empty line or method close if broken
                 lines.append("}\n")
            else:
                 if not lines[-1].endswith("\n"):
                      lines[-1] = lines[-1] + "\n"
                 lines.append("}\n")

            with open(filepath, 'w') as f:
                f.writelines(lines)
