package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.ToolExecutionException;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Tool for analyzing and managing project dependencies across different build systems.
 * Supports Maven, Gradle, and Node.js projects.
 */
public class DependencyManagementTool extends AbstractTool {
    
    private static final String NAME = "dependency_management";
    private static final String DESCRIPTION = "Analyzes project dependencies in various build systems (Maven, Gradle, npm)";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
            new ToolParameter("path", "string", "Path to the project file (pom.xml, build.gradle, package.json, etc.) or project directory", true),
            new ToolParameter("format", "string", "Optional format override (maven, gradle, npm). If not provided, will be detected from file extension", false)
    );
    
    // Patterns for Gradle dependency extraction
    private static final Pattern GRADLE_DEPENDENCY_PATTERN = 
            Pattern.compile("(implementation|api|compile|testImplementation|testCompile|runtimeOnly|annotationProcessor)\\s*['\"]([^'\"]+:[^'\"]+:[^'\"]+)['\"]");
    
    /**
     * Creates a new DependencyManagementTool.
     */
    public DependencyManagementTool() {
        super(NAME, DESCRIPTION, PARAMETERS);
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) throws ToolExecutionException {
        String pathStr = parameters.get("path").toString();
        String format = parameters.containsKey("format") ? parameters.get("format").toString().toLowerCase() : null;
        
        try {
            Path path = Paths.get(pathStr);
            
            if (!Files.exists(path)) {
                return ToolResult.failure("Path does not exist: " + pathStr);
            }
            
            // If path is a directory, try to find standard dependency files
            if (Files.isDirectory(path)) {
                path = findDependencyFile(path, format);
                if (path == null) {
                    return ToolResult.failure("Could not find a dependency file in the specified directory");
                }
            }
            
            // If format is not specified, detect from file name
            if (format == null) {
                format = detectFormatFromFileName(path.getFileName().toString());
                if (format == null) {
                    return ToolResult.failure("Could not detect dependency format from file: " + path.getFileName());
                }
            }
            
            // Parse dependencies based on the format
            Map<String, Object> result = new HashMap<>();
            List<Map<String, String>> dependencies = new ArrayList<>();
            
            switch (format) {
                case "maven":
                    dependencies = parseMavenDependencies(path);
                    break;
                case "gradle":
                    dependencies = parseGradleDependencies(path);
                    break;
                case "npm":
                    dependencies = parseNpmDependencies(path);
                    break;
                default:
                    return ToolResult.failure("Unsupported dependency format: " + format);
            }
            
            result.put("format", format);
            result.put("file", path.toString());
            result.put("dependencies", dependencies);
            
            // Add additional dependency metadata
            result.put("count", dependencies.size());
            
            // Group dependencies by their group/organization
            Map<String, List<Map<String, String>>> groupedDeps = new HashMap<>();
            for (Map<String, String> dep : dependencies) {
                String group = dep.getOrDefault("group", "unknown");
                groupedDeps.computeIfAbsent(group, k -> new ArrayList<>()).add(dep);
            }
            result.put("groupedDependencies", groupedDeps);
            
            return ToolResult.success(result);
            
        } catch (IOException e) {
            throw new ToolExecutionException("Failed to read dependency file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ToolExecutionException("Failed to analyze dependencies: " + e.getMessage(), e);
        }
    }
    
    /**
     * Find a standard dependency file in the given directory.
     * 
     * @param directory The directory to search in
     * @param format Optional format hint
     * @return Path to the dependency file, or null if not found
     */
    private Path findDependencyFile(Path directory, String format) {
        // Try to find standard dependency files based on format or in order of precedence
        if (format == null || "maven".equals(format)) {
            Path pomFile = directory.resolve("pom.xml");
            if (Files.exists(pomFile)) {
                return pomFile;
            }
        }
        
        if (format == null || "gradle".equals(format)) {
            Path gradleFile = directory.resolve("build.gradle");
            if (Files.exists(gradleFile)) {
                return gradleFile;
            }
            
            Path gradleKtsFile = directory.resolve("build.gradle.kts");
            if (Files.exists(gradleKtsFile)) {
                return gradleKtsFile;
            }
        }
        
        if (format == null || "npm".equals(format)) {
            Path packageJsonFile = directory.resolve("package.json");
            if (Files.exists(packageJsonFile)) {
                return packageJsonFile;
            }
        }
        
        return null;
    }
    
    /**
     * Detect the dependency format from a file name.
     * 
     * @param fileName The file name
     * @return The detected format, or null if unknown
     */
    private String detectFormatFromFileName(String fileName) {
        fileName = fileName.toLowerCase();
        
        if (fileName.equals("pom.xml")) {
            return "maven";
        } else if (fileName.startsWith("build.gradle")) {
            return "gradle";
        } else if (fileName.equals("package.json")) {
            return "npm";
        }
        
        return null;
    }
    
    /**
     * Parse Maven dependencies from a pom.xml file.
     * 
     * @param pomFile Path to the pom.xml file
     * @return List of dependency information
     * @throws Exception if parsing fails
     */
    private List<Map<String, String>> parseMavenDependencies(Path pomFile) throws ParserConfigurationException, SAXException, IOException {
        List<Map<String, String>> dependencies = new ArrayList<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(pomFile.toFile());
        
        // Extract dependencies
        NodeList dependencyNodes = document.getElementsByTagName("dependency");
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Element depElement = (Element) dependencyNodes.item(i);
            
            Map<String, String> dependency = new HashMap<>();
            
            // Extract basic dependency information
            String groupId = getElementTextContent(depElement, "groupId");
            String artifactId = getElementTextContent(depElement, "artifactId");
            String version = getElementTextContent(depElement, "version");
            String scope = getElementTextContent(depElement, "scope");
            
            dependency.put("group", groupId);
            dependency.put("name", artifactId);
            dependency.put("version", version);
            if (scope != null && !scope.isEmpty()) {
                dependency.put("scope", scope);
            } else {
                dependency.put("scope", "compile");
            }
            dependency.put("type", "maven");
            
            dependencies.add(dependency);
        }
        
        return dependencies;
    }
    
    /**
     * Helper method to get text content of an XML element.
     */
    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }
    
    /**
     * Parse Gradle dependencies from a build.gradle file.
     * 
     * @param gradleFile Path to the build.gradle file
     * @return List of dependency information
     * @throws IOException if reading fails
     */
    private List<Map<String, String>> parseGradleDependencies(Path gradleFile) throws IOException {
        List<Map<String, String>> dependencies = new ArrayList<>();
        
        String content = Files.readString(gradleFile, StandardCharsets.UTF_8);
        Matcher matcher = GRADLE_DEPENDENCY_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String configurationName = matcher.group(1);
            String coordinates = matcher.group(2);
            
            // Parse the GAV coordinates (group:artifact:version)
            String[] parts = coordinates.split(":");
            if (parts.length >= 2) {
                Map<String, String> dependency = new HashMap<>();
                dependency.put("group", parts[0]);
                dependency.put("name", parts[1]);
                
                if (parts.length >= 3) {
                    dependency.put("version", parts[2]);
                } else {
                    dependency.put("version", "unknown");
                }
                
                dependency.put("scope", configurationName);
                dependency.put("type", "gradle");
                
                dependencies.add(dependency);
            }
        }
        
        return dependencies;
    }
    
    /**
     * Parse npm dependencies from a package.json file.
     * 
     * @param packageJsonFile Path to the package.json file
     * @return List of dependency information
     * @throws IOException if reading fails
     */
    private List<Map<String, String>> parseNpmDependencies(Path packageJsonFile) throws IOException {
        List<Map<String, String>> dependencies = new ArrayList<>();
        
        String content = Files.readString(packageJsonFile, StandardCharsets.UTF_8);
        
        // Simple JSON parsing - in a real implementation, use a proper JSON parser
        // Extract dependencies and devDependencies sections
        
        Map<String, String> depsMap = extractNpmDependencies(content, "dependencies");
        for (Map.Entry<String, String> entry : depsMap.entrySet()) {
            Map<String, String> dependency = new HashMap<>();
            dependency.put("name", entry.getKey());
            dependency.put("version", entry.getValue());
            dependency.put("scope", "runtime");
            dependency.put("type", "npm");
            dependencies.add(dependency);
        }
        
        Map<String, String> devDepsMap = extractNpmDependencies(content, "devDependencies");
        for (Map.Entry<String, String> entry : devDepsMap.entrySet()) {
            Map<String, String> dependency = new HashMap<>();
            dependency.put("name", entry.getKey());
            dependency.put("version", entry.getValue());
            dependency.put("scope", "development");
            dependency.put("type", "npm");
            dependencies.add(dependency);
        }
        
        return dependencies;
    }
    
    /**
     * Extract npm dependencies from a section of package.json.
     * 
     * @param content The package.json content
     * @param sectionName The section name (dependencies or devDependencies)
     * @return Map of dependency name to version
     */
    private Map<String, String> extractNpmDependencies(String content, String sectionName) {
        Map<String, String> dependencies = new HashMap<>();
        
        // Find the section (e.g., "dependencies": { ... })
        Pattern sectionPattern = Pattern.compile("\"" + sectionName + "\"\\s*:\\s*\\{([^\\}]+)\\}");
        Matcher sectionMatcher = sectionPattern.matcher(content);
        
        if (sectionMatcher.find()) {
            String sectionContent = sectionMatcher.group(1);
            
            // Extract name-version pairs
            Pattern depPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
            Matcher depMatcher = depPattern.matcher(sectionContent);
            
            while (depMatcher.find()) {
                String name = depMatcher.group(1);
                String version = depMatcher.group(2);
                dependencies.put(name, version);
            }
        }
        
        return dependencies;
    }
}
