#!/bin/bash

# Manorrock Coding Assistant - CLI Wrapper
# A wrapper script that enhances the Manorrock Assistant CLI with specialized coding commands

# Version information
VERSION="25.4.2"
MIN_JAVA_VERSION="11"
MIN_CLI_VERSION="25.4.2"
WRAPPER_NAME="Manorrock Coding Assistant"

# Configuration
CONFIG_DIR="$HOME/.manorrock/assistant/config"
TEMPLATES_DIR="$HOME/.manorrock/assistant/templates"
CONTEXT_FILE="$HOME/.manorrock/assistant/context.json"
VERSION_FILE="$CONFIG_DIR/version_info.json"

# Version compatibility check functions
function check_java_version() {
    if ! command -v java &> /dev/null; then
        echo "Error: Java not found. Please install Java $MIN_JAVA_VERSION or later." >&2
        return 1
    fi
    
    # Extract Java version (works with both Java 8 and later versions)
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
    
    if [[ -z "$java_version" ]]; then
        echo "Warning: Could not determine Java version." >&2
        return 0  # Continue anyway
    fi
    
    if (( java_version < MIN_JAVA_VERSION )); then
        echo "Error: Java version $java_version is not supported." >&2
        echo "Please upgrade to Java $MIN_JAVA_VERSION or later." >&2
        return 1
    fi
    
    return 0
}

