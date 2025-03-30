package com.manorrock.assistant.tool;

import com.manorrock.assistant.api.ToolParameter;
import com.manorrock.assistant.api.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WebScraperTool extends AbstractTool {
    private static final int DEFAULT_TIMEOUT = 30;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public WebScraperTool() {
        super("web_scraper",  // This is correct
              "Scrapes content from web pages using CSS selectors",
              Arrays.asList(
                  new ToolParameter("url", "string", "Target webpage URL", true),
                  new ToolParameter("method", "string", 
                      "HTTP method (GET, HEAD, POST, PUT). Default: GET", false),
                  new ToolParameter("selector", "string", 
                      "CSS selector for targeting elements. If omitted, returns whole document", false),
                  new ToolParameter("headers", "object", 
                      "HTTP headers to include in request. Values must be strings", false),
                  new ToolParameter("data", "object", 
                      "Request body data for POST/PUT requests. Can contain any valid JSON values", false),
                  new ToolParameter("attribute", "string", 
                      "HTML attribute to extract from matched elements. Default: text", false),
                  new ToolParameter("timeout", "number", 
                      "Connection timeout in seconds. Default: 30", false),
                  new ToolParameter("followRedirects", "boolean", 
                      "Whether to follow redirects. Default: true", false),
                  new ToolParameter("outputFormat", "string", 
                      "Format of the output (html/text/json). Default: text", false)
              ));
    }

    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters) throws Exception {
        // Extract parameters
        String url = getRequiredString(parameters, "url");
        String method = getString(parameters, "method", "GET");
        String selector = getString(parameters, "selector", "");
        String attribute = getString(parameters, "attribute", "text");
        int timeout = getInteger(parameters, "timeout", DEFAULT_TIMEOUT);
        boolean followRedirects = getBoolean(parameters, "followRedirects", true);
        String outputFormat = getString(parameters, "outputFormat", "text");

        // Validate URL
        validateUrl(url);

        // Store request headers for later use
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("User-Agent", "Manorrock-Assistant");

        // Configure connection
        Connection connection = Jsoup.connect(url)
                .method(Connection.Method.valueOf(method))
                .timeout((int) TimeUnit.SECONDS.toMillis(timeout))
                .followRedirects(followRedirects)
                .userAgent("Manorrock-Assistant");

        // Add custom headers
        if (parameters.containsKey("headers") && parameters.get("headers") != null) {
            Object headersObj = parameters.get("headers");
            if (headersObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) headersObj;
                headers.forEach((key, value) -> {
                    connection.header(key, value);
                    requestHeaders.put(key, value);
                });
            } else if (headersObj instanceof String && !((String) headersObj).isEmpty()) {
                // If headers is a non-empty string but not a map, it's an error
                return ToolResult.failure("Headers parameter must be a map of key-value pairs");
            }
            // If it's an empty string, just ignore it
        }

        // Add request body for POST/PUT
        if (parameters.containsKey("data") && parameters.get("data") != null && 
            (method.equals("POST") || method.equals("PUT"))) {
            Object dataObj = parameters.get("data");
            if (dataObj instanceof Map || dataObj instanceof List) {
                connection.requestBody(MAPPER.writeValueAsString(dataObj));
            } else if (dataObj instanceof String) {
                if (!((String) dataObj).isEmpty()) {
                    connection.requestBody((String) dataObj);
                }
                // If it's an empty string, just ignore it
            } else {
                connection.requestBody(dataObj.toString());
            }
        }

        // Execute request and parse
        Connection.Response response = connection.execute();
        Document document = response.parse();

        // Extract content
        String content;
        if (!selector.isEmpty()) {
            Elements elements = document.select(selector);
            if (elements.isEmpty()) {
                return ToolResult.failure("No elements found matching selector: " + selector);
            }
            content = extractContent(elements, attribute, outputFormat);
        } else {
            content = formatOutput(document, outputFormat);
        }

        // Build result data
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("url", response.url().toString());
        resultData.put("rawUrl", url);
        resultData.put("method", method);
        resultData.put("statusCode", response.statusCode());
        resultData.put("content", content);
        resultData.put("requestHeaders", requestHeaders);
        resultData.put("responseHeaders", response.headers());

        return ToolResult.success(resultData, "Successfully retrieved document");
    }

    @SuppressWarnings("deprecation")
    private void validateUrl(String url) throws Exception {
        new URL(url);
    }

    private String extractContent(Elements elements, String attribute, String outputFormat) {
        if (attribute.equals("text")) {
            return formatOutput(elements, outputFormat);
        }
        return elements.stream()
                .map(e -> e.attr(attribute))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private String formatOutput(Object content, String format) {
        switch (format) {
            case "html":
                return content instanceof Elements ? 
                       ((Elements) content).outerHtml() : 
                       ((Document) content).outerHtml();
            case "json":
                return formatAsJson(content);
            default: // text
                return content instanceof Elements ? 
                       ((Elements) content).text() : 
                       ((Document) content).text();
        }
    }

    private String formatAsJson(Object content) {
        try {
            if (content instanceof Elements elements) {
                ObjectNode rootNode = MAPPER.createObjectNode();
                rootNode.put("count", elements.size());
                
                ArrayNode elementArray = rootNode.putArray("elements");
                elements.forEach(element -> elementArray.add(element.outerHtml()));
                
                return MAPPER.writeValueAsString(rootNode);
            } else if (content instanceof Document document) {
                ObjectNode rootNode = MAPPER.createObjectNode();
                rootNode.put("title", document.title());
                rootNode.put("content", document.text());
                
                return MAPPER.writeValueAsString(rootNode);
            }
            return "{}"; // Empty JSON object for unknown content types
        } catch (Exception e) {
            // In case of serialization error, return an error JSON
            try {
                ObjectNode errorNode = MAPPER.createObjectNode();
                errorNode.put("error", "Failed to serialize content to JSON: " + e.getMessage());
                return MAPPER.writeValueAsString(errorNode);
            } catch (Exception ex) {
                return "{\"error\":\"Failed to serialize content to JSON\"}";
            }
        }
    }

    private String getRequiredString(Map<String, Object> params, String name) {
        Object value = params.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + name);
        }
        return value.toString();
    }

    private String getString(Map<String, Object> params, String name, String defaultValue) {
        Object value = params.get(name);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInteger(Map<String, Object> params, String name, int defaultValue) {
        Object value = params.get(name);
        if (value == null) {
            return defaultValue;
        }
        
        // Handle floating-point numbers by converting to double first, then to int
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            // For string values, try to parse as double first to handle decimal values
            try {
                return (int) Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Invalid integer value for parameter '" + name + "': " + value);
            }
        }
    }

    private boolean getBoolean(Map<String, Object> params, String name, boolean defaultValue) {
        Object value = params.get(name);
        return value != null ? Boolean.parseBoolean(value.toString()) : defaultValue;
    }
}
