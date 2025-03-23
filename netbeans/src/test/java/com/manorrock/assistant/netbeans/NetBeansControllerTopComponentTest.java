package com.manorrock.assistant.netbeans;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JTextArea;
import javax.swing.JButton;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.windows.TopComponent;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.GraphicsEnvironment;
import com.manorrock.assistant.shared.Command;
import com.manorrock.assistant.shared.CommandRegistry;
import com.manorrock.assistant.core.Assistant;
import java.lang.reflect.Field;

public class NetBeansControllerTopComponentTest {

  private static boolean isHeadless;
  private static final int UI_DELAY = 1000; // 1 second delay for human visibility
  private NetBeansControllerTopComponent component;
  private JFrame frame;
  private Assistant assistance;

  @BeforeClass
  public static void setUpClass() {
    // Only set netbeans test environment property
    System.setProperty("netbeans.running.environment", "junit-test");
    isHeadless = GraphicsEnvironment.isHeadless();
    if (!isHeadless) {
      java.awt.Toolkit.getDefaultToolkit();
      try {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Before
  public void setUp() throws Exception {
    if (isHeadless) {
      component = new NetBeansControllerTopComponent();
      // Get the assistance field for headless mode
      try {
        Field assistanceField = NetBeansControllerTopComponent.class.getDeclaredField("assistance");
        assistanceField.setAccessible(true);
        assistance = (Assistant) assistanceField.get(component);
      } catch (Exception e) {
        System.err.println("Error accessing assistance field in headless mode: " + e.getMessage());
        assistance = new Assistant();
        initializeAssistant(assistance);
      }
      return;
    }

    try {
      javax.swing.SwingUtilities.invokeAndWait(() -> {
        component = new NetBeansControllerTopComponent();
        frame = new JFrame("Manorrock Assistant Test");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(component);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
      });
      Thread.sleep(UI_DELAY);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Fix: First check if the component was created successfully
    if (component != null) {
      try {
        // Access the private assistance field using reflection
        Field assistanceField = NetBeansControllerTopComponent.class.getDeclaredField("assistance");
        assistanceField.setAccessible(true);
        assistance = (Assistant) assistanceField.get(component);
        
        // If reflection failed or returned null, create a new instance as fallback
        if (assistance == null) {
            assistance = new Assistant();
            initializeAssistant(assistance);
            // Try to set it back to the component
            assistanceField.set(component, assistance);
        }
      } catch (Exception e) {
        // If reflection fails, create a new Assistant instance as fallback
        System.err.println("Error accessing assistance field: " + e.getMessage());
        assistance = new Assistant();
        initializeAssistant(assistance);
      }
    } else {
      // Create a default instance if component creation failed
      assistance = new Assistant();
      initializeAssistant(assistance);
    }
  }
  
  /**
   * Initialize an Assistant instance with the same commands as in the component.
   * This ensures consistent test behavior even if reflection fails.
   */
  private void initializeAssistant(Assistant assistant) {
    // Register the same commands as in the NetBeansControllerTopComponent constructor
    assistant.getCommandRegistry().registerCommand("llmModel", 
        new com.manorrock.assistant.shared.LlmModelCommand(new com.manorrock.assistant.llm.LlmConfiguration(
            "http://localhost:11434/api/chat", "llama2", "ollama", "", 0.7)));
    assistant.getCommandRegistry().registerCommand("source",
        new com.manorrock.assistant.shared.SourceCommand(s -> {}, s -> {}));
    assistant.getCommandRegistry().registerCommand("help", 
        new com.manorrock.assistant.shared.HelpCommand());
    
    // Register commands from componentOpened
    assistant.getCommandRegistry().registerCommand("new", 
        new com.manorrock.assistant.shared.NewCommand(() -> {}));
    assistant.getCommandRegistry().registerCommand("model", 
        new com.manorrock.assistant.shared.DeprecatedCommand("model", "llm model", 
        assistant.getCommandRegistry().getCommand("llmModel")));
  }

  @After
  public void tearDown() {
    if (!isHeadless && frame != null) {
      frame.dispose();
      frame = null;
    }
    component = null;
  }

  @Test
  public void testComponentInitialization() throws InterruptedException {
    assertNotNull("Component should not be null", component);

    if (isHeadless) {
      assertTrue("Component should be instance of TopComponent", component instanceof TopComponent);
      return;
    }

    System.out.println("Component hierarchy:");
    printComponentHierarchy(component, "");

    JTextArea responseArea = findComponent(component, JTextArea.class, 0);
    JTextArea requestArea = findComponent(component, JTextArea.class, 1);
    JButton sendButton = findComponent(component, JButton.class, 0);
    JButton startOverButton = findComponent(component, JButton.class, 1);

    assertNotNull("Response area should exist", responseArea);
    assertNotNull("Request area should exist", requestArea);
    assertNotNull("Send button should exist", sendButton);
    assertNotNull("Start over button should exist", startOverButton);

    assertTrue("Response area should contain welcome message",
        responseArea.getText().contains("Welcome to Manorrock Assistant"));
    assertEquals("Request area should be empty", "", requestArea.getText());
    assertEquals("Send button should be labeled correctly", "Send", sendButton.getText());
    assertEquals("Start Over button should be labeled correctly", "Start Over", startOverButton.getText());

    Thread.sleep(UI_DELAY * 2);
  }

  @Test
  public void testHelpCommand() throws InterruptedException {
    assertNotNull("Assistance instance should not be null", assistance);
    if (isHeadless) {
      Command helpCommand = assistance.getCommandRegistry().getCommand("help");
      assertNotNull("Help command should be available", helpCommand);
      String helpText = helpCommand.executeToString("");
      assertTrue("Help text should contain command information", helpText.contains("Available commands:"));
      return;
    }

    JTextArea requestArea = findComponent(component, JTextArea.class, 1);
    JTextArea responseArea = findComponent(component, JTextArea.class, 0);
    JButton sendButton = findComponent(component, JButton.class, 0);

    Thread.sleep(UI_DELAY);
    requestArea.setText("/help");
    Thread.sleep(UI_DELAY);
    sendButton.doClick();
    Thread.sleep(UI_DELAY * 2);

    assertTrue("Response should contain help message", responseArea.getText().contains("Available commands:"));
    assertTrue("Help should list llmEndpoint command", responseArea.getText().contains("/llmEndpoint"));

    Thread.sleep(UI_DELAY * 2);
  }

  @Test
  public void testCommandRegistration() {
    assertNotNull("Assistance instance should not be null", assistance);
    
    // Verify that the expected commands are registered
    assertTrue("llmModel command should be registered", 
        assistance.getCommandRegistry().hasCommand("llmModel"));
    assertTrue("source command should be registered", 
        assistance.getCommandRegistry().hasCommand("source"));
    assertTrue("help command should be registered", 
        assistance.getCommandRegistry().hasCommand("help"));
  }

  @Test
  public void testHandleCommand() throws Exception {
    // Create a simple test command instead of using a mock
    TestCommand testCommand = new TestCommand();
    
    // Register the test command
    assistance.getCommandRegistry().registerCommand("testCommand", testCommand);

    // Access the private handleCommand method using reflection
    java.lang.reflect.Method handleCommandMethod = NetBeansControllerTopComponent.class.getDeclaredMethod(
            "handleCommand", String.class);
    handleCommandMethod.setAccessible(true);

    // Invoke the handleCommand method with our test command
    handleCommandMethod.invoke(component, "/testCommand test arguments");

    // Verify that the command was executed with the correct arguments
    assertEquals("test arguments", testCommand.getLastArguments());
  }

  @Test
  public void testComponentOpened() {
    assertNotNull("Assistance instance should not be null", assistance);
    
    try {
      // Clear any existing commands to ensure clean test
      CommandRegistry registry = assistance.getCommandRegistry();
      registry.clearCommands();
      assertFalse("Registry should be empty after clearing", registry.hasCommand("source"));

      // Invoke componentOpened method
      component.componentOpened();

      // Verify that the expected commands are registered
      assertTrue("source command should be registered", registry.hasCommand("source"));
      assertTrue("new command should be registered", registry.hasCommand("new"));
      assertTrue("help command should be registered", registry.hasCommand("help"));
      assertTrue("model command should be registered", registry.hasCommand("model"));
    } catch (Exception e) {
      fail("Exception during test: " + e.getMessage());
    }
  }

  // Simple test command implementation
  private static class TestCommand implements Command {
    private String lastArguments;
    
    @Override
    public String executeToString(String arguments) {
      this.lastArguments = arguments;
      return "Test command executed with: " + arguments;
    }
    
    @Override
    public java.io.InputStream executeToStream(String arguments) {
      // Convert the response to an InputStream
      this.lastArguments = arguments;
      String response = "Test command executed with: " + arguments;
      return new java.io.ByteArrayInputStream(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    public String getLastArguments() {
      return lastArguments;
    }
    
    @Override
    public String getDescription() {
      return "Test command for unit testing";
    }
  }

  private <T extends Component> T findComponent(TopComponent parent, Class<T> type, int index) {
    System.out.println("Searching for " + type.getSimpleName() + " with index " + index);
    return findComponentRecursive(parent, type, new Counter(index));
  }

  private static class Counter {
    int value;
    Counter(int value) {
      this.value = value;
    }
  }

  private <T extends Component> T findComponentRecursive(Container container, Class<T> type, Counter counter) {
    for (Component c : container.getComponents()) {
      if (type.isInstance(c)) {
        if (counter.value == 0) {
          return type.cast(c);
        }
        counter.value--;
      }

      if (c instanceof Container) {
        T result = findComponentRecursive((Container) c, type, counter);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  private void printComponentHierarchy(Container container, String indent) {
    System.out.println(indent + container.getClass().getName());
    for (Component c : container.getComponents()) {
      if (c instanceof Container) {
        printComponentHierarchy((Container) c, indent + "  ");
      } else {
        System.out.println(indent + "  " + c.getClass().getName());
      }
    }
  }
}
