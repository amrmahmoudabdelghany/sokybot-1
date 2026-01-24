#!/bin/bash
# =============================================================================
# Sokybot CLI Autocompletion
# =============================================================================
# Source this file in your ~/.bashrc or ~/.zshrc to enable tab completion
# for the 'soky' command.
#
# Usage: source path/to/soky-completion.bash
# =============================================================================

_soky_completions() {
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    
    # Top-level commands
    opts="backend ui build deploy clean status st setup kill install start stop restart logs shell"
    
    # 2nd level commands for 'backend'
    case "${prev}" in
        backend)
            COMPREPLY=( $(compgen -W "start stop restart debug logs shell clean features" -- ${cur}) )
            return 0
            ;;
        ui)
            COMPREPLY=( $(compgen -W "dev" -- ${cur}) )
            return 0
            ;;
        build)
            COMPREPLY=( $(compgen -W "backend ui all module --fast" -- ${cur}) )
            return 0
            ;;
        clean)
            COMPREPLY=( $(compgen -W "backend maven all" -- ${cur}) )
            return 0
            ;;
        deploy)
            # Dynamic module completion
            # Scan directories for potential modules
            local project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
            
            # Find directories that look like modules (contain pom.xml)
            if [ -d "$project_root" ]; then
                local modules=$(find "$project_root/core" "$project_root/game" "$project_root/network" "$project_root/ui" "$project_root/actuators" "$project_root/infra" -maxdepth 2 -name "pom.xml" -exec dirname {} \; 2>/dev/null | xargs -I {} basename {})
                COMPREPLY=( $(compgen -W "${modules}" -- ${cur}) )
            fi
            return 0
            ;;
    esac
    
    # If the previous word was 'module' (under build)
    if [[ "${COMP_WORDS[COMP_CWORD-2]}" == "build" && "${prev}" == "module" ]]; then
         local project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
            if [ -d "$project_root" ]; then
                local modules=$(find "$project_root/core" "$project_root/game" "$project_root/network" "$project_root/ui" "$project_root/actuators" "$project_root/infra" -maxdepth 2 -name "pom.xml" -exec dirname {} \; 2>/dev/null | xargs -I {} basename {})
                COMPREPLY=( $(compgen -W "${modules}" -- ${cur}) )
            fi
        return 0
    fi

    # Default: Top-level commands
    COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
}

complete -F _soky_completions soky
