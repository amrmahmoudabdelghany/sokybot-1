
import os
import re

def fix_empty_catch(directory):
    count = 0
    # Walk through all files in the directory
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith("Translator.java"):
                params = os.path.join(root, file)
                with open(params, 'r') as f:
                    content = f.read()
                
                # Regex to find catch (Exception e) { \s* } and replace with return noEvents();
                # Also handle cases where catch block is empty across lines
                
                # Pattern: catch (Exception e) { [whitespace] }
                pattern = r'(catch\s*\(\s*Exception\s+e\s*\)\s*\{\s*)(\})'
                replacement = r'\1            return noEvents();\n        \2'
                
                new_content = re.sub(pattern, replacement, content)
                
                # Also handle the generated empty blocks from my previous script (which might be } catch { })
                
                if new_content != content:
                    print(f"Fixed empty catch in: {file}")
                    with open(params, 'w') as f:
                        f.write(new_content)
                    count += 1
    print(f"Total files fixed: {count}")

if __name__ == "__main__":
    fix_empty_catch("/home/amr/sokybot-workspace/sokybot-workspace/sokybot/sokybot-game-events/src/main/java/org/sokybot/gameevents/internal")
