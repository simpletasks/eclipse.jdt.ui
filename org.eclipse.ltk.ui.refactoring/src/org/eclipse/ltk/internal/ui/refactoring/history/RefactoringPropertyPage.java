/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.internal.ui.refactoring.history;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringPreferenceConstants;
import org.eclipse.ltk.core.refactoring.history.IRefactoringHistoryService;

import org.eclipse.ltk.internal.ui.refactoring.RefactoringUIMessages;
import org.eclipse.ltk.internal.ui.refactoring.RefactoringUIPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferencePageContainer;

import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Property page for the project refactoring history.
 * 
 * @since 3.2
 */
public final class RefactoringPropertyPage extends PropertyPage {

	/** The enable history button, or <code>null</code> */
	private Button fEnableButton= null;

	/** The initial value of the refactoring preference */
	private boolean fHasProjectHistory= false;

	/** The preferences working copy manager, or <code>null</code> */
	private IWorkingCopyManager fManager= null;

	/**
	 * {@inheritDoc}
	 */
	protected final Control createContents(final Composite parent) {
		initializeDialogUnits(parent);

		final IPreferencePageContainer container= getContainer();
		if (container instanceof IWorkbenchPreferenceContainer)
			fManager= ((IWorkbenchPreferenceContainer) container).getWorkingCopyManager();

		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= 0;
		layout.verticalSpacing= convertVerticalDLUsToPixels(10);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		result.setLayout(layout);

		Label label= new Label(result, SWT.NONE);
		label.setText(RefactoringUIMessages.RefactoringPropertyPage_label_message);

		fEnableButton= new Button(result, SWT.CHECK);
		fEnableButton.setText(RefactoringUIMessages.RefactoringPropertyPage_enable_message);
		fEnableButton.setData(RefactoringPreferenceConstants.PREFERENCE_ENABLE_PROJECT_REFACTORING_HISTORY);

		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fEnableButton.setLayoutData(data);

		fEnableButton.setSelection(hasProjectHistory());
		fEnableButton.setEnabled(RefactoringCore.internalGetPreferences().getBoolean(RefactoringPreferenceConstants.PREFERENCE_ENABLE_WORKSPACE_REFACTORING_HISTORY));

		applyDialogFont(result);
		return result;
	}

	/**
	 * Returns the preferences for the specified context.
	 * 
	 * @param manager
	 *            the working copy manager
	 * @param context
	 *            the scope context
	 * @return the preferences
	 */
	private IEclipsePreferences getPreferences(final IWorkingCopyManager manager, final IScopeContext context) {
		IEclipsePreferences preferences= context.getNode(RefactoringCore.ID_PLUGIN);
		if (manager != null)
			return manager.getWorkingCopy(preferences);
		return preferences;
	}

	/**
	 * Returns whether a project has an explicit refactoring history.
	 * 
	 * @return <code>true</code> if the project contains an explicit project
	 *         history, <code>false</code> otherwise
	 */
	private boolean hasProjectHistory() {
		final IProject project= (IProject) getElement().getAdapter(IProject.class);
		if (project != null)
			return RefactoringCore.getRefactoringHistoryService().hasProjectHistory(project);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void performDefaults() {
		super.performDefaults();
		final IProject project= (IProject) getElement().getAdapter(IProject.class);
		if (project != null)
			setPreference(fManager, new ProjectScope(project), RefactoringPreferenceConstants.PREFERENCE_ENABLE_PROJECT_REFACTORING_HISTORY, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean performOk() {
		final IProject project= (IProject) getElement().getAdapter(IProject.class);
		if (project != null)
			setPreference(fManager, new ProjectScope(project), RefactoringPreferenceConstants.PREFERENCE_ENABLE_PROJECT_REFACTORING_HISTORY, Boolean.valueOf(fEnableButton.getSelection()).toString());
		if (fManager != null)
			try {
				fManager.applyChanges();
				final IRefactoringHistoryService service= RefactoringCore.getRefactoringHistoryService();
				final boolean history= service.hasProjectHistory(project);
				if (history != fHasProjectHistory && project != null)
					service.setProjectHistory(project, history);
			} catch (BackingStoreException exception) {
				RefactoringUIPlugin.log(exception);
			} catch (CoreException exception) {
				RefactoringUIPlugin.log(exception);
			}
		return super.performOk();
	}

	/**
	 * Sets the preference for a certain context.
	 * 
	 * @param manager
	 *            the working copy manager
	 * @param context
	 *            the scope context
	 * @param key
	 *            the key of the preference
	 * @param value
	 *            the value of the preference
	 */
	private void setPreference(final IWorkingCopyManager manager, final IScopeContext context, final String key, final String value) {
		if (value != null)
			getPreferences(manager, context).put(key, value);
		else
			getPreferences(manager, context).remove(key);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setVisible(final boolean visible) {
		fHasProjectHistory= hasProjectHistory();
		super.setVisible(visible);
	}
}