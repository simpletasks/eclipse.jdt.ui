package org.eclipse.jdt.internal.ui.preferences;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import org.eclipse.swt.graphics.Font;import org.eclipse.swt.graphics.FontData;import org.eclipse.jface.preference.FieldEditorPreferencePage;import org.eclipse.jface.preference.FontFieldEditor;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.preference.PreferenceConverter;import org.eclipse.jface.resource.JFaceResources;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.ui.texteditor.AbstractTextEditor;import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A preference page to set the font used in the Java editor.
 * This preference page uses the key <code>"JavaEditorPreferencePage.description"</code> 
 * to look up the page description and <code>"JavaEditorPreferencePage.fontEditor"</code>
 * for the font editor description.
 */
public class JavaEditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	/**
	 * Create the preference page.
	 */
	public JavaEditorPreferencePage() {
		super(GRID);
		
		setDescription(JavaPlugin.getResourceString("JavaEditorPreferencePage.description"));		
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
	}
	
	/*
	 * @see FieldEditorPreferencePage#createFieldEditors
	 */
	public void createFieldEditors() {
		addField(new FontFieldEditor(AbstractTextEditor.PREFERENCE_FONT, JavaPlugin.getResourceString("JavaEditorPreferencePage.fontEditor"), getFieldEditorParent()));
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
	
	public static void initDefaults(IPreferenceStore store) {
		Font font= JFaceResources.getTextFont();
		if (font != null) {
			FontData[] data= font.getFontData();
			if (data != null && data.length > 0)
				PreferenceConverter.setDefault(store, AbstractTextEditor.PREFERENCE_FONT, data[0]);
		}
	}	
}
