package org.epos.dbconnector.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Iterator;
import java.util.Map;

/**
 * Utility class for handling JSON with nested escaped JSON strings.
 * 
 * The configuration data has a specific structure where:
 * 1. The entire value is a JSON string (wrapped in quotes)
 * 2. Inside, there's a JSON object
 * 3. Some values within that object are also stringified JSON
 * 
 * This utility helps:
 * - Normalize (unescape) nested JSON for readable output
 * - Denormalize (re-escape) for storage/encryption
 */
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Normalizes a JSON string by unescaping nested JSON strings.
     * Converts the "strange" escaped format to proper nested JSON.
     * 
     * Input example:  "{\"key\":\"[{\\\"nested\\\":\\\"value\\\"}]\"}"
     * Output example: {"key":[{"nested":"value"}]}
     *
     * @param escapedJson The JSON string with escaped nested JSON
     * @return A properly formatted JSON string with nested objects/arrays
     */
    public static String normalize(String escapedJson) {
        if (escapedJson == null || escapedJson.isEmpty()) {
            return escapedJson;
        }

        try {
            String json = escapedJson;
            
            // If the entire string is wrapped in quotes (it's a JSON string value), unwrap it
            if (json.startsWith("\"") && json.endsWith("\"")) {
                // Parse it as a JSON string to properly unescape
                json = objectMapper.readValue(json, String.class);
            }

            // Parse the JSON
            JsonNode rootNode = objectMapper.readTree(json);
            
            // Recursively process all nodes to unescape nested JSON strings
            JsonNode normalizedNode = normalizeNode(rootNode);
            
            // Return as formatted JSON
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalizedNode);
            
        } catch (JsonProcessingException e) {
            // If parsing fails, return the original
            return escapedJson;
        }
    }

    /**
     * Normalizes a JSON string without pretty printing.
     *
     * @param escapedJson The JSON string with escaped nested JSON
     * @return A compact normalized JSON string
     */
    public static String normalizeCompact(String escapedJson) {
        if (escapedJson == null || escapedJson.isEmpty()) {
            return escapedJson;
        }

        try {
            String json = escapedJson;
            
            // If the entire string is wrapped in quotes (it's a JSON string value), unwrap it
            if (json.startsWith("\"") && json.endsWith("\"")) {
                json = objectMapper.readValue(json, String.class);
            }

            // Parse the JSON
            JsonNode rootNode = objectMapper.readTree(json);
            
            // Recursively process all nodes to unescape nested JSON strings
            JsonNode normalizedNode = normalizeNode(rootNode);
            
            // Return as compact JSON
            return objectMapper.writeValueAsString(normalizedNode);
            
        } catch (JsonProcessingException e) {
            return escapedJson;
        }
    }

    /**
     * Denormalizes a proper JSON back to the escaped string format.
     * This is the reverse of normalize() - converts proper nested JSON
     * back to the "strange" format with stringified values.
     * 
     * Input example:  {"key":[{"nested":"value"}]}
     * Output example: "{\"key\":\"[{\\\"nested\\\":\\\"value\\\"}]\"}"
     *
     * @param normalizedJson The properly formatted JSON
     * @param wrapInQuotes Whether to wrap the entire result in quotes (as original format)
     * @return The escaped JSON string format
     */
    public static String denormalize(String normalizedJson, boolean wrapInQuotes) {
        if (normalizedJson == null || normalizedJson.isEmpty()) {
            return normalizedJson;
        }

        try {
            // Parse the normalized JSON
            JsonNode rootNode = objectMapper.readTree(normalizedJson);
            
            // Recursively process to stringify nested objects/arrays
            JsonNode denormalizedNode = denormalizeNode(rootNode);
            
            // Convert to string
            String result = objectMapper.writeValueAsString(denormalizedNode);
            
            // If we need to wrap in quotes (original format), stringify the result
            if (wrapInQuotes) {
                result = objectMapper.writeValueAsString(result);
            }
            
            return result;
            
        } catch (JsonProcessingException e) {
            return normalizedJson;
        }
    }

    /**
     * Denormalizes without wrapping in outer quotes.
     */
    public static String denormalize(String normalizedJson) {
        return denormalize(normalizedJson, false);
    }

    /**
     * Recursively normalizes a JsonNode by parsing string values that contain JSON.
     */
    private static JsonNode normalizeNode(JsonNode node) throws JsonProcessingException {
        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                objectNode.set(field.getKey(), normalizeNode(field.getValue()));
            }
            return objectNode;
        } else if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(normalizeNode(element));
            }
            return arrayNode;
        } else if (node.isTextual()) {
            String text = node.asText();
            // Try to parse as JSON if it looks like JSON
            if (looksLikeJson(text)) {
                try {
                    JsonNode parsed = objectMapper.readTree(text);
                    return normalizeNode(parsed);
                } catch (JsonProcessingException e) {
                    // Not valid JSON, return as-is
                    return node;
                }
            }
            return node;
        } else {
            return node;
        }
    }

    /**
     * Recursively denormalizes a JsonNode by stringifying nested objects/arrays.
     * Only stringifies values that would have been stringified in the original format.
     */
    private static JsonNode denormalizeNode(JsonNode node) throws JsonProcessingException {
        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode value = field.getValue();
                
                // Check if this value should be stringified
                // Based on the original format, array and object values in the root level are stringified
                if (value.isArray() || value.isObject()) {
                    // First process children recursively
                    JsonNode processedValue = denormalizeNodeDeep(value);
                    // Then stringify the result
                    String stringified = objectMapper.writeValueAsString(processedValue);
                    objectNode.put(key, stringified);
                } else {
                    objectNode.set(key, value);
                }
            }
            return objectNode;
        } else {
            return node;
        }
    }

    /**
     * Recursively processes nodes for denormalization at deeper levels.
     * At deeper levels, we don't stringify - we just process the structure.
     */
    private static JsonNode denormalizeNodeDeep(JsonNode node) throws JsonProcessingException {
        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode value = field.getValue();
                
                // At deeper levels, check if the value itself contains nested structures
                // that need to be stringified (like the "value" field in configurables)
                if (shouldStringify(key, value)) {
                    JsonNode processedValue = denormalizeNodeDeep(value);
                    String stringified = objectMapper.writeValueAsString(processedValue);
                    objectNode.put(key, stringified);
                } else {
                    objectNode.set(key, denormalizeNodeDeep(value));
                }
            }
            return objectNode;
        } else if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(denormalizeNodeDeep(element));
            }
            return arrayNode;
        } else {
            return node;
        }
    }

    /**
     * Determines if a value should be stringified based on the key and value type.
     * This is heuristic-based on the observed data structure.
     */
    private static boolean shouldStringify(String key, JsonNode value) {
        // The "value" key in configurables often contains stringified JSON
        if ("value".equals(key) && (value.isArray() || value.isObject())) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a string looks like it could be JSON.
     */
    private static boolean looksLikeJson(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * Validates if a string is valid JSON.
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
