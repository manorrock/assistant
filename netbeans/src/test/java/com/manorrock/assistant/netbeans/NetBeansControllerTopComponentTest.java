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

public class NetBeansControllerTopComponentTest {

  private static boolean isHeadless;
  private static final int UI_DELAY = 1000; // 1 second delay for human visibility
  private NetBeansControllerTopComponent component;
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
  public void setUp() {
    if (isHeadless) {
      component = new NetBeansControllerTopComponent();
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
    if (isHeadless) {
      Command helpCommand = CommandRegistry.getInstance().getCommand("help");
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
