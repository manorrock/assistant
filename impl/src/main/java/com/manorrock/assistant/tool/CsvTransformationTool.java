package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.Tool;
import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tool for transforming CSV data.
 */
public class CsvTransformationTool implements Tool {
    
    private static final String NAME = "csv_transform";
    private static final String DESCRIPTION = "Transforms CSV data with operations like filtering, sorting, and column manipulation";
    private static final List<ToolParameter> PARAMETERS = Arrays.asList(
        new ToolParameter("input", "string", "CSV data to transform", true),
        new ToolParameter("delimiter", "string", "CSV delimiter (default: ,)", false),
        new ToolParameter("operations", "array", "List of operations to perform", true),
        new ToolParameter("header", "boolean", "Whether input has a header row (default: true)", false),
        new ToolParameter("outputDelimiter", "string", "Output CSV delimiter (default: same as input)", false)
    );
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return PARAMETERS;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            String input = (String) parameters.get("input");
            if (input == null || input.trim().isEmpty()) {
                return ToolResult.failure("Input CSV data is required");
            }
            
            String delimiter = (String) parameters.getOrDefault("delimiter", ",");
            boolean hasHeader = (boolean) parameters.getOrDefault("header", true);
            String outputDelimiter = (String) parameters.getOrDefault("outputDelimiter", delimiter);
            List<Map<String, String>> operations = (List<Map<String, String>>) parameters.get("operations");
            
            if (operations == null || operations.isEmpty()) {
                return ToolResult.failure("At least one operation is required");
            }
            
            // Parse CSV
            List<String[]> csvData = parseCsv(input, delimiter);
            if (csvData.isEmpty()) {
                return ToolResult.failure("Failed to parse CSV data");
            }
            
            // Process operations
            for (Map<String, String> operation : operations) {
                csvData = processOperation(csvData, operation, hasHeader);
            }
            
            // Convert back to CSV
            String result = toCsv(csvData, outputDelimiter);
            
            return ToolResult.success(
                Map.of(
                    "result", result,
                    "rowCount", csvData.size() - (hasHeader ? 1 : 0),
                    "columnCount", csvData.isEmpty() ? 0 : csvData.get(0).length
                ),
                "CSV transformation completed successfully"
            );
            
        } catch (Exception e) {
            return ToolResult.failure("Error transforming CSV: " + e.getMessage());
        }
    }
    
    private List<String[]> parseCsv(String input, String delimiter) {
        List<String[]> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                results.add(line.split(delimiter, -1));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage());
        }
        return results;
    }
    
    private List<String[]> processOperation(List<String[]> data, Map<String, String> operation, boolean hasHeader) {
        String type = operation.get("type");
        if (type == null) {
            throw new IllegalArgumentException("Operation type is required");
        }
        
        return switch (type.toLowerCase()) {
            case "select" -> selectColumns(data, operation.get("columns").split(","), hasHeader);
            case "filter" -> filterRows(data, operation.get("column"), operation.get("condition"), hasHeader);
            case "sort" -> sortRows(data, operation.get("column"), 
                                  operation.getOrDefault("ascending", "true"), hasHeader);
            default -> throw new IllegalArgumentException("Unknown operation type: " + type);
        };
    }
    
    private List<String[]> selectColumns(List<String[]> data, String[] columns, boolean hasHeader) {
        if (data.isEmpty()) return data;
        
        int[] columnIndices = new int[columns.length];
        String[] headers = data.get(0);
        
        // Find column indices
        for (int i = 0; i < columns.length; i++) {
            if (hasHeader) {
                columnIndices[i] = findColumnIndex(headers, columns[i].trim());
            } else {
                columnIndices[i] = Integer.parseInt(columns[i].trim());
            }
        }
        
        // Create new data with selected columns
        List<String[]> result = new ArrayList<>();
        for (String[] row : data) {
            String[] newRow = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                newRow[i] = row[columnIndices[i]];
            }
            result.add(newRow);
        }
        
        return result;
    }
    
    private List<String[]> filterRows(List<String[]> data, String column, String condition, boolean hasHeader) {
        if (data.isEmpty()) return data;
        
        int columnIndex = hasHeader ? findColumnIndex(data.get(0), column) : Integer.parseInt(column);
        List<String[]> result = new ArrayList<>();
        
        // Always include header if present
        if (hasHeader) {
            result.add(data.get(0));
        }
        
        // Filter rows based on condition
        for (int i = hasHeader ? 1 : 0; i < data.size(); i++) {
            String[] row = data.get(i);
            if (evaluateCondition(row[columnIndex], condition)) {
                result.add(row);
            }
        }
        
        return result;
    }
    
    private List<String[]> sortRows(List<String[]> data, String column, String ascending, boolean hasHeader) {
        if (data.size() <= (hasHeader ? 1 : 0)) return data;
        
        int columnIndex = hasHeader ? findColumnIndex(data.get(0), column) : Integer.parseInt(column);
        boolean isAscending = Boolean.parseBoolean(ascending);
        
        List<String[]> result = new ArrayList<>(data);
        int startIndex = hasHeader ? 1 : 0;
        
        // Sort all rows except header
        result.subList(startIndex, result.size()).sort((a, b) -> {
            int comparison = a[columnIndex].compareTo(b[columnIndex]);
            return isAscending ? comparison : -comparison;
        });
        
        return result;
    }
    
    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName.trim())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Column not found: " + columnName);
    }
    
    private boolean evaluateCondition(String value, String condition) {
        String op = condition.substring(0, 2);
        String target = condition.substring(2);
        
        return switch (op) {
            case "==" -> value.equals(target);
            case "!=" -> !value.equals(target);
            case ">=" -> value.compareTo(target) >= 0;
            case "<=" -> value.compareTo(target) <= 0;
            case "~=" -> value.matches(target);
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }
    
    private String toCsv(List<String[]> data, String delimiter) {
        StringWriter writer = new StringWriter();
        for (String[] row : data) {
            writer.write(String.join(delimiter, row));
            writer.write("\n");
        }
        return writer.toString();
    }
}
