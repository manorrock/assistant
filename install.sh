#!/bin/bash

# Manorrock Assistant Installer
# This script downloads the latest release of Manorrock Assistant
# and places it in ~/.manorrock/assistant/cli.jar

set -e

# If this script was downloaded and piped to bash, the arguments need to be passed through
# Get all arguments passed to the script
SCRIPT_ARGS="$@"

# Print banner first
echo "════════════════════════════════════════════"
echo "    Manorrock Assistant Installer"
echo "════════════════════════════════════════════"
echo ""

INSTALL_DIR="$HOME/.manorrock/assistant"
JAR_PATH="$INSTALL_DIR/cli.jar"
SCRIPT_PATH="$INSTALL_DIR/assistant"
ACE_SCRIPT_PATH="$INSTALL_DIR/ace-coding"
CONFIG_DIR="$INSTALL_DIR/config"
TEMPLATES_DIR="$CONFIG_DIR/templates"

# Parse command line arguments
USE_SNAPSHOT=false
DEBUG=false
INSTALL_ACE=true
# Use SCRIPT_ARGS if running through curl, otherwise use regular args
if [ ! -z "$SCRIPT_ARGS" ]; then
    set -- $SCRIPT_ARGS
fi

while [[ $# -gt 0 ]]; do
    case $1 in
        --snapshot) USE_SNAPSHOT=true; shift ;;
        --debug) DEBUG=true; shift ;;
        --no-ace) INSTALL_ACE=false; shift ;;
        *) shift ;;
    esac
done

# Check if GitHub API is accessible
check_github_api() {
    if ! curl -s -f -I "$GITHUB_API" > /dev/null; then
        echo "Warning: Cannot access GitHub API. Check your internet connection."
        return 1
    fi
    return 0
}

# Function to pretty print JSON
pretty_print_json() {
    echo "$1" | python3 -m json.tool 2>/dev/null || echo "$1"
}

# Function to print debug information
debug_info() {
    if [ "$DEBUG" = true ]; then
        echo "[DEBUG] $1"
        if [ ! -z "$2" ]; then
            pretty_print_json "$2"
        fi
    fi
}

if [ "$USE_SNAPSHOT" = true ]; then
    DOWNLOAD_URL="https://github.com/manorrock/assistant/releases/download/SNAPSHOT/Manorrock-Assistant.jar"
    VERSION="SNAPSHOT"
else
    GITHUB_API="https://api.github.com/repos/manorrock/assistant/releases/latest"
    echo "Fetching latest release information..."
    if ! check_github_api; then
        echo "Warning: Cannot access GitHub API. Check your internet connection."
        echo "Falling back to SNAPSHOT build..."
        DOWNLOAD_URL="https://github.com/manorrock/assistant/releases/download/SNAPSHOT/Manorrock-Assistant.jar"
        VERSION="SNAPSHOT"
    else
        RELEASE_INFO=$(curl -s -H "Accept: application/vnd.github.v3+json" $GITHUB_API)
        debug_info "GitHub API Response:" "$RELEASE_INFO"
        DOWNLOAD_URL=$(echo $RELEASE_INFO | grep -o '"browser_download_url": "[^"]*"' | grep 'Manorrock-Assistant.jar"' | cut -d'"' -f4)
        VERSION=$(echo $RELEASE_INFO | grep -o '"tag_name": "[^"]*"' | cut -d'"' -f4)

        if [ -z "$DOWNLOAD_URL" ] || [ -z "$VERSION" ]; then
            if [ "$DEBUG" = true ]; then
                echo "No stable release found. Debug information:"
                echo "----------------------------------------"
                echo "API Response:"
                pretty_print_json "$RELEASE_INFO"
                echo "----------------------------------------"
            else
                echo "No stable release found."
            fi
            echo "Falling back to SNAPSHOT build..."
            DOWNLOAD_URL="https://github.com/manorrock/assistant/releases/download/SNAPSHOT/Manorrock-Assistant.jar"
            VERSION="SNAPSHOT"
        else
            echo "Found stable release version: $VERSION"
            debug_info "Download URL: $DOWNLOAD_URL"
        fi
    fi
fi

# Create installation directory if it doesn't exist
echo "Creating installation directory..."
mkdir -p "$INSTALL_DIR"
mkdir -p "$CONFIG_DIR"
mkdir -p "$TEMPLATES_DIR"

