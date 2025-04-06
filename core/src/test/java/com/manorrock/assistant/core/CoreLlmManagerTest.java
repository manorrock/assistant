package com.manorrock.assistant.core;

import com.manorrock.assistant.api.Llm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CoreLlmManagerTest {

    private CoreLlmManager coreLlmManager;

    @BeforeEach
    void setUp() {
        coreLlmManager = new CoreLlmManager(null);
    }

    @Test
    void testRegisterLlm() {
        Llm mockLlm = mock(Llm.class);
        coreLlmManager.registerLlm("testLlm", mockLlm);

        assertEquals(mockLlm, coreLlmManager.getLlm("testLlm"));
    }

    @Test
    void testGetLlm() {
        Llm mockLlm = mock(Llm.class);
        coreLlmManager.registerLlm("testLlm", mockLlm);

        Llm retrievedLlm = coreLlmManager.getLlm("testLlm");
        assertNotNull(retrievedLlm);
        assertEquals(mockLlm, retrievedLlm);
    }

    @Test
    void testUnregisterLlm() {
        Llm mockLlm = mock(Llm.class);
        coreLlmManager.registerLlm("testLlm", mockLlm);

        coreLlmManager.unregisterLLM("testLlm");
        assertNull(coreLlmManager.getLlm("testLlm"));
    }

    @Test
    void testGetLLMs() {
        Llm mockLlm1 = mock(Llm.class);
        Llm mockLlm2 = mock(Llm.class);

        coreLlmManager.registerLlm("llm1", mockLlm1);
        coreLlmManager.registerLlm("llm2", mockLlm2);

        Map<String, Llm> llms = coreLlmManager.getLlms();
        assertEquals(2, llms.size());
        assertTrue(llms.containsKey("llm1"));
        assertTrue(llms.containsKey("llm2"));
    }
}