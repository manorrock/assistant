package com.manorrock.assistant.llm;

/**
 * Provider interface for language models.
 * 
 * @param <T> the type of model
 * @author Manfred Riem (mriem@manorrock.com)
 */
@Deprecated
@FunctionalInterface
public interface LlmModelProvider<T> {
    
    /**
     * Create a new model instance.
     * 
     * @param config the configuration
     * @return the model
     */
    T createModel(LlmConfiguration config);
}
