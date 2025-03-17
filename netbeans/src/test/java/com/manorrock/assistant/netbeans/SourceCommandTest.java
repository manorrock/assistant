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
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.nio.file.Path;

public class SourceCommandTest {
  private static boolean isHeadless;
  private static final int UI_DELAY = 1000;
  private NetBeansControllerTopComponent component;
  private JFrame frame;

  @BeforeClass
  public static void setUpClass() {
    System.setProperty("netbeans.running.environment", "junit-test");
    isHeadless = GraphicsEnvironment.isHeadless();
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
        frame = new JFrame("Source Command Test");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(component);
        frame.setVisible(true);
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
    }
    component = null;
  }

  @Test
  public void testSourceCommand() throws Exception {
    if (isHeadless) {
      return;
    }

    System.out.println("Component hierarchy:");
    printComponentHierarchy(component, "");

    Path promptFile = Path.of("src/test/prompts/help.prompt");
    JTextArea requestArea = findComponent(component, JTextArea.class, 1);
    JTextArea responseArea = findComponent(component, JTextArea.class, 0);
    JButton sendButton = findComponent(component, JButton.class, 0);

    Thread.sleep(UI_DELAY);
    requestArea.setText("/source " + promptFile);
    Thread.sleep(UI_DELAY);
    sendButton.doClick();
    Thread.sleep(UI_DELAY * 2);

    String response = responseArea.getText();
    System.out.println("Actual response: " + response);

    assertTrue("Response should contain help message", response.contains("Available commands:"));
    assertTrue("Response should contain command information", response.contains("/help"));
  }

  @Test
  public void testSourceCommandWithNonExistentFile() throws Exception {
    if (isHeadless) {
      return;
    }

    JTextArea requestArea = findComponent(component, JTextArea.class, 1);
    JTextArea responseArea = findComponent(component, JTextArea.class, 0);
    JButton sendButton = findComponent(component, JButton.class, 0);

    Thread.sleep(UI_DELAY);
    requestArea.setText("/source nonexistent.file");
    Thread.sleep(UI_DELAY);
    sendButton.doClick();
    Thread.sleep(UI_DELAY * 2);

    String response = responseArea.getText();
    assertTrue("Response should contain error message", response.contains("Error reading source file"));
  }

  private <T extends Component> T findComponent(Container parent, Class<T> type, int index) {
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

  // Add debugging helper method
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
