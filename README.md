# Manorrock Assistant

Note this project was almost 100% developed using AI as proof of concept to determine how far AI assistated development has come.

If you want to try it out, you can do so, but you will have to build it yourself.

Contributions are welcome, but be aware this is neither funded nor supported by any organization.

## Overview

Manorrock Assistant exposes Large Language Models in a chat like interface in a variety of different ways, e.g. as a console application, a desktop application, or as an IDE extension or plugin. More implementations are under consideration or under development.

## Command Description

| Command | Description |
|---------|-------------|
| `/endpoint <host:port>` | Changes the Ollama API endpoint (e.g., `/endpoint localhost:11434`) |
| `/model <name>` | Changes the LLM model used (e.g., `/model llama3`) |
| `/help` | Displays available commands |
| `/clear` | Clears the response window |
| `/explain` | Explains selected code or text (IDE plugins only) |
| `/startover` | Clears conversation history and starts a new session |

## Command Support Matrix

This matrix shows which commands are supported by each implementation of the Manorrock Assistant.

| Command | CLI | Desktop | Eclipse | NetBeans | IntelliJ | VSCode | Mobile |
|---------|-----|---------|---------|----------|----------|--------|--------|
| `/endpoint <host:port>` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅* | ✅ |
| `/model <name>` | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| `/help` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/clear` | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| `/explain` | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/startover` or equivalent | ❌ | ✅** | ✅** | ✅** | ✅** | ❌ | ❌ |

**Notes:**
- ✅ Fully supported
- ❌ Not supported
- ✅* VSCode extension handles endpoint through Configuration settings rather than command
- ✅** Desktop, Eclipse, NetBeans, and IntelliJ implementations have a "Start Over" button rather than a command

## Implementations

1. **Java based CLI**
   - A command line interface (CLI) version of the application.
   - **Uber JAR**
     - A standalone JAR file that can be executed from the command line.
   - **GraalVM native executable**
     - A native executable generated using GraalVM for improved performance and reduced startup time.

2. **JavaFX based desktop application**
   - A desktop application built with JavaFX.
   - **DMG installer (macOS)**
     - A macOS installer package for easy installation on macOS systems.
   - **MSI installer (Windows)**
     - A Windows installer package for easy installation on Windows systems.

3. **NetBeans plugin**
   - A plugin for the NetBeans IDE to integrate Manorrock Assistant functionalities.

4. **VSCode extension**
   - An extension for Visual Studio Code to integrate Manorrock Assistant functionalities.

5. **IntelliJ plugin**
   - A plugin for the IntelliJ IDEA IDE to integrate Manorrock Assistant functionalities.

6. **Eclipse plugin**
   - A plugin for the Eclipse IDE to integrate Manorrock Assistant functionalities.


## Under Consideration or Under Development

These implementations are either under consideration or currently in development. If they are under development, be aware they are not ready for use.

3. **Mobile application**
   - A mobile application version as the basis for the iPhone and Android versions.

4. **iPhone applicatio**
   - A mobile application version for iPhone devices.

5. **Spring Boot REST application**
   - A Spring Boot based RESTful web service version.

6. **Android application**
   - A mobile application version for Android devices.

7. **Quarkus application**
   - A Quarkus based version for improved performance and reduced memory footprint.

8. **Slack bot**
   - A Slack bot version for integration with Slack.

9. **Microsoft Teams bot**
   - A Microsoft Teams bot version for integration with Microsoft Teams.

10. **Discord bot**
    - A Discord bot version for integration with Discord.
