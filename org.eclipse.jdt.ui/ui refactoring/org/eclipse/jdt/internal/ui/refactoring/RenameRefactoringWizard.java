/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jface.resource.ImageDescriptor;
public class RenameRefactoringWizard extends RefactoringWizard {
	
	private String fPageMessage;
	private String fPageContextHelpId;
	private ImageDescriptor fInputPageImageDescriptor;
	
	public RenameRefactoringWizard(IRenameRefactoring ref, String title, String message, String pageContextHelpId, String errorContextHelpId){
		super((Refactoring) ref, title, errorContextHelpId);
		fPageMessage= message;
		fPageContextHelpId= pageContextHelpId;
	}
	
	public void setInputPageImageDescriptor(ImageDescriptor desc){
		fInputPageImageDescriptor= desc;
	}
	
	protected String getPageContextHelpId() {
		return fPageContextHelpId;
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		String initialSetting= getRenameRefactoring().getCurrentName();
		setPageTitle(getPageTitle());
		createInputPage(initialSetting).setImageDescriptor(fInputPageImageDescriptor);
		createInputPage(initialSetting).setMessage(fPageMessage);
		addPage(createInputPage(initialSetting));
	}

	protected RenameInputWizardPage createInputPage(String initialSetting) {
		return new RenameInputWizardPage(fPageContextHelpId, true, initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				return validateNewName(text);
			}	
		};
	}
	
	private IRenameRefactoring getRenameRefactoring(){
		return (IRenameRefactoring)getRefactoring();	
	}
	
	protected RefactoringStatus validateNewName(String newName){
		IRenameRefactoring ref= getRenameRefactoring();
		ref.setNewName(newName);
		try{
			return ref.checkNewName(newName);
		} catch (JavaModelException e){
			//XXX: should log the exception
			String msg= e.getMessage() == null ? "": e.getMessage(); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringMessages.getFormattedString("RenameRefactoringWizard.internal_error", msg));//$NON-NLS-1$
		}	
	}
}