#!/usr/bin/env bash

# Manorrock Coding Assistant - Auto-completion script for Bash
# This script supports auto-completion on macOS, Linux, and Windows Git Bash
#
# Installation instructions:
# - macOS/Linux: 
#   Add to ~/.bashrc or ~/.bash_profile: source /path/to/manorrock-coding-assistant-completion.bash
# 
# - Windows (Git Bash): 
#   Add to ~/.bashrc: source /path/to/manorrock-coding-assistant-completion.bash

# Determine OS
OS_TYPE="unknown"
if [[ "$OSTYPE" == "darwin"* ]]; then
    OS_TYPE="macos"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS_TYPE="linux"
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    OS_TYPE="windows-bash"
fi

_manorrock_coding_assistant_completions() {
    # OS-specific adjustments
    if [[ "$OS_TYPE" == "macos" ]]; then
        # macOS-specific settings if needed
        :
    elif [[ "$OS_TYPE" == "windows-bash" ]]; then
        # Windows Git Bash specific settings if needed
        :
    fi
    
    local cur prev opts cmd subcmd
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    
    # Top-level commands
    opts="analyze debug fix generate document review refactor context help"
    
    # If we're on the first word (the command)
    if [[ ${COMP_CWORD} -eq 1 ]]; then
        COMPREPLY=( $(compgen -W "${opts}" -- "${cur}") )
        return 0
    fi
    
    # Get the command (second word)
    cmd="${COMP_WORDS[1]}"
    
    # Complete based on the command
    case "${cmd}" in
        analyze|debug|fix|document|review|refactor)
            # Complete with files with common programming extensions
            local file_extensions="java py js ts jsx tsx c cpp h hpp rb php cs go rs swift kt scala html css json yaml yml xml sql md sh bash"
            
            # If this is a directory, list directories and files
            if [[ -d "${cur}" ]]; then
                COMPREPLY=( $(compgen -d -- "${cur}") )
                return 0
            fi
            
            # Complete with files matching these extensions
            for ext in ${file_extensions}; do
                COMPREPLY+=( $(compgen -f -X "!*.${ext}" -- "${cur}") )
            done
            
            # Add directory completion
            COMPREPLY+=( $(compgen -d -- "${cur}") )
            return 0
            ;;
            
        generate)
            # For generate, we don't offer completions beyond the command
            return 0
            ;;
            
        context)
            # Subcommands for context
            subcmd="${COMP_WORDS[2]}"
            
            # If we're completing the subcommand
            if [[ ${COMP_CWORD} -eq 2 ]]; then
                local context_opts="add list clear use"
                COMPREPLY=( $(compgen -W "${context_opts}" -- "${cur}") )
                return 0
            fi
            
            # Complete based on the subcommand
            case "${subcmd}" in
                add)
                    # Complete with files
                    if [[ -d "${cur}" ]]; then
                        COMPREPLY=( $(compgen -d -- "${cur}") )
                        return 0
                    fi
                    
                    # Offer file completion
                    COMPREPLY=( $(compgen -f -- "${cur}") )
                    return 0
                    ;;
                    
                use)
                    # For use, we don't offer completions beyond the subcommand
                    return 0
                    ;;
                    
                list|clear)
                    # These commands don't take arguments
                    return 0
                    ;;
            esac
            ;;
            
        help)
            # help doesn't take arguments
            return 0
            ;;
    esac
    
    # Default to file and directory completion
    if [[ -d "${cur}" ]]; then
        COMPREPLY=( $(compgen -d -- "${cur}") )
    else
        COMPREPLY=( $(compgen -f -- "${cur}") )
    fi
    
    return 0
}

# Register the completion function
complete -F _manorrock_coding_assistant_completions manorrock-coding-assistant.sh
complete -F _manorrock_coding_assistant_completions ./manorrock-coding-assistant.sh
complete -F _manorrock_coding_assistant_completions ./scripts/manorrock-coding-assistant.sh

# If the script is installed to /usr/local/bin or elsewhere, add that path too
if command -v manorrock-coding-assistant &>/dev/null; then
    complete -F _manorrock_coding_assistant_completions manorrock-coding-assistant
fi

# macOS-specific setup
if [[ "$OS_TYPE" == "macos" ]]; then
    # For macOS with Homebrew-installed bash-completion
    if [ -f "$(brew --prefix 2>/dev/null)/etc/bash_completion" ]; then
        echo "Bash completion is available via Homebrew. Add this script to $(brew --prefix)/etc/bash_completion.d/" >/dev/null
    fi
fi