/*
 * Copyright (c) 2002-2025, Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright 
 *        notice, this list of conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.manorrock.assistant.eclipse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.manorrock.assistant.core.CoreAssistant;

/**
 * Provides AI-powered code completion suggestions in Eclipse's Java editor.
 */
public class AssistantCompletionProposalComputer implements IJavaCompletionProposalComputer {
    
    private CodeCompletionProvider completionProvider;
    
    public AssistantCompletionProposalComputer() {
        // Initialize completion provider directly
        completionProvider = new CodeCompletionProvider(new CoreAssistant());
    }
    
    @Override
    public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
        try {
            // Get document and offset information from the context
            ITextSelection selection = new TextSelection(context.getDocument(), context.getInvocationOffset(), 0);
            ITextEditor editor = getActiveEditor();
            
            // Use the context information to generate completion proposals
            List<ICompletionProposal> proposals = null;
			try {
				proposals = completionProvider
				    .generateCompletionSuggestions(editor, selection)
				    .get(2, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
            
            return proposals != null ? proposals : new ArrayList<>();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private ITextEditor getActiveEditor() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IEditorPart editor = page.getActiveEditor();
                if (editor instanceof ITextEditor) {
                    return (ITextEditor) editor;
                }
            }
        }
        return null;
    }
    
    @Override
    public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
        return new ArrayList<>(); // Not implemented for now
    }
    
    @Override
    public String getErrorMessage() {
        return null; // Return null when no error
    }
    
    @Override
    public void sessionStarted() {
        // Nothing to initialize
    }
    
    @Override
    public void sessionEnded() {
        // Nothing to clean up
    }
}