function check_cli_jar_version() {
    local jar_path=$1
    
    if [ ! -f "$jar_path" ]; then
        echo "Error: CLI JAR not found at $jar_path" >&2
        return 1
    fi
    
    # Extract version from JAR manifest
    local jar_version=""
    if command -v unzip &> /dev/null; then
        jar_version=$(unzip -p "$jar_path" META-INF/MANIFEST.MF 2>/dev/null | grep "Implementation-Version" | cut -d: -f2 | tr -d ' \r\n')
    fi
    
    # If we can't extract version from manifest, try running the JAR with --version
    if [ -z "$jar_version" ]; then
        if java -jar "$jar_path" --version &> /dev/null; then
            jar_version=$(java -jar "$jar_path" --version 2>/dev/null | grep -o '[0-9]\+\.[0-9]\+\.[0-9]\+' | head -1)
        fi
    fi
    
    # If we still can't get the version, treat it as an error
    if [ -z "$jar_version" ]; then
        echo "Error: Could not determine CLI JAR version. Unable to verify compatibility." >&2
        return 1
    fi
    
    # Parse versions into components
    local min_version_parts=(${MIN_CLI_VERSION//./ })
    local jar_version_parts=(${jar_version//./ })
    
    # Compare major version
    if [ ${jar_version_parts[0]} -lt ${min_version_parts[0]} ]; then
        echo "Error: CLI JAR version $jar_version is not compatible with this wrapper (version $VERSION)." >&2
        echo "Please update your CLI JAR to version $MIN_CLI_VERSION or later." >&2
        return 1
    elif [ ${jar_version_parts[0]} -gt ${min_version_parts[0]} ]; then
        # Major version is higher, should be compatible
        :
    else
        # Major versions match, check minor version
        if [ ${jar_version_parts[1]} -lt ${min_version_parts[1]} ]; then
            echo "Error: CLI JAR version $jar_version is not compatible with this wrapper (version $VERSION)." >&2
            echo "Please update your CLI JAR to version $MIN_CLI_VERSION or later." >&2
            return 1
        elif [ ${jar_version_parts[1]} -gt ${min_version_parts[1]} ]; then
            # Minor version is higher, should be compatible
            :
        else
            # Minor versions match, check patch version
            if [ ${jar_version_parts[2]} -lt ${min_version_parts[2]} ]; then
                echo "Error: CLI JAR version $jar_version is not compatible with this wrapper (version $VERSION)." >&2
                echo "Please update your CLI JAR to version $MIN_CLI_VERSION or later." >&2
                return 1
            fi
        fi
    fi
    
    # Store CLI version for future reference
    if [ ! -f "$VERSION_FILE" ]; then
        echo "{}" > "$VERSION_FILE"
    fi
    
    local temp_file=$(mktemp)
    if command -v jq &> /dev/null; then
        jq --arg path "$jar_path" --arg ver "$jar_version" \
           '.cli_versions[$path] = $ver' "$VERSION_FILE" > "$temp_file" && \
        mv "$temp_file" "$VERSION_FILE"
    fi
    
    echo "CLI version check passed: $jar_version is compatible with wrapper version $VERSION" >&2
    return 0
}

function check_dependencies() {
    local missing_deps=()
    
    # Check for jq (used for JSON manipulation)
    if ! command -v jq &> /dev/null; then
        missing_deps+=("jq")
    fi
    
    # Check for realpath (used in context management)
    if ! command -v realpath &> /dev/null; then
        missing_deps+=("realpath")
    fi
    
    if [ ${#missing_deps[@]} -gt 0 ]; then
        echo "Warning: The following dependencies are missing:" >&2
        for dep in "${missing_deps[@]}"; do
            echo "  - $dep" >&2
        done
        
        echo "" >&2
        echo "Some functionality may be limited. Install missing dependencies for full functionality." >&2
        
        case "$(uname -s)" in
            Linux*)
                echo "For Debian/Ubuntu: sudo apt-get install ${missing_deps[*]}" >&2
                echo "For Fedora: sudo dnf install ${missing_deps[*]}" >&2
                ;;
            Darwin*)
                echo "For macOS: brew install ${missing_deps[*]}" >&2
                ;;
            MINGW*|MSYS*|CYGWIN*)
                echo "For Windows: Install these tools via chocolatey, msys2, or manually" >&2
                ;;
        esac
        
        # Ask user if they want to continue
        read -p "Continue anyway? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            return 1
        fi
    fi
    
    return 0
}

function verify_compatibility() {
    # Check Java version
    if ! check_java_version; then
        return 1
    fi
    
    # Get CLI JAR path
    local jar_path=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    # Check CLI JAR version
    if ! check_cli_jar_version "$jar_path"; then
        return 1
    fi
    
    # Check for required dependencies
    if ! check_dependencies; then
        return 1
    fi
    
    return 0
}

# Find the CLI JAR, looking in common locations
function find_cli_jar() {
    local possible_locations=(
        "$HOME/.manorrock/assistant/cli.jar"
        "$HOME/.m2/repository/com/manorrock/assistant/cli/*/cli-*-jar-with-dependencies.jar"
        "$(pwd)/cli/target/cli-*-jar-with-dependencies.jar"
        "$(pwd)/assistant/cli/target/cli-*-jar-with-dependencies.jar"
        "/usr/local/lib/manorrock-assistant/cli-*-jar-with-dependencies.jar"
        "/opt/manorrock-assistant/cli-*-jar-with-dependencies.jar"
    )
    
    for pattern in "${possible_locations[@]}"; do
        local found_jars=( $pattern )
        if [ ${#found_jars[@]} -gt 0 ] && [ -f "${found_jars[0]}" ]; then
            echo "${found_jars[0]}"
            return 0
        fi
    done
    
    # If we couldn't find it automatically, ask the user
    echo "Could not locate the Manorrock Assistant CLI JAR file." >&2
    echo "Please provide the path to cli-*-jar-with-dependencies.jar:" >&2
    read -r jar_path
    
    if [ -f "$jar_path" ]; then
        # Save this path for future use
        mkdir -p "$CONFIG_DIR"
        echo "$jar_path" > "$CONFIG_DIR/cli_jar_path"
        echo "$jar_path"
        return 0
    else
        echo "Invalid path: $jar_path does not exist." >&2
        return 1
    fi
}

# Get CLI JAR path (from config or by searching)
function get_cli_jar() {
    if [ -f "$CONFIG_DIR/cli_jar_path" ]; then
        local jar_path=$(cat "$CONFIG_DIR/cli_jar_path")
        if [ -f "$jar_path" ]; then
            echo "$jar_path"
            return 0
        fi
    fi
    
    find_cli_jar
}

# Initialize configuration
function init_config() {
    mkdir -p "$CONFIG_DIR"
    mkdir -p "$TEMPLATES_DIR"
    
    # Create default templates if they don't exist
    if [ ! -f "$TEMPLATES_DIR/code-review.txt" ]; then
        echo "You are a senior developer conducting a code review. Focus on identifying potential bugs, performance issues, security vulnerabilities, and areas for improvement. Provide constructive feedback and suggest specific solutions." > "$TEMPLATES_DIR/code-review.txt"
    fi
    
    if [ ! -f "$TEMPLATES_DIR/debug.txt" ]; then
        echo "You are a debugging specialist. Analyze code errors systematically, identify root causes, and propose precise solutions. Focus on examining error messages, tracing code execution, and recognizing common bug patterns." > "$TEMPLATES_DIR/debug.txt"
    fi
    
    if [ ! -f "$TEMPLATES_DIR/refactor.txt" ]; then
        echo "You are a refactoring expert. Analyze code for readability, maintainability, and design improvements without changing functionality. Apply appropriate design patterns, reduce complexity, and eliminate code smells." > "$TEMPLATES_DIR/refactor.txt"
    fi
    
    if [ ! -f "$TEMPLATES_DIR/document.txt" ]; then
        echo "You are a technical documentation expert. Create clear, concise, and comprehensive documentation for code. Focus on explaining purpose, usage, parameters, return values, and include appropriate examples." > "$TEMPLATES_DIR/document.txt"
    fi
    
    if [ ! -f "$TEMPLATES_DIR/generate.txt" ]; then
        echo "You are an expert code generator. Write clean, efficient, and well-structured code based on requirements. Focus on following best practices, proper error handling, and optimizing for readability and maintainability." > "$TEMPLATES_DIR/generate.txt"
    fi
}

# Call the CLI with specific system message based on file type
function analyze_file() {
    local file_path=$1
    
    if [ ! -f "$file_path" ]; then
        echo "Error: File not found - $file_path"
        return 1
    fi
    
    local file_ext="${file_path##*.}"
    local file_name="${file_path##*/}"
    
    CLI_JAR=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    echo "Analyzing $file_name..."
    java -jar "$CLI_JAR" --no-banner "Explain this code in detail: $(cat $file_path)"
}

# Help command to display usage information
function help_command() {
    echo "Manorrock Coding Assistant - CLI Wrapper"
    echo ""
    echo "Usage: $(basename "$0") [command] [arguments]"
    echo ""
    echo "Commands:"
    echo "  analyze <file>             Analyze a file with language-specific understanding"
    echo "  debug <file>               Debug code with intelligent analysis"
    echo "  fix <file>                 Get repair suggestions for problematic code"
    echo "  generate <description>     Generate code based on a description"
    echo "  document <file>            Auto-document existing code"
    echo "  review <file>              Perform a code review"
    echo "  refactor <file>            Get refactoring suggestions"
    echo "  context add <file>         Add a file to the current context"
    echo "  context list               List files in the current context"
    echo "  context clear              Clear the current context"
    echo "  context use <prompt>       Use the current context with a prompt"
    echo "  version                    Display version information"
    echo "  help                       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $(basename "$0") analyze src/main/java/MyClass.java"
    echo "  $(basename "$0") debug buggy_code.js"
    echo "  $(basename "$0") generate \"Create a REST API endpoint for user registration\""
}

# Debug code with specialized system message
function debug_code() {
    local file_path=$1
    
    if [ ! -f "$file_path" ]; then
        echo "Error: File not found - $file_path"
        return 1
    fi
    
    CLI_JAR=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    echo "Debugging $file_path..."
    java -jar "$CLI_JAR" --no-banner "Debug this code and identify problems: $(cat $file_path)"
}

# Fix code with repair suggestions
function fix_code() {
    local file_path=$1
    
    if [ ! -f "$file_path" ]; then
        echo "Error: File not found - $file_path"
        return 1
    fi
    
    CLI_JAR=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    echo "Generating repair suggestions for $file_path..."
    java -jar "$CLI_JAR" --no-banner "Help me fix issues in this code: $(cat $file_path)"
}

# Generate code based on a description
function generate_code() {
    local description=$1
    
    if [ -z "$description" ]; then
        echo "Error: No description provided"
        echo "Usage: $(basename "$0") generate \"<code description>\""
        return 1
    fi
    
    CLI_JAR=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    echo "Generating code based on description..."
    java -jar "$CLI_JAR" --no-banner "Generate code for: $description"
}

# Auto-document code
function document_code() {
    local file_path=$1
    
    if [ ! -f "$file_path" ]; then
        echo "Error: File not found - $file_path"
        return 1
    fi
    
    CLI_JAR=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    echo "Generating documentation for $file_path..."
    java -jar "$CLI_JAR" --no-banner "Create comprehensive documentation for this code file. Include function/method purpose, parameters, return values, exceptions, and usage examples: $(cat $file_path)"
}

# Perform a code review
function review_code() {
    local file_path=$1
    
    if [ ! -f "$file_path" ]; then
        echo "Error: File not found - $file_path"
        return 1
    fi
    
    CLI_JAR=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    echo "Reviewing $file_path..."
    java -jar "$CLI_JAR" --no-banner "Perform a thorough code review of this file. Evaluate for bugs, edge cases, performance issues, security vulnerabilities, maintainability, and adherence to best practices. Provide specific, actionable feedback: $(cat $file_path)"
}

# Suggest code refactoring
function refactor_code() {
    local file_path=$1
    
    if [ ! -f "$file_path" ]; then
        echo "Error: File not found - $file_path"
        return 1
    fi
    
    CLI_JAR=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    echo "Generating refactoring suggestions for $file_path..."
    java -jar "$CLI_JAR" --no-banner "Analyze this code for improvements in readability, maintainability, and efficiency without changing functionality. Suggest specific refactorings with before/after code examples: $(cat $file_path)"
}

# Context management functions
function context_add() {
    local file_path=$1
    
    if [ ! -f "$file_path" ]; then
        echo "Error: File not found - $file_path"
        return 1
    fi
    
    # Initialize context file if it doesn't exist
    if [ ! -f "$CONTEXT_FILE" ]; then
        echo "[]" > "$CONTEXT_FILE"
    fi
    
    # Get absolute path
    local abs_path=$(realpath "$file_path")
    
    # Check if file is already in context
    if grep -q "\"$abs_path\"" "$CONTEXT_FILE"; then
        echo "File already in context: $file_path"
        return 0
    fi
    
    # Add file to context
    local temp_file=$(mktemp)
    jq ". += [\"$abs_path\"]" "$CONTEXT_FILE" > "$temp_file" && mv "$temp_file" "$CONTEXT_FILE"
    
    echo "Added to context: $file_path"
}

function context_list() {
    if [ ! -f "$CONTEXT_FILE" ]; then
        echo "Context is empty"
        return 0
    fi
    
    echo "Current context:"
    jq -r '.[]' "$CONTEXT_FILE" | nl
}

function context_clear() {
    echo "[]" > "$CONTEXT_FILE"
    echo "Context cleared"
}

function context_use() {
    local prompt=$1
    
    if [ -z "$prompt" ]; then
        echo "Error: No prompt provided"
        echo "Usage: $(basename "$0") context use \"<your prompt>\""
        return 1
    fi
    
    if [ ! -f "$CONTEXT_FILE" ] || [ "$(jq 'length' "$CONTEXT_FILE")" -eq 0 ]; then
        echo "Error: Context is empty. Add files with '$(basename "$0") context add <file>'"
        return 1
    fi
    
    CLI_JAR=$(get_cli_jar)
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    # Create a temporary file to concatenate all context files
    local temp_file=$(mktemp)
    
    echo "Using context with the following files:" > "$temp_file"
    
    # Add each file in the context to the temp file
    local count=0
    while read -r file_path; do
        if [ -f "$file_path" ]; then
            echo -e "\n===== FILE: $file_path =====\n" >> "$temp_file"
            cat "$file_path" >> "$temp_file"
            ((count++))
        else
            echo "Warning: Context file not found - $file_path" >&2
        fi
    done < <(jq -r '.[]' "$CONTEXT_FILE")
    
    echo -e "\n===== USER PROMPT =====\n" >> "$temp_file"
    echo "$prompt" >> "$temp_file"
    
    echo "Processing prompt with $count file(s) in context..."
    java -jar "$CLI_JAR" --no-banner /explain "$temp_file"
    
    # Clean up temp file
    rm "$temp_file"
}

# Process context-related commands
function handle_context() {
    local subcommand=$1
    shift  # Remove the subcommand from the arguments
    
    case "$subcommand" in
        add)
            context_add "$1"
            ;;
        list)
            context_list
            ;;
        clear)
            context_clear
            ;;
        use)
            context_use "$*"
            ;;
        *)
            echo "Unknown context subcommand: $subcommand"
            echo "Available subcommands: add, list, clear, use"
            return 1
            ;;
    esac
}

