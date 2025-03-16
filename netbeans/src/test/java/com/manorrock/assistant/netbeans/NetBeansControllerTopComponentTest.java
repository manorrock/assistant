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

public class NetBeansControllerTopComponentTest {

  private static final int UI_DELAY = 1000; // 1 second delay for human visibility

  private NetBeansControllerTopComponent component;
  private JFrame frame; // Add frame field

  @BeforeClass
  public static void setUpClass() {
    // Initialize AWT toolkit
    java.awt.Toolkit.getDefaultToolkit();
    try {
      javax.swing.SwingUtilities.invokeAndWait(() -> {
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Before
  public void setUp() {
    try {
      javax.swing.SwingUtilities.invokeAndWait(() -> {
        component = new NetBeansControllerTopComponent();

        // Create and setup frame
        frame = new JFrame("Manorrock Assistant Test");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);

        // Add component to frame
        frame.add(component);

        // Center on screen
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
      });
      Thread.sleep(UI_DELAY); // Let human see initial state
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @After
  public void tearDown() {
    if (frame != null) {
      frame.dispose();
      frame = null;
    }
    component = null;
  }

  @Test
  public void testComponentInitialization() throws InterruptedException {
    // Verify component was initialized
    assertNotNull("Component should not be null", component);

    // Print component hierarchy for debugging
    System.out.println("Component hierarchy:");
    printComponentHierarchy(component, "");

    // Find the text areas and buttons
    JTextArea responseArea = findComponent(component, JTextArea.class, 0);
    System.out.println("Found response area: " + (responseArea != null));

    JTextArea requestArea = findComponent(component, JTextArea.class, 1);
    System.out.println("Found request area: " + (requestArea != null));

    JButton sendButton = findComponent(component, JButton.class, 0);
    JButton startOverButton = findComponent(component, JButton.class, 1);

    assertNotNull("Response area should exist", responseArea);
    assertNotNull("Request area should exist", requestArea);
    assertNotNull("Send button should exist", sendButton);
    assertNotNull("Start over button should exist", startOverButton);

    // Verify initial state
    assertTrue("Response area should contain welcome message",
        responseArea.getText().contains("Welcome to Manorrock Assistant"));
    assertEquals("Request area should be empty", "", requestArea.getText());
    assertEquals("Send button should be labeled correctly", "Send", sendButton.getText());
    assertEquals("Start Over button should be labeled correctly", "Start Over", startOverButton.getText());

    Thread.sleep(UI_DELAY * 2); // Pause to let human see the component
  }

  @Test
  public void testHelpCommand() throws InterruptedException {
    JTextArea requestArea = findComponent(component, JTextArea.class, 1);
    JTextArea responseArea = findComponent(component, JTextArea.class, 0);
    JButton sendButton = findComponent(component, JButton.class, 0);

    // Slow down the test for visibility
    Thread.sleep(UI_DELAY);
    requestArea.setText("/help");
    Thread.sleep(UI_DELAY);
    sendButton.doClick();
    Thread.sleep(UI_DELAY * 2); // Longer pause to read the help text

    assertTrue("Response should contain help message", responseArea.getText().contains("Available commands:"));
    assertTrue("Help should list llmEndpoint command", responseArea.getText().contains("/llmEndpoint"));

    Thread.sleep(UI_DELAY * 2); // Final pause to see results
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
      System.out.println("Found component: " + c.getClass().getName());

      if (type.isInstance(c)) {
        if (counter.value == 0) {
          System.out.println("Found matching component: " + c.getClass().getName());
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
