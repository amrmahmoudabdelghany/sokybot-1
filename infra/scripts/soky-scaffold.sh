#!/bin/bash
# =============================================================================
# Sokybot Scaffolding Tool
# =============================================================================
# Generates boilerplate code for new bundles.
# Usage: ./infra/scripts/soky-scaffold.sh bundle <category> <name>
# =============================================================================

set -e

# resolve script dir
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

cmd_bundle() {
    local CATEGORY=$1
    local NAME=$2
    
    if [ -z "$CATEGORY" ] || [ -z "$NAME" ]; then
        echo "Usage: soky generate bundle <category> <name>"
        echo "Categories: core, game, network, ui, infra, actuators"
        exit 1
    fi
    
    # Normalize name (remove sokybot- prefix if present)
    NAME=${NAME#sokybot-}
    ARTIFACT_ID="sokybot-${NAME}"
    PACKAGE_NAME="org.sokybot.${NAME//-/.}" # replace - with .
    
    # Locate category dir
    CATEGORY_DIR="$ROOT_DIR/$CATEGORY"
    if [ ! -d "$CATEGORY_DIR" ]; then
        log_error "Category directory '$CATEGORY' not found."
        exit 1
    fi
    
    MODULE_DIR="$CATEGORY_DIR/$ARTIFACT_ID"
    
    if [ -d "$MODULE_DIR" ]; then
        log_error "Module '$ARTIFACT_ID' already exists in $CATEGORY"
        exit 1
    fi
    
    log_info "Creating bundle '$ARTIFACT_ID' in '$CATEGORY'..."
    
    # 1. Create Directory Structure
    mkdir -p "$MODULE_DIR/src/main/java/${PACKAGE_NAME//.//}/api"
    mkdir -p "$MODULE_DIR/src/main/java/${PACKAGE_NAME//.//}/internal"
    
    # 2. Create POM
    # Capitalize first letter of name for display name
    NAME_CAP="$(tr '[:lower:]' '[:upper:]' <<< ${NAME:0:1})${NAME:1}"
    
    cat > "$MODULE_DIR/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.github.sokybot</groupId>
        <artifactId>sokybot</artifactId>
        <version>1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>${ARTIFACT_ID}</artifactId>
    <packaging>bundle</packaging>
    <name>Sokybot ${NAME_CAP}</name>
    <description>Sokybot module for ${NAME}</description>

    <dependencies>
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>osgi.core</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>org.osgi.service.component.annotations</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <extensions>true</extensions>
                <configuration>
                    <instructions>
                        <Export-Package>${PACKAGE_NAME}.api.*</Export-Package>
                        <Private-Package>${PACKAGE_NAME}.internal.*</Private-Package>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

    log_success "Created module structure and pom.xml"
    
    # 3. Register in Parent POM
    CATEGORY_POM="$CATEGORY_DIR/pom.xml"
    if [ -f "$CATEGORY_POM" ]; then
        # Check if modules section exists
        if grep -q "<modules>" "$CATEGORY_POM"; then
            # Insert before </modules>
            # Use sed to insert the module line
            # We use a temporary file to avoid complex sed in-place issues across OS versions
            sed "/<\/modules>/i \        <module>${ARTIFACT_ID}</module>" "$CATEGORY_POM" > "${CATEGORY_POM}.tmp" && mv "${CATEGORY_POM}.tmp" "$CATEGORY_POM"
            log_success "Registered module in $CATEGORY/pom.xml"
        else
            log_error "Could not find <modules> tag in $CATEGORY_POM. Please add <module>${ARTIFACT_ID}</module> manually."
        fi
    else
         log_error "Category POM not found at $CATEGORY_POM. skipping registration."
    fi
    
    echo ""
    log_info "Bundle created successfully!"
    echo "  Path: $MODULE_DIR"
    echo "  Package: $PACKAGE_NAME"
    echo ""
    echo "Next steps:"
    echo "  1. Add your code to src/main/java"
    echo "  2. Build: soky build module ${ARTIFACT_ID}"
}

case "$1" in
    bundle)
        shift
        cmd_bundle "$@"
        ;;
    *)
        echo "Usage: soky generate [bundle]"
        exit 1
        ;;
esac
