#compdef manorrock-coding-assistant.sh manorrock-coding-assistant

# Manorrock Coding Assistant - Auto-completion script for Zsh
# This script provides tab completion for the Manorrock Coding Assistant CLI on macOS (and other systems with zsh)
#
# Installation instructions:
# 1. Make sure this file is executable:
#    chmod +x manorrock-coding-assistant-completion.zsh
# 2. Create or add to your ~/.zshrc file:
#    fpath=(/path/to/script/directory $fpath)
#    autoload -Uz compinit && compinit
# 3. Either start a new terminal or run:
#    source ~/.zshrc
#
# Alternative installation:
# 1. Copy this file to any directory in your $fpath (e.g., /usr/local/share/zsh/site-functions/)
#    with an underscore prefix:
#    cp manorrock-coding-assistant-completion.zsh /usr/local/share/zsh/site-functions/_manorrock-coding-assistant
# 2. Run: compinit

# Define local scope variables
local -a commands command_args context_commands file_args
local expl cmd curcontext="$curcontext" state line context ret=1
local -A opt_args

# Main commands
commands=(
  'analyze:Analyze a file with language-specific understanding'
  'debug:Debug code with intelligent analysis'
  'fix:Get repair suggestions for problematic code'
  'generate:Generate code based on a description'
  'document:Auto-document existing code'
  'review:Perform a code review'
  'refactor:Get refactoring suggestions'
  'context:Manage multi-file context'
  'help:Show help message'
)

# File-related commands that need file completion
file_commands=('analyze' 'debug' 'fix' 'document' 'review' 'refactor')

# Context subcommands
context_commands=(
  'add:Add a file to the current context'
  'list:List files in the current context'
  'clear:Clear the current context'
  'use:Use the current context with a prompt'
)

# Common code file extensions for auto-completion
file_extensions=(
  '*.java' '*.py' '*.js' '*.ts' '*.jsx' '*.tsx' 
  '*.c' '*.cpp' '*.h' '*.hpp' '*.cc' 
  '*.rb' '*.php' '*.cs' '*.go' '*.rs' 
  '*.swift' '*.kt' '*.scala' 
  '*.html' '*.css' '*.scss' '*.sass' '*.less'
  '*.json' '*.yaml' '*.yml' '*.xml' '*.sql' 
  '*.md' '*.sh' '*.bash' '*.zsh'
)

# Define the completion function
_manorrock-coding-assistant() {
  _arguments -C \
    '1: :->command' \
    '*: :->args' && ret=0
  
  # Handle different argument positions
  case $state in
    command)
      # Complete the main commands
      _describe -t commands 'manorrock-coding-assistant commands' commands && ret=0
      ;;
    args)
      # Get the current command
      cmd="${line[1]}"
      
      case $cmd in
        analyze|debug|fix|document|review|refactor)
          # For file-related commands, provide file completion with preferred extensions
          if [[ -d $line[2] ]]; then
            # If argument is a directory, list its contents
            _path_files -W $line[2] -g "(${(j:|:)file_extensions})" && ret=0
          else
            # Otherwise provide normal file completion with extension filtering
            _path_files -g "(${(j:|:)file_extensions})" && ret=0
          fi
          ;;
        
        context)
          # Handle context subcommands
          if (( CURRENT == 2 )); then
            _describe -t context_commands 'context subcommands' context_commands && ret=0
          else
            # Get the context subcommand
            local subcmd="${line[2]}"
            
            case $subcmd in
              add)
                # Complete with files for 'add' subcommand
                _path_files && ret=0
                ;;
              use)
                # No completion for 'use' subcommand
                ;;
              list|clear)
                # No completion for 'list' or 'clear' subcommands
                ;;
            esac
          fi
          ;;
        
        generate)
          # No completion for 'generate' command
          ;;
        
        help)
          # No completion for 'help' command
          ;;
      esac
      ;;
  esac
  
  return ret
}

# Call the completion function
_manorrock-coding-assistant "$@"