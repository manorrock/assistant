package com.manorrock.assistant.core;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

/**
 * Utility class for estimating token counts for various LLM models.
 */
public class CoreTokenCounterUtil {

    /**
     * The encoding registry from jtokkit.
     */
    private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    /**
     * Count tokens for a given text using the specified model's encoding.
     * 
     * @param text The text to count tokens for
     * @param modelName The name of the model
     * @return Token count
     */
    public static int countTokens(String text, String modelName) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        EncodingType encodingType = getEncodingTypeForModel(modelName);
        Encoding encoding = registry.getEncoding(encodingType);
        
        return encoding.countTokens(text);
    }
    
    /**
     * Get the encoding type appropriate for a given model.
     * 
     * @param modelName The name of the model
     * @return The appropriate EncodingType
     */
    private static EncodingType getEncodingTypeForModel(String modelName) {
        if (modelName == null) {
            return EncodingType.CL100K_BASE; // Default for newer models
        }
        
        modelName = modelName.toLowerCase();
        
        if (modelName.contains("gpt-4") || modelName.contains("gpt4")) {
            return EncodingType.CL100K_BASE;
        } else if (modelName.contains("gpt-3.5") || modelName.contains("gpt35")) {
            return EncodingType.CL100K_BASE;
        } else if (modelName.contains("llama") || modelName.contains("claude")) {
            return EncodingType.CL100K_BASE; // Best approximation for these models
        } else if (modelName.contains("davinci") || modelName.contains("curie") || 
                   modelName.contains("babbage") || modelName.contains("ada")) {
            return EncodingType.P50K_BASE;
        } else {
            // Default to cl100k for newer models
            return EncodingType.CL100K_BASE;
        }
    }
}
