package com.manorrock.assistant.shared.tools;

import com.manorrock.assistant.shared.ToolExecutionException;
import com.manorrock.assistant.shared.ToolParameter;
import com.manorrock.assistant.shared.ToolResult;

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
    public ToolResult execute(Map<String, Object> parameters) throws ToolExecutionException {
        String projectPath = parameters.get("projectPath").toString();
        int maxDepth = 10;
        
        if (parameters.containsKey("maxDepth")) {
            Object depthParam = parameters.get("maxDepth");
            if (depthParam instanceof Number) {
                maxDepth = ((Number) depthParam).intValue();
            } else {
                try {
                    maxDepth = Integer.parseInt(depthParam.toString());
                } catch (NumberFormatException e) {
                    return ToolResult.failure("Invalid maxDepth parameter: " + depthParam);
                }
            }
        }
        
        try {
            Path rootPath = Paths.get(projectPath);
            
            if (!Files.exists(rootPath)) {
                return ToolResult.failure("Project directory does not exist: " + projectPath);
            }
            
            if (!Files.isDirectory(rootPath)) {
                return ToolResult.failure("Path is not a directory: " + projectPath);
            }
            
            Map<String, Object> result = analyzeProject(rootPath, maxDepth);
            return ToolResult.success(result);
            
        } catch (IOException e) {
            throw new ToolExecutionException("Failed to analyze project: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ToolExecutionException("Unexpected error during project analysis: " + e.getMessage(), e);
        }
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
                    buildFile.put("path", relativePath);
                    buildFile.put("type", BUILD_FILES.get(fileName));
                    buildFiles.add(buildFile);
                }
                
                // Check if it's a config file
                for (Map.Entry<String, String> entry : CONFIG_FILES.entrySet()) {
                    if (fileName.equals(entry.getKey()) || fileName.endsWith(entry.getKey())) {
                        Map<String, String> configFile = new HashMap<>();
                        configFile.put("path", relativePath);
                        configFile.put("type", entry.getValue());
                        configFiles.add(configFile);
                        break;
                    }
                }
                
                // Identify source file language
                String language = identifyLanguage(fileName);
                if (language != null) {
                    sourceFiles.put(language, sourceFiles.getOrDefault(language, 0) + 1);
                    
                    // Add to source directories
                    Path parentDir = path.getParent().relativize(rootPath);
                    String dirPath = parentDir.toString();
                    if (dirPath.isEmpty()) {
                        dirPath = ".";
                    }
                    
                    List<String> dirs = sourceDirectories.getOrDefault(language, new ArrayList<>());
                    if (!dirs.contains(dirPath)) {
                        dirs.add(dirPath);
                        sourceDirectories.put(language, dirs);
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
            Map<String, Object> dependencies = analyzeDependencies(rootPath, buildFiles);
            if (!dependencies.isEmpty()) {
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
            return "React";
        } else if (lowerFileName.endsWith(".tsx")) {
            return "React TypeScript";
        } else if (lowerFileName.endsWith(".py")) {
            return "Python";
        } else if (lowerFileName.endsWith(".rb")) {
            return "Ruby";
        } else if (lowerFileName.endsWith(".c") || lowerFileName.endsWith(".h")) {
            return "C";
        } else if (lowerFileName.endsWith(".cpp") || lowerFileName.endsWith(".hpp") || 
                  lowerFileName.endsWith(".cc") || lowerFileName.endsWith(".cxx")) {
            return "C++";
        } else if (lowerFileName.endsWith(".cs")) {
            return "C#";
        } else if (lowerFileName.endsWith(".go")) {
            return "Go";
        } else if (lowerFileName.endsWith(".rs")) {
            return "Rust";
        } else if (lowerFileName.endsWith(".swift")) {
            return "Swift";
        } else if (lowerFileName.endsWith(".php")) {
            return "PHP";
        } else if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm")) {
            return "HTML";
        } else if (lowerFileName.endsWith(".css")) {
            return "CSS";
        } else if (lowerFileName.endsWith(".scss")) {
            return "SCSS";
        } else if (lowerFileName.endsWith(".less")) {
            return "Less";
        } else if (lowerFileName.endsWith(".json")) {
            return "JSON";
        } else if (lowerFileName.endsWith(".xml")) {
            return "XML";
        } else if (lowerFileName.endsWith(".yaml") || lowerFileName.endsWith(".yml")) {
            return "YAML";
        } else if (lowerFileName.endsWith(".md") || lowerFileName.endsWith(".markdown")) {
            return "Markdown";
        } else if (lowerFileName.endsWith(".sh")) {
            return "Shell";
        } else if (lowerFileName.endsWith(".bat") || lowerFileName.endsWith(".cmd")) {
            return "Batch";
        } else if (lowerFileName.endsWith(".ps1")) {
            return "PowerShell";
        } else if (lowerFileName.endsWith(".sql")) {
            return "SQL";
        }
        
        return null;
    }
    
    private String identifyProjectType(List<Map<String, String>> buildFiles, List<String> directories) {
        // Check for build files first
        for (Map<String, String> buildFile : buildFiles) {
            String type = buildFile.get("type");
            if ("Maven".equals(type)) {
                return "Maven Java Project";
            } else if (type.startsWith("Gradle")) {
                return "Gradle Java Project";
            } else if ("Node.js".equals(type)) {
                return "Node.js Project";
            } else if ("Rust".equals(type)) {
                return "Rust Project";
            }
        }
        
        // Check directories if build files didn't give us a clear answer
        if (directories.contains("src/main/java")) {
            return "Java Project";
        } else if (directories.contains("app/src/main/java")) {
            return "Android Project";
        } else if (directories.contains("src/test/java")) {
            return "Java Project";
        } else if (directories.contains("node_modules")) {
            return "Node.js Project";
        } else if (directories.contains("target")) {
            return "Maven Project";
        } else if (directories.contains("build")) {
            return "Build-tool Project";
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
            
            if ("Maven".equals(type)) {
                dependencies.put("type", "Maven");
                dependencies.put("file", path);
            } else if (type.startsWith("Gradle")) {
                dependencies.put("type", "Gradle");
                dependencies.put("file", path);
            } else if ("Node.js".equals(type)) {
                Path packageLockPath = rootPath.resolve("package-lock.json");
                if (Files.exists(packageLockPath)) {
                    dependencies.put("type", "NPM");
                    dependencies.put("file", "package.json");
                    dependencies.put("lockFile", "package-lock.json");
                } else {
                    Path yarnLockPath = rootPath.resolve("yarn.lock");
                    if (Files.exists(yarnLockPath)) {
                        dependencies.put("type", "Yarn");
                        dependencies.put("file", "package.json");
                        dependencies.put("lockFile", "yarn.lock");
                    } else {
                        dependencies.put("type", "Node.js");
                        dependencies.put("file", "package.json");
                    }
                }
            }
        }
        
        return dependencies;
    }
}
