package com.manorrock.assistant.core;

import com.manorrock.assistant.api.AgentMessage;
import com.manorrock.assistant.api.AgentResponse;
import com.manorrock.assistant.api.AgentStreamingResponseHandler;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CoreEchoAgentTest {
    @Test
    void testStatelessProcess() {
        CoreEchoAgent agent = new CoreEchoAgent();
        String response = agent.process("Hello");
        assertEquals("Echo: Hello", response);
    }

    @Test
    void testStatelessStreaming() {
        CoreEchoAgent agent = new CoreEchoAgent();
        StringBuilder builder = new StringBuilder();
        agent.processStreaming("Hi", new AgentStreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                builder.append(token);
            }
            @Override
            public void onComplete() {}
            @Override
            public void onError(String error) { fail(error); }
        });
        assertEquals("Echo: Hi", builder.toString());
    }

    @Test
    void testSessionLifecycle() {
        CoreEchoAgent agent = new CoreEchoAgent();
        String sessionId = agent.startSession(Map.of("foo", "bar"));
        assertNotNull(sessionId);
        assertEquals("bar", agent.getSessionContext(sessionId).get("foo"));
        agent.endSession(sessionId);
        assertTrue(agent.getSessionContext(sessionId).isEmpty());
    }

    @Test
    void testSessionMessage() {
        CoreEchoAgent agent = new CoreEchoAgent();
        String sessionId = agent.startSession(Collections.emptyMap());
        AgentResponse response = agent.sendMessage(sessionId, "Echo this", List.of(new AgentMessage("user", "Echo this")));
        assertEquals("Echo: Echo this", response.getContent());
        assertEquals(sessionId, response.getMetadata().get("sessionId"));
    }

    @Test
    void testSessionStreaming() {
        CoreEchoAgent agent = new CoreEchoAgent();
        String sessionId = agent.startSession(Collections.emptyMap());
        AtomicReference<String> streamed = new AtomicReference<>("");
        agent.sendMessageStreaming(sessionId, "Stream me", List.of(new AgentMessage("user", "Stream me")), new AgentStreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                streamed.set(streamed.get() + token);
            }
            @Override
            public void onComplete() {}
            @Override
            public void onError(String error) { fail(error); }
        });
        assertEquals("Echo: Stream me", streamed.get());
    }
}
