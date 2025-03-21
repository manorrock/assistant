package com.manorrock.assistant.shared.tools;

import com.manorrock.assistant.shared.ToolExecutionException;
import com.manorrock.assistant.shared.ToolParameter;
import com.manorrock.assistant.shared.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool for analyzing Java source files to extract structural information.
 */
public class JavaFileAnalysisTool extends AbstractTool {
    
    private static final String NAME = "java_file_analysis";
    private static final String DESCRIPTION = "Analyzes a Java source file to extract class, method, and field information";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
            new ToolParameter("path", "string", "Path to the Java file to analyze", true)
    );
    
    // Patterns for matching Java constructs
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(public|private|protected)?\\s*(abstract|final)?\\s*class\\s+([a-zA-Z0-9_]+)(?:\\s+extends\\s+([a-zA-Z0-9_<>.]+))?(?:\\s+implements\\s+([a-zA-Z0-9_<>.,\\s]+))?");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("(public|private|protected)?\\s*interface\\s+([a-zA-Z0-9_]+)(?:\\s+extends\\s+([a-zA-Z0-9_<>.,\\s]+))?");
    private static final Pattern ENUM_PATTERN = Pattern.compile("(public|private|protected)?\\s*enum\\s+([a-zA-Z0-9_]+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(public|private|protected)?\\s*(static|abstract|final)?\\s*(?:<[^>]+>)?\\s*([a-zA-Z0-9_<>\\[\\],.\\s]+)\\s+([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("(public|private|protected)?\\s*(static|final)?\\s*([a-zA-Z0-9_<>\\[\\],.\\s]+)\\s+([a-zA-Z0-9_]+)\\s*(?:=\\s*[^;]+)?;");
    
    /**
     * Creates a new JavaFileAnalysisTool.
     */
    public JavaFileAnalysisTool() {
        super(NAME, DESCRIPTION, PARAMETERS);
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) throws ToolExecutionException {
        String filePath = parameters.get("path").toString();
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return ToolResult.failure("File does not exist: " + filePath);
            }
            
            if (!Files.isRegularFile(path)) {
                return ToolResult.failure("Path is not a regular file: " + filePath);
            }
            
            if (!Files.isReadable(path)) {
                return ToolResult.failure("File is not readable: " + filePath);
            }
            
            if (!filePath.toLowerCase().endsWith(".java")) {
                return ToolResult.failure("File is not a Java source file: " + filePath);
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return ToolResult.success(analyzeJavaFile(content));
            
        } catch (IOException e) {
            throw new ToolExecutionException("Failed to read file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ToolExecutionException("Failed to analyze Java file: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> analyzeJavaFile(String content) {
        Map<String, Object> result = new HashMap<>();
        
        // Remove comments to avoid false matches
        content = removeComments(content);
        
        // Extract package
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
        if (packageMatcher.find()) {
            result.put("package", packageMatcher.group(1));
        }
        
        // Extract imports
        List<String> imports = new ArrayList<>();
        Matcher importMatcher = IMPORT_PATTERN.matcher(content);
        while (importMatcher.find()) {
            imports.add(importMatcher.group(1));
        }
        result.put("imports", imports);
        
        // Extract classes
        List<Map<String, Object>> classes = new ArrayList<>();
        Matcher classMatcher = CLASS_PATTERN.matcher(content);
        while (classMatcher.find()) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("type", "class");
            classInfo.put("visibility", classMatcher.group(1) != null ? classMatcher.group(1) : "default");
            classInfo.put("modifier", classMatcher.group(2) != null ? classMatcher.group(2) : "");
            classInfo.put("name", classMatcher.group(3));
            
            if (classMatcher.group(4) != null) {
                classInfo.put("extends", classMatcher.group(4).trim());
            }
            
            if (classMatcher.group(5) != null) {
                String implementsStr = classMatcher.group(5).trim();
                String[] implementsList = implementsStr.split("\\s*,\\s*");
                classInfo.put("implements", Arrays.asList(implementsList));
            }
            
            // Extract methods and fields for this class
            int classStart = classMatcher.start();
            int classEnd = findMatchingBrace(content, classMatcher.end());
            if (classEnd > classStart) {
                String classContent = content.substring(classMatcher.end(), classEnd);
                classInfo.put("methods", extractMethods(classContent));
                classInfo.put("fields", extractFields(classContent));
            }
            
            classes.add(classInfo);
        }
        
        // Extract interfaces
        Matcher interfaceMatcher = INTERFACE_PATTERN.matcher(content);
        while (interfaceMatcher.find()) {
            Map<String, Object> interfaceInfo = new HashMap<>();
            interfaceInfo.put("type", "interface");
            interfaceInfo.put("visibility", interfaceMatcher.group(1) != null ? interfaceMatcher.group(1) : "default");
            interfaceInfo.put("name", interfaceMatcher.group(2));
            
            if (interfaceMatcher.group(3) != null) {
                String extendsStr = interfaceMatcher.group(3).trim();
                String[] extendsList = extendsStr.split("\\s*,\\s*");
                interfaceInfo.put("extends", Arrays.asList(extendsList));
            }
            
            // Extract methods for this interface
            int interfaceStart = interfaceMatcher.start();
            int interfaceEnd = findMatchingBrace(content, interfaceMatcher.end());
            if (interfaceEnd > interfaceStart) {
                String interfaceContent = content.substring(interfaceMatcher.end(), interfaceEnd);
                interfaceInfo.put("methods", extractMethods(interfaceContent));
            }
            
            classes.add(interfaceInfo);
        }
        
        // Extract enums
        Matcher enumMatcher = ENUM_PATTERN.matcher(content);
        while (enumMatcher.find()) {
            Map<String, Object> enumInfo = new HashMap<>();
            enumInfo.put("type", "enum");
            enumInfo.put("visibility", enumMatcher.group(1) != null ? enumMatcher.group(1) : "default");
            enumInfo.put("name", enumMatcher.group(2));
            
            // Extract enum values
            int enumStart = enumMatcher.start();
            int enumEnd = findMatchingBrace(content, enumMatcher.end());
            if (enumEnd > enumStart) {
                String enumContent = content.substring(enumMatcher.end(), enumEnd);
                enumInfo.put("values", extractEnumValues(enumContent));
                enumInfo.put("methods", extractMethods(enumContent));
            }
            
            classes.add(enumInfo);
        }
        
        result.put("classes", classes);
        return result;
    }
    
    private List<Map<String, Object>> extractMethods(String content) {
        List<Map<String, Object>> methods = new ArrayList<>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(content);
        
        while (methodMatcher.find()) {
            Map<String, Object> methodInfo = new HashMap<>();
            methodInfo.put("visibility", methodMatcher.group(1) != null ? methodMatcher.group(1) : "default");
            methodInfo.put("modifier", methodMatcher.group(2) != null ? methodMatcher.group(2) : "");
            methodInfo.put("returnType", methodMatcher.group(3).trim());
            methodInfo.put("name", methodMatcher.group(4));
            
            String params = methodMatcher.group(5).trim();
            List<Map<String, String>> parameters = new ArrayList<>();
            if (!params.isEmpty()) {
                String[] paramList = params.split("\\s*,\\s*");
                for (String param : paramList) {
                    String[] parts = param.trim().split("\\s+");
                    if (parts.length >= 2) {
                        Map<String, String> paramInfo = new HashMap<>();
                        paramInfo.put("type", parts[0]);
                        paramInfo.put("name", parts[1]);
                        parameters.add(paramInfo);
                    }
                }
            }
            methodInfo.put("parameters", parameters);
            
            methods.add(methodInfo);
        }
        
        return methods;
    }
    
    private List<Map<String, Object>> extractFields(String content) {
        List<Map<String, Object>> fields = new ArrayList<>();
        Matcher fieldMatcher = FIELD_PATTERN.matcher(content);
        
        while (fieldMatcher.find()) {
            Map<String, Object> fieldInfo = new HashMap<>();
            fieldInfo.put("visibility", fieldMatcher.group(1) != null ? fieldMatcher.group(1) : "default");
            fieldInfo.put("modifier", fieldMatcher.group(2) != null ? fieldMatcher.group(2) : "");
            fieldInfo.put("type", fieldMatcher.group(3).trim());
            fieldInfo.put("name", fieldMatcher.group(4));
            
            fields.add(fieldInfo);
        }
        
        return fields;
    }
    
    private List<String> extractEnumValues(String content) {
        List<String> values = new ArrayList<>();
        
        // Look for the enum values section
        int valuesEnd = content.indexOf('{');
        if (valuesEnd == -1) {
            valuesEnd = content.indexOf(';');
        }
        
        if (valuesEnd != -1) {
            String valuesSection = content.substring(0, valuesEnd).trim();
            String[] valuesList = valuesSection.split("\\s*,\\s*");
            
            for (String value : valuesList) {
                // Remove any annotations or comments
                value = value.trim();
                if (value.contains("(")) {
                    value = value.substring(0, value.indexOf('('));
                }
                value = value.trim();
                
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        
        return values;
    }
    
    private String removeComments(String content) {
        // Remove block comments
        content = content.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        
        // Remove line comments
        StringBuilder sb = new StringBuilder();
        String[] lines = content.split("\n");
        for (String line : lines) {
            int commentStart = line.indexOf("//");
            if (commentStart != -1) {
                sb.append(line.substring(0, commentStart));
            } else {
                sb.append(line);
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    private int findMatchingBrace(String content, int startPos) {
        int level = 0;
        boolean inString = false;
        boolean inChar = false;
        
        for (int i = startPos; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '\"' && !inChar) {
                // Handle string literals
                boolean escaped = (i > 0 && content.charAt(i - 1) == '\\');
                if (!escaped) {
                    inString = !inString;
                }
            } else if (c == '\'' && !inString) {
                // Handle character literals
                boolean escaped = (i > 0 && content.charAt(i - 1) == '\\');
                if (!escaped) {
                    inChar = !inChar;
                }
            } else if (!inString && !inChar) {
                if (c == '{') {
                    level++;
                } else if (c == '}') {
                    level--;
                    if (level == 0) {
                        return i;
                    }
                }
            }
        }
        
        // No matching brace found
        return -1;
    }
}
