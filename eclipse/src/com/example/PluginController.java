package com.example;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;

public class PluginController {
    private Text responseArea;
    private Text requestArea;
    private Text logArea;
    private ProgressBar progressBar;

    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite leftComposite = new Composite(sashForm, SWT.NONE);
        leftComposite.setLayout(new GridLayout(1, false));

        responseArea = new Text(leftComposite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        responseArea.setEditable(false);
        responseArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        requestArea = new Text(leftComposite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        requestArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        requestArea.setMessage("Type your message here...");

        Composite buttonComposite = new Composite(leftComposite, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button sendButton = new Button(buttonComposite, SWT.PUSH);
        sendButton.setText("Send");
        sendButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSendAction();
            }
        });

        Button startOverButton = new Button(buttonComposite, SWT.PUSH);
        startOverButton.setText("Start Over");
        startOverButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        startOverButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleStartOverAction();
            }
        });

        Composite rightComposite = new Composite(sashForm, SWT.NONE);
        rightComposite.setLayout(new GridLayout(1, false));

        logArea = new Text(rightComposite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        logArea.setEditable(false);
        logArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite dropZone = new Composite(rightComposite, SWT.BORDER);
        dropZone.setLayout(new GridLayout(1, false));
        dropZone.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Label dropLabel = new Label(dropZone, SWT.NONE);
        dropLabel.setText("Drop files here");

        sashForm.setWeights(new int[] { 70, 30 });

        progressBar = new ProgressBar(parent, SWT.NONE);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void handleSendAction() {
        // Implement send action logic
    }

    private void handleStartOverAction() {
        // Implement start over action logic
    }
}
