package com.manorrock.assistant.desktop;

import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

public class DesktopApplicationTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        new DesktopApplication().start(stage);
    }

    @Test
    public void testApplicationLaunches() {
        assertThat(lookup("#root").queryAs(BorderPane.class), is(notNullValue()));
    }
}
