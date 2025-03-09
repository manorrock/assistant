package com.manorrock.assistant.eclipse.perspectives;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;

import com.manorrock.assistant.eclipse.views.AssistantView;

/**
 * A perspective listener that ensures the Assistant view is opened
 * whenever a perspective is activated or opened.
 */
public class PerspectiveListener extends PerspectiveAdapter {
    
    private IWorkbenchWindow window;
    
    public PerspectiveListener(IWorkbenchWindow window) {
        this.window = window;
    }
    
    @Override
    public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
        openAssistantView(page);
    }
    
    @Override
    public void perspectiveOpened(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
        openAssistantView(page);
    }
    
    private void openAssistantView(IWorkbenchPage page) {
        try {
            if (page.findView(AssistantView.ID) == null) {
                page.showView(AssistantView.ID);
            }
        } catch (PartInitException e) {
            // Log error
            e.printStackTrace();
        }
    }
}