# Download the JAR file
echo "Downloading Manorrock Assistant ${VERSION}..."
if ! curl -L -f -# "$DOWNLOAD_URL" -o "$JAR_PATH"; then
    echo "Error: Download failed. Please check your internet connection and try again."
    exit 1
fi

# Set permissions
echo "Setting permissions..."
chmod +x "$JAR_PATH"

# Create the executable shell script
echo "Creating executable script..."
cat > "$SCRIPT_PATH" << 'EOF'
#!/bin/bash

# Manorrock Assistant Runner
# This script runs the Manorrock Assistant from the current directory

JAR_PATH="$HOME/.manorrock/assistant/cli.jar"

# Run the JAR file from the current directory
# The JVM will naturally use the current directory as user.dir
java -jar "$JAR_PATH" "$@"
EOF

# Make the script executable
chmod +x "$SCRIPT_PATH"

# Install ACE coding assistant if requested
if [ "$INSTALL_ACE" = true ]; then
    echo "Installing ACE coding assistant..."
    
    # Download the coding assistant wrapper script directly from GitHub
    WRAPPER_SCRIPT_URL="https://raw.githubusercontent.com/manorrock/assistant/main/scripts/ace-coding.sh"
    if ! curl -L -f -s "$WRAPPER_SCRIPT_URL" -o "$ACE_SCRIPT_PATH"; then
        echo "Error: Could not download ACE coding assistant script from GitHub."
        echo "Make sure you have internet connectivity."
        INSTALL_ACE=false
    else
        echo "Downloaded ACE coding assistant script successfully."
        
        # Make the ACE script executable
        chmod +x "$ACE_SCRIPT_PATH"
        
        # Update the script to use the correct JAR path
        sed -i.bak "s|CLI_JAR=.*|CLI_JAR=\"$JAR_PATH\"|g" "$ACE_SCRIPT_PATH" || true
        rm -f "${ACE_SCRIPT_PATH}.bak"
        
        # Create default templates
        echo "Creating default templates..."
        echo "You are a senior developer conducting a code review. Focus on identifying potential bugs, performance issues, security vulnerabilities, and areas for improvement. Provide constructive feedback and suggest specific solutions." > "$TEMPLATES_DIR/code-review.txt"
        echo "You are a debugging specialist. Analyze code errors systematically, identify root causes, and propose precise solutions. Focus on examining error messages, tracing code execution, and recognizing common bug patterns." > "$TEMPLATES_DIR/debug.txt"
        echo "You are a refactoring expert. Analyze code for readability, maintainability, and design improvements without changing functionality. Apply appropriate design patterns, reduce complexity, and eliminate code smells." > "$TEMPLATES_DIR/refactor.txt"
        echo "You are a technical documentation expert. Create clear, concise, and comprehensive documentation for code. Focus on explaining purpose, usage, parameters, return values, and include appropriate examples." > "$TEMPLATES_DIR/document.txt"
        echo "You are an expert code generator. Write clean, efficient, and well-structured code based on requirements. Focus on following best practices, proper error handling, and optimizing for readability and maintainability." > "$TEMPLATES_DIR/generate.txt"
    fi
    
    # Download shell completions if ACE installation was successful
    # if [ "$INSTALL_ACE" = true ]; then
        # echo "Installing shell completions for ACE..."
        # mkdir -p "$CONFIG_DIR/completions"
        
        # Download Bash completion
        #if curl -L -f -s "https://raw.githubusercontent.com/manorrock/assistant/main/scripts/ace-coding-completion.bash" \
        #        -o "$CONFIG_DIR/completions/ace-coding-completion.bash"; then
        #    echo "Downloaded Bash completion for ACE."
        #else
        #    echo "Warning: Could not download Bash completion for ACE."
        #fi
        
        # Download Zsh completion
        #if curl -L -f -s "https://raw.githubusercontent.com/manorrock/assistant/main/scripts/ace-coding-completion.zsh" \
        #        -o "$CONFIG_DIR/completions/ace-coding-completion.zsh"; then
        #    echo "Downloaded Zsh completion for ACE."
        #else
        #    echo "Warning: Could not download Zsh completion for ACE."
        #fi
        
        # Download PowerShell completion
        #if curl -L -f -s "https://raw.githubusercontent.com/manorrock/assistant/main/scripts/ace-coding-completion.ps1" \
        #        -o "$CONFIG_DIR/completions/ace-coding-completion.ps1"; then
        #    echo "Downloaded PowerShell completion for ACE."
        #else
        #    echo "Warning: Could not download PowerShell completion for ACE."
        #fi
    fi
