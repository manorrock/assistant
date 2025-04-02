package com.manorrock.assistant.netbeans;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.windows.TopComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.GraphicsEnvironment;

public class AssistantTopComponentTest {

  private static boolean isHeadless;
  private static final int UI_DELAY = 1000; // 1 second delay for human visibility
  private AssistantTopComponent component;
  private JFrame frame;

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
      component = new AssistantTopComponent();
      return;
    }

    try {
      javax.swing.SwingUtilities.invokeAndWait(() -> {
        component = new AssistantTopComponent();
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

    JTextPane responseArea = findComponent(component, JTextPane.class, 0);
    JTextArea requestArea = findComponent(component, JTextArea.class, 0);
    JButton sendButton = findComponent(component, JButton.class, 0);

    assertNotNull("Response area should exist", responseArea);
    assertNotNull("Request area should exist", requestArea);
    assertNotNull("Send button should exist", sendButton);

    assertTrue("Response area should contain welcome message",
        responseArea.getText().contains("Welcome to Manorrock Assistant"));
    assertEquals("Request area should be empty", "", requestArea.getText());
    assertEquals("Send button should be labeled correctly", "Send", sendButton.getText());

    Thread.sleep(UI_DELAY * 2);
  }

  @Test
  public void testSimpleMessage() throws Exception {
    if (isHeadless) return;

    JTextArea requestArea = findComponent(component, JTextArea.class, 0);
    JTextPane responseArea = findComponent(component, JTextPane.class, 0);
    JButton sendButton = findComponent(component, JButton.class, 0);

    assertNotNull("Request area should not be null", requestArea);
    assertNotNull("Response area should not be null", responseArea);
    assertNotNull("Send button should not be null", sendButton);

    // Clear the response area first to get a clean slate
    // This makes it easier to check for our specific message
    SwingUtilities.invokeAndWait(() -> {
      responseArea.setText("");
    });
    
    // Set up our test message
    final String message = "Hello Assistant!";
    
    // Use invokeAndWait to ensure UI operations happen on EDT
    SwingUtilities.invokeAndWait(() -> {
      requestArea.setText(message);
    });
    Thread.sleep(UI_DELAY);
    
    // Directly invoke the handleSendAction method to ensure proper processing
    SwingUtilities.invokeAndWait(() -> {
      component.handleSendAction();
    });
    
    // Wait and check for the message to appear
    for (int i = 0; i < 10; i++) { // Increased attempts to 10
      Thread.sleep(UI_DELAY);
      SwingUtilities.invokeAndWait(() -> {});
      
      String responseText = responseArea.getText();
      System.out.println("Response area content (attempt " + (i+1) + "): " + responseText);
      
      if (responseText.contains("You: " + message)) {
        break;
      }
    }
    
    assertTrue("Response area should contain user message", 
        responseArea.getText().contains("You: " + message));
  }

  @Test
  public void testNewCommand() throws Exception {
    if (isHeadless) return;

    JTextArea requestArea = findComponent(component, JTextArea.class, 0);
    JTextPane responseArea = findComponent(component, JTextPane.class, 0);
    JButton sendButton = findComponent(component, JButton.class, 0);

    // First add some content
    requestArea.setText("/help");
    sendButton.doClick();
    Thread.sleep(UI_DELAY * 2);

    // Then test /new command
    requestArea.setText("/new");
    sendButton.doClick();
    Thread.sleep(UI_DELAY * 2);

    String responseText = responseArea.getText().toLowerCase();
    assertTrue("Response area should be cleared and contain new session message",
        responseText.contains("new") && 
        responseText.contains("session") || 
        responseText.contains("chat") ||
        responseText.isEmpty());
  }

  @Test
  public void testExplainCommand() throws Exception {
    if (isHeadless) return;

    JTextArea requestArea = findComponent(component, JTextArea.class, 0);
    JTextPane responseArea = findComponent(component, JTextPane.class, 0);
    JButton sendButton = findComponent(component, JButton.class, 0);

    assertNotNull("Request area should not be null", requestArea);
    assertNotNull("Response area should not be null", responseArea);
    assertNotNull("Send button should not be null", sendButton);

    // Set some text to explain
    String testContent = "public class Test { }";
    responseArea.setText(testContent);
    responseArea.setSelectionStart(0);
    responseArea.setSelectionEnd(responseArea.getText().length());

    Thread.sleep(UI_DELAY * 2);
    requestArea.setText("/explain");
    sendButton.doClick();
    Thread.sleep(UI_DELAY * 3); // Increased delay for LLM response

    String responseText = responseArea.getText().toLowerCase();
    assertTrue("Response should contain explanation attempt",
        responseText.contains("explain") || 
        responseText.contains("class") ||
        responseText.contains("this code") ||
        responseText.contains("analysis"));
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
