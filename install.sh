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

# Parse command line arguments
USE_SNAPSHOT=false
DEBUG=false
# Use SCRIPT_ARGS if running through curl, otherwise use regular args
if [ ! -z "$SCRIPT_ARGS" ]; then
    set -- $SCRIPT_ARGS
fi

while [[ $# -gt 0 ]]; do
    case $1 in
        --snapshot) USE_SNAPSHOT=true; shift ;;
        --debug) DEBUG=true; shift ;;
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

# Determine user's shell for appropriate alias command
SHELL_NAME=$(basename "$SHELL")
if [[ "$SHELL_NAME" == "bash" ]]; then
    SHELL_RC="~/.bashrc"
    ALIAS_COMMAND="echo \"alias assistant=\\\"$SCRIPT_PATH\\\"\" >> ~/.bashrc"
elif [[ "$SHELL_NAME" == "zsh" ]]; then
    SHELL_RC="~/.zshrc"
    ALIAS_COMMAND="echo \"alias assistant=\\\"$SCRIPT_PATH\\\"\" >> ~/.zshrc"
else
    SHELL_RC="your shell's configuration file"
    ALIAS_COMMAND="alias assistant=\"$SCRIPT_PATH\""
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
echo "After adding the alias, restart your terminal or run:"
echo "    source $SHELL_RC"
echo ""
echo "Then you can run Manorrock Assistant by simply typing:"
echo "    assistant"
echo "════════════════════════════════════════════"

exit 0
