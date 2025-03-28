package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for analyzing project structure to identify source files, build files, and other components.
 */
public class ProjectStructureAnalysisTool extends AbstractTool {
    
    private static final String NAME = "project_structure_analysis";
    private static final String DESCRIPTION = "Analyzes a project directory to identify source files, build files, and project structure";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
            new ToolParameter("projectPath", "string", "Path to the root directory of the project", true),
            new ToolParameter("maxDepth", "number", "Maximum depth to search for files (default: 10)", false)
    );
    
    // Known file types and their significance
    private static final Map<String, String> BUILD_FILES = new HashMap<>();
    static {
        BUILD_FILES.put("pom.xml", "Maven");
        BUILD_FILES.put("build.gradle", "Gradle");
        BUILD_FILES.put("build.gradle.kts", "Gradle Kotlin DSL");
        BUILD_FILES.put("package.json", "Node.js");
        BUILD_FILES.put("Makefile", "Make");
        BUILD_FILES.put("CMakeLists.txt", "CMake");
        BUILD_FILES.put("Cargo.toml", "Rust");
    }
    
    private static final Map<String, String> CONFIG_FILES = new HashMap<>();
    static {
        CONFIG_FILES.put(".gitignore", "Git ignore rules");
        CONFIG_FILES.put(".git", "Git repository");
        CONFIG_FILES.put(".github", "GitHub configuration");
        CONFIG_FILES.put("Dockerfile", "Docker image definition");
        CONFIG_FILES.put("docker-compose.yml", "Docker Compose configuration");
        CONFIG_FILES.put(".eslintrc", "ESLint configuration");
        CONFIG_FILES.put("tsconfig.json", "TypeScript configuration");
        CONFIG_FILES.put(".editorconfig", "Editor configuration");
        CONFIG_FILES.put(".travis.yml", "Travis CI configuration");
        CONFIG_FILES.put("Jenkinsfile", "Jenkins pipeline");
        CONFIG_FILES.put(".gitlab-ci.yml", "GitLab CI configuration");
    }
    
    /**
     * Creates a new ProjectStructureAnalysisTool.
     */
    public ProjectStructureAnalysisTool() {
        super(NAME, DESCRIPTION, PARAMETERS);
    }
    
    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters) throws Exception {
        String projectPath = parameters.get("projectPath").toString();
        int maxDepth = 10;
        
        if (parameters.containsKey("maxDepth")) {
            Object maxDepthObj = parameters.get("maxDepth");
            if (maxDepthObj instanceof Number) {
                maxDepth = ((Number) maxDepthObj).intValue();
            } else {
                maxDepth = Integer.parseInt(maxDepthObj.toString());
            }
        }
        
        Path rootPath = Paths.get(projectPath);
        if (!Files.exists(rootPath)) {
            return ToolResult.failure("Project path does not exist: " + projectPath);
        }
        
        if (!Files.isDirectory(rootPath)) {
            return ToolResult.failure("Path is not a directory: " + projectPath);
        }
        
        Map<String, Object> analysisResult = analyzeProject(rootPath, maxDepth);
        return ToolResult.success(analysisResult, "Project structure analysis completed successfully");
    }
    
    private Map<String, Object> analyzeProject(Path rootPath, int maxDepth) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("projectRoot", rootPath.toAbsolutePath().toString());
        
        // Find build files
        List<Map<String, String>> buildFiles = new ArrayList<>();
        // Find configuration files
        List<Map<String, String>> configFiles = new ArrayList<>();
        // Count source files by language
        Map<String, Integer> sourceFiles = new HashMap<>();
        // Find directories with source files
        Map<String, List<String>> sourceDirectories = new HashMap<>();
        
        // Walk the directory tree to find relevant files
        Files.walk(rootPath, maxDepth)
            .filter(Files::isRegularFile)
            .forEach(path -> {
                String fileName = path.getFileName().toString();
                String relativePath = rootPath.relativize(path).toString();
                
                // Check if it's a build file
                if (BUILD_FILES.containsKey(fileName)) {
                    Map<String, String> buildFile = new HashMap<>();
                    buildFile.put("name", fileName);
                    buildFile.put("path", relativePath);
                    buildFile.put("type", BUILD_FILES.get(fileName));
                    buildFiles.add(buildFile);
                }
                
                // Check if it's a config file
                if (CONFIG_FILES.containsKey(fileName)) {
                    Map<String, String> configFile = new HashMap<>();
                    configFile.put("name", fileName);
                    configFile.put("path", relativePath);
                    configFile.put("type", CONFIG_FILES.get(fileName));
                    configFiles.add(configFile);
                }
                
                // Identify source file language
                String language = identifyLanguage(fileName);
                if (language != null) {
                    sourceFiles.put(language, sourceFiles.getOrDefault(language, 0) + 1);
                    
                    // Track source directories
                    Path parent = path.getParent();
                    String parentRelative = rootPath.relativize(parent).toString();
                    sourceDirectories.computeIfAbsent(language, k -> new ArrayList<>());
                    if (!sourceDirectories.get(language).contains(parentRelative)) {
                        sourceDirectories.get(language).add(parentRelative);
                    }
                }
            });
        
        // Find directories
        List<String> directories = Files.walk(rootPath, maxDepth)
            .filter(Files::isDirectory)
            .map(path -> rootPath.relativize(path).toString())
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        
        // Identify project type based on build files and structure
        String projectType = identifyProjectType(buildFiles, directories);
        
        result.put("projectType", projectType);
        result.put("buildFiles", buildFiles);
        result.put("configFiles", configFiles);
        result.put("sourceFiles", sourceFiles);
        result.put("sourceDirectories", sourceDirectories);
        result.put("directories", directories);
        
        // Analyze dependencies if possible
        if (!buildFiles.isEmpty()) {
            try {
                Map<String, Object> dependencies = analyzeDependencies(rootPath, buildFiles);
                result.put("dependencies", dependencies);
            } catch (Exception e) {
                // Dependencies analysis is optional, so just record the error
                Map<String, Object> dependencies = new HashMap<>();
                dependencies.put("error", "Failed to analyze dependencies: " + e.getMessage());
                result.put("dependencies", dependencies);
            }
        }
        
        return result;
    }
    
    private String identifyLanguage(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.endsWith(".java")) {
            return "Java";
        } else if (lowerFileName.endsWith(".kt") || lowerFileName.endsWith(".kts")) {
            return "Kotlin";
        } else if (lowerFileName.endsWith(".groovy")) {
            return "Groovy";
        } else if (lowerFileName.endsWith(".scala")) {
            return "Scala";
        } else if (lowerFileName.endsWith(".js")) {
            return "JavaScript";
        } else if (lowerFileName.endsWith(".ts")) {
            return "TypeScript";
        } else if (lowerFileName.endsWith(".jsx")) {
            return "React JSX";
        } else if (lowerFileName.endsWith(".tsx")) {
            return "React TSX";
        } else if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm")) {
            return "HTML";
        } else if (lowerFileName.endsWith(".css")) {
            return "CSS";
        } else if (lowerFileName.endsWith(".scss") || lowerFileName.endsWith(".sass")) {
            return "SASS";
        } else if (lowerFileName.endsWith(".less")) {
            return "LESS";
        } else if (lowerFileName.endsWith(".py")) {
            return "Python";
        } else if (lowerFileName.endsWith(".rb")) {
            return "Ruby";
        } else if (lowerFileName.endsWith(".php")) {
            return "PHP";
        } else if (lowerFileName.endsWith(".go")) {
            return "Go";
        } else if (lowerFileName.endsWith(".rs")) {
            return "Rust";
        } else if (lowerFileName.endsWith(".c") || lowerFileName.endsWith(".h")) {
            return "C";
        } else if (lowerFileName.endsWith(".cpp") || lowerFileName.endsWith(".hpp") || 
                   lowerFileName.endsWith(".cc") || lowerFileName.endsWith(".hh")) {
            return "C++";
        } else if (lowerFileName.endsWith(".cs")) {
            return "C#";
        } else if (lowerFileName.endsWith(".swift")) {
            return "Swift";
        } else if (lowerFileName.endsWith(".m") || lowerFileName.endsWith(".mm")) {
            return "Objective-C";
        } else if (lowerFileName.endsWith(".json")) {
            return "JSON";
        } else if (lowerFileName.endsWith(".xml")) {
            return "XML";
        } else if (lowerFileName.endsWith(".yml") || lowerFileName.endsWith(".yaml")) {
            return "YAML";
        }
        
        return null;
    }
    
    private String identifyProjectType(List<Map<String, String>> buildFiles, List<String> directories) {
        // Check for build files first
        for (Map<String, String> buildFile : buildFiles) {
            String name = buildFile.get("name");
            if ("pom.xml".equals(name)) {
                return "Maven Project";
            } else if ("build.gradle".equals(name) || "build.gradle.kts".equals(name)) {
                return "Gradle Project";
            } else if ("package.json".equals(name)) {
                return "Node.js Project";
            } else if ("Cargo.toml".equals(name)) {
                return "Rust Project";
            } else if ("Makefile".equals(name)) {
                return "Make Project";
            }
        }
        
        // Check directories if build files didn't give us a clear answer
        if (directories.contains("src/main/java")) {
            return "Java Project";
        } else if (directories.contains("src/main/kotlin")) {
            return "Kotlin Project";
        } else if (directories.contains("src/main/scala")) {
            return "Scala Project";
        } else if (directories.contains("src") && directories.contains("lib")) {
            return "Generic Project";
        } else if (directories.contains("app") && directories.contains("config")) {
            return "Rails Project";
        } else if (directories.contains("src") && directories.contains("public")) {
            return "Web Project";
        }
        
        return "Unknown Project Type";
    }
    
    private Map<String, Object> analyzeDependencies(Path rootPath, List<Map<String, String>> buildFiles) {
        Map<String, Object> dependencies = new HashMap<>();
        
        // This is a simplified implementation
        // In a real implementation, you'd parse the build files to extract dependencies
        // Here we just identify the dependency files
        
        for (Map<String, String> buildFile : buildFiles) {
            String type = buildFile.get("type");
            String path = buildFile.get("path");
            
            dependencies.put("buildSystem", type);
            dependencies.put("definitionFile", path);
            
            // Check for common dependency-related files
            if ("Maven".equals(type)) {
                Path dependencyTree = rootPath.resolve("target/dependency-tree.txt");
                if (Files.exists(dependencyTree)) {
                    dependencies.put("dependencyTreeAvailable", true);
                }
            } else if ("Gradle".equals(type)) {
                Path dependenciesDir = rootPath.resolve("build/reports/dependencies");
                if (Files.exists(dependenciesDir)) {
                    dependencies.put("dependencyReportsAvailable", true);
                }
            } else if ("Node.js".equals(type)) {
                Path packageLockJson = rootPath.resolve("package-lock.json");
                Path yarnLock = rootPath.resolve("yarn.lock");
                
                if (Files.exists(packageLockJson)) {
                    dependencies.put("lockFile", "package-lock.json");
                } else if (Files.exists(yarnLock)) {
                    dependencies.put("lockFile", "yarn.lock");
                }
            }
        }
        
        return dependencies;
    }
}
