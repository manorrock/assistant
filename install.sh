#!/bin/bash

# Manorrock Assistant Installer
# This script downloads the latest SNAPSHOT JAR release of Manorrock Assistant
# and places it in ~/.manorrock/assistant/cli.jar

set -e

INSTALL_DIR="$HOME/.manorrock/assistant"
JAR_PATH="$INSTALL_DIR/cli.jar"
SCRIPT_PATH="$INSTALL_DIR/assistant"
DOWNLOAD_URL="https://github.com/manorrock/assistant/releases/download/SNAPSHOT/Manorrock-Assistant.jar"

# Print banner
echo "════════════════════════════════════════════"
echo "    Manorrock Assistant Installer"
echo "════════════════════════════════════════════"
echo ""

# Create installation directory if it doesn't exist
echo "Creating installation directory..."
mkdir -p "$INSTALL_DIR"

# Download the JAR file
echo "Downloading Manorrock Assistant JAR..."
curl -L -f -# "$DOWNLOAD_URL" -o "$JAR_PATH"

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
echo "Manorrock Assistant has been installed to:"
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