fi

# Determine user's shell for appropriate alias command
SHELL_NAME=$(basename "$SHELL")
if [[ "$SHELL_NAME" == "bash" ]]; then
    SHELL_RC="~/.bashrc"
    ALIAS_COMMAND="echo \"alias assistant=\\\"$SCRIPT_PATH\\\"\" >> ~/.bashrc"
#    if [ "$INSTALL_ACE" = true ]; then
#        ACE_ALIAS_COMMAND="echo \"alias ace-coding=\\\"$ACE_SCRIPT_PATH\\\"\" >> ~/.bashrc"
#        COMPLETION_COMMAND="echo \"source $CONFIG_DIR/completions/ace-coding-completion.bash\" >> ~/.bashrc"
#    fi
elif [[ "$SHELL_NAME" == "zsh" ]]; then
    SHELL_RC="~/.zshrc"
    ALIAS_COMMAND="echo \"alias assistant=\\\"$SCRIPT_PATH\\\"\" >> ~/.zshrc"
    #if [ "$INSTALL_ACE" = true ]; then
    #    ACE_ALIAS_COMMAND="echo \"alias ace-coding=\\\"$ACE_SCRIPT_PATH\\\"\" >> ~/.zshrc"
    #    COMPLETION_COMMAND="echo \"source $CONFIG_DIR/completions/ace-coding-completion.zsh\" >> ~/.zshrc"
    #fi
else
    SHELL_RC="your shell's configuration file"
    ALIAS_COMMAND="alias assistant=\"$SCRIPT_PATH\""
    #if [ "$INSTALL_ACE" = true ]; then
    #    ACE_ALIAS_COMMAND="alias ace-coding=\"$ACE_SCRIPT_PATH\""
    #    COMPLETION_COMMAND="# Source the appropriate completion file for your shell"
    #fi
fi

# Create a helpful message about how to use it
echo ""
echo "════════════════════════════════════════════"
echo "Installation completed successfully!"
echo "Manorrock Assistant version ${VERSION} has been installed to:"
echo "$JAR_PATH"
echo ""
echo "A convenient script has been created at:"
echo "$SCRIPT_PATH"
echo ""
echo "To easily run Manorrock Assistant from anywhere, add this alias to $SHELL_RC:"
echo ""
echo "    alias assistant=\"$SCRIPT_PATH\""
echo ""
echo "You can do this by running:"
echo "    $ALIAS_COMMAND"
echo ""

if [ "$INSTALL_ACE" = true ]; then
    echo "The ACE coding assistant has been installed to:"
    echo "$ACE_SCRIPT_PATH"
    echo ""
    echo "To easily run ACE coding assistant from anywhere, add this alias to $SHELL_RC:"
    echo ""
    echo "    alias ace-coding=\"$ACE_SCRIPT_PATH\""
    echo ""
    echo "You can do this by running:"
    echo "    $ACE_ALIAS_COMMAND"
    echo ""
    echo "For shell completion (tab completion), add this to $SHELL_RC:"
    echo "    $COMPLETION_COMMAND"
    echo ""
    echo "ACE coding assistant provides specialized commands for code analysis:"
    echo "    ace-coding analyze <file>    - Analyze a file's code"
    echo "    ace-coding debug <file>      - Debug problematic code"
    echo "    ace-coding review <file>     - Get a code review"
    echo "    ace-coding refactor <file>   - Get refactoring suggestions"
    echo "    ace-coding document <file>   - Generate documentation for code"
    echo "    ace-coding fix <file>        - Get fix suggestions for code"
    echo "    ace-coding generate <desc>   - Generate code from description"
    echo "    ace-coding help              - See all available commands"
    echo ""
fi

echo "After adding the alias(es), restart your terminal or run:"
echo "    source $SHELL_RC"
echo ""
echo "Then you can run Manorrock Assistant by simply typing:"
echo "    assistant"
if [ "$INSTALL_ACE" = true ]; then
    echo "Or the ACE coding assistant by typing:"
    echo "    ace-coding"
fi
echo "════════════════════════════════════════════"

exit 0
