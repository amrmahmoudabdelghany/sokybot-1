import os
import re
import sys

def migrate_pom(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    if '<packaging>bundle</packaging>' not in content:
        return False

    print(f"Migrating {filepath}")
    
    # 1. Change packaging
    content = content.replace('<packaging>bundle</packaging>', '<packaging>jar</packaging>')

    # 2. Extract maven-bundle-plugin block
    plugin_pattern = re.compile(r'<plugin>\s*<groupId>org\.apache\.felix</groupId>\s*<artifactId>maven-bundle-plugin</artifactId>.*?</plugin>', re.DOTALL)
    
    match = plugin_pattern.search(content)
    if not match:
        print(f"Warning: No maven-bundle-plugin found in {filepath}")
        with open(filepath, 'w') as f:
            f.write(content)
        return True

    plugin_block = match.group(0)
    
    # 3. Extract instructions
    instructions_pattern = re.compile(r'<instructions>(.*?)</instructions>', re.DOTALL)
    inst_match = instructions_pattern.search(plugin_block)
    
    bnd_lines = []
    has_embed = False
    embed_deps = []
    
    if inst_match:
        instructions = inst_match.group(1)
        # Parse XML tags
        tag_pattern = re.compile(r'<([^>]+)>(.*?)</\1>', re.DOTALL)
        for tag_match in tag_pattern.finditer(instructions):
            tag = tag_match.group(1).strip()
            val = tag_match.group(2).strip()
            
            # Translate tags
            if tag == '_dsannotations':
                bnd_lines.append(f'-dsannotations: {val}')
            elif tag == '_removeheaders':
                bnd_lines.append(f'-removeheaders: {val}')
            elif tag == 'Embed-Dependency':
                if 'compile' in val:
                    # In real code we only have groovy and commons-io
                    dep = val.split(';')[0]
                    embed_deps.append(dep)
                    bnd_lines.append(f'-conditionalpackage: org.apache.commons.io.*, groovy.*, org.codehaus.groovy.*')
                    has_embed = True
            elif tag == 'Embed-Transitive':
                pass # Bnd does transitive embedding differently, usually not needed if we embed via -conditionalpackage
            else:
                # Normal tags (Export-Package, Import-Package, Private-Package, etc)
                # Ensure they don't break on multiple lines
                val = val.replace('\n', ' ').replace('\r', '')
                val = re.sub(r'\s+', ' ', val)
                bnd_lines.append(f'{tag}: {val}')
    
    # 4. Remove plugin block
    content = content[:match.start()] + content[match.end():]
    
    # Write back pom.xml
    with open(filepath, 'w') as f:
        f.write(content)
        
    # 5. Write bnd.bnd
    if bnd_lines:
        bnd_path = os.path.join(os.path.dirname(filepath), 'bnd.bnd')
        with open(bnd_path, 'w') as f:
            f.write('\n'.join(bnd_lines) + '\n')
            
    return True

if __name__ == '__main__':
    count = 0
    for root, dirs, files in os.walk('.'):
        # skip target, node_modules, etc.
        if 'target' in dirs:
            dirs.remove('target')
        if 'node_modules' in dirs:
            dirs.remove('node_modules')
        if '.git' in dirs:
            dirs.remove('.git')
            
        for file in files:
            if file == 'pom.xml':
                filepath = os.path.join(root, file)
                if 'sokybot-webview' in filepath:
                    continue # already handled manually
                if migrate_pom(filepath):
                    count += 1
    print(f"Migrated {count} bundles.")