# Recursive directory analysis
function analyze_directory() {
    local dir_path=$1
    local file_ext=$2
    
    if [ ! -d "$dir_path" ]; then
        echo "Error: Directory not found - $dir_path"
        return 1
    fi
    
    echo "Analyzing directory: $dir_path"
    
    # Find files with the specified extension (or all files if no extension provided)
    if [ -z "$file_ext" ]; then
        find "$dir_path" -type f -not -path "*/\.*" -not -path "*/node_modules/*" -not -path "*/target/*" -not -path "*/build/*" | while read -r file; do
            echo "=== Analyzing: $file ==="
            analyze_file "$file"
            echo ""
        done
    else
        find "$dir_path" -type f -name "*.$file_ext" -not -path "*/\.*" -not -path "*/node_modules/*" -not -path "*/target/*" -not -path "*/build/*" | while read -r file; do
            echo "=== Analyzing: $file ==="
            analyze_file "$file"
            echo ""
        done
    fi
}

# Initialize configuration
init_config

# Check version compatibility before proceeding
if ! verify_compatibility; then
    echo "Error: Version compatibility check failed. Please resolve the issues before continuing." >&2
    exit 1
fi

# Main command processing
case "$1" in
    analyze)
        if [ -d "$2" ]; then
            analyze_directory "$2" "$3"
        else
            analyze_file "$2"
        fi
        ;;
    debug)
        debug_code "$2"
        ;;
    fix)
        fix_code "$2"
        ;;
    generate)
        shift
        generate_code "$*"
        ;;
    document)
        document_code "$2"
        ;;
    review)
        review_code "$2"
        ;;
    refactor)
        refactor_code "$2"
        ;;
    context)
        shift
        handle_context "$@"
        ;;
    version)
        echo "$WRAPPER_NAME version $VERSION"
        CLI_JAR=$(get_cli_jar)
        if [ $? -eq 0 ]; then
            echo -n "CLI JAR: "
            java -jar "$CLI_JAR" --version
        fi
        ;;
    help|--help|-h)
        help_command
        ;;
    *)
        if [ -z "$1" ]; then
            help_command
        else
            echo "Unknown command: $1"
            echo "Run '$(basename "$0") help' for usage information"
            exit 1
        fi
        ;;
esac