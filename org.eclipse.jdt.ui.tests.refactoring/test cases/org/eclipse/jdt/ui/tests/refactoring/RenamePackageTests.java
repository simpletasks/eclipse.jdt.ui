/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.ui.tests.refactoring.infra.DebugUtils;
import org.eclipse.jdt.ui.tests.refactoring.infra.ZipTools;


public class RenamePackageTests extends RefactoringTest {
	private static final boolean BUG_6054= true;
	private static final boolean BUG_54962_71267= false;
	
	private static final Class clazz= RenamePackageTests.class;
	private static final String REFACTORING_PATH= "RenamePackage/";
	
	private boolean fUpdateReferences;
	private boolean fUpdateTextualMatches;
	private String fQualifiedNamesFilePatterns;
	private boolean fRenameSubpackages;
	
	public RenamePackageTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java15Setup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java15Setup(someTest);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		fUpdateReferences= true;
		fUpdateTextualMatches= false;
		fQualifiedNamesFilePatterns= null;
		fRenameSubpackages= false;
		// fIsPreDeltaTest= true;
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	// -------------
	private RenameJavaElementDescriptor createRefactoringDescriptor(IPackageFragment pack, String newName) throws CoreException {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		descriptor.setJavaElement(pack);
		descriptor.setNewName(newName);
		descriptor.setUpdateReferences(true);
		return descriptor;
	}

	/* non java-doc
	 * the 0th one is the one to rename
	 */
	private void helper1(String packageNames[], String[][] packageFiles, String newPackageName) throws Exception{
		try{
			IPackageFragment[] packages= new IPackageFragment[packageNames.length];
			for (int i= 0; i < packageFiles.length; i++){
				packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
				for (int j= 0; j < packageFiles[i].length; j++){
					createCUfromTestFile(packages[i], packageFiles[i][j], packageNames[i].replace('.', '/') + "/");
					//DebugUtils.dump(cu.getElementName() + "\n" + cu.getSource());
				}	
			}
			IPackageFragment thisPackage= packages[0];
			RefactoringStatus result= performRefactoring(createRefactoringDescriptor(thisPackage, newPackageName));
			assertNotNull("precondition was supposed to fail", result);
			if (fIsVerbose)
				DebugUtils.dump("" + result);
		} finally{		
			performDummySearch();
		
			for (int i= 0; i < packageNames.length; i++){
				IPackageFragment pack= getRoot().getPackageFragment(packageNames[i]);
				if (pack.exists())
					pack.delete(true, null);
			}	
		}	
	}
	
	/* non java-doc
	 * the 0th one is the one to rename
	 */
	private void helper1(String[] packageNames, String newPackageName) throws Exception{
		try{
			IPackageFragment[] packages= new IPackageFragment[packageNames.length];
			for (int i= 0; i < packageNames.length; i++){
				packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
			}
			IPackageFragment thisPackage= packages[0];
			RefactoringStatus result= performRefactoring(createRefactoringDescriptor(thisPackage, newPackageName));
			assertNotNull("precondition was supposed to fail", result);
			if (fIsVerbose)
				DebugUtils.dump("" + result);
		} finally{		
			performDummySearch();	
			
			for (int i= 0; i < packageNames.length; i++){
				IPackageFragment pack= getRoot().getPackageFragment(packageNames[i]);
				if (pack.exists())
					pack.delete(true, null);
			}		
		}	
	}
	
	private void helper1() throws Exception{
		helper1(new String[]{"r"}, new String[][]{{"A"}}, "p1");
	}
	
	private void helper2(String[] packageNames, String[][] packageFileNames, String newPackageName) throws Exception{
			ParticipantTesting.reset();
			IPackageFragment[] packages= new IPackageFragment[packageNames.length];
			ICompilationUnit[][] cus= new ICompilationUnit[packageFileNames.length][packageFileNames[0].length];
			for (int i= 0; i < packageNames.length; i++){
				packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
				for (int j= 0; j < packageFileNames[i].length; j++){
					cus[i][j]= createCUfromTestFile(packages[i], packageFileNames[i][j], packageNames[i].replace('.', '/') + "/");
				}
			}
			IPackageFragment thisPackage= packages[0];
			boolean hasSubpackages= thisPackage.hasSubpackages();
			
			IPath path= thisPackage.getParent().getPath();
			path= path.append(newPackageName.replace('.', '/'));
			IFolder target= ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
			boolean targetExists= target.exists();
			boolean isRename= !targetExists && !thisPackage.hasSubpackages() && thisPackage.getResource().getParent().equals(target.getParent());
			
			String[] createHandles= null;
			String[] moveHandles= null;
			String[] deleteHandles= null;
			boolean doDelete= true;
			
			String[] renameHandles= null;
			if (isRename) {
				renameHandles= ParticipantTesting.createHandles(thisPackage, thisPackage.getResource());
			} else {
				renameHandles= ParticipantTesting.createHandles(thisPackage);
				IContainer loop= target;
				List handles= new ArrayList();
				while (loop != null && !loop.exists()) {
					handles.add(ParticipantTesting.createHandles(loop)[0]);
					loop= loop.getParent();
				}
				createHandles= (String[]) handles.toArray(new String[handles.size()]);
				IFolder source= (IFolder)thisPackage.getResource();
				deleteHandles= ParticipantTesting.createHandles(source);
				IResource members[]= source.members();
				List movedObjects= new ArrayList();
				for (int i= 0; i < members.length; i++) {
					if (members[i] instanceof IFolder) {
						doDelete= false;
					} else {
						movedObjects.add(members[i]);
					}
				}
				moveHandles= ParticipantTesting.createHandles(movedObjects.toArray());
			}
			RenameJavaElementDescriptor descriptor= createRefactoringDescriptor(thisPackage, newPackageName);
			descriptor.setUpdateReferences(fUpdateReferences);
			descriptor.setUpdateTextualOccurrences(fUpdateTextualMatches);
			setFilePatterns(descriptor);
			RefactoringStatus result= performRefactoring(descriptor);
			assertEquals("preconditions were supposed to pass", null, result);
			
			if (isRename) {
				ParticipantTesting.testRename(renameHandles,
					new RenameArguments[] {
						new RenameArguments(newPackageName, fUpdateReferences),
						new RenameArguments(target.getName(), fUpdateReferences)
					}
				);
			} else {
				ParticipantTesting.testRename(renameHandles,
					new RenameArguments[] {
					new RenameArguments(newPackageName, fUpdateReferences)});
				
				ParticipantTesting.testCreate(createHandles);
				
				List args= new ArrayList();
				for (int i= 0; i < packageFileNames[0].length; i++) {
					args.add(new MoveArguments(target, fUpdateReferences));
				}
				ParticipantTesting.testMove(moveHandles, (MoveArguments[]) args.toArray(new MoveArguments[args.size()]));
				
				if (doDelete) {
					ParticipantTesting.testDelete(deleteHandles);
				} else {
					ParticipantTesting.testDelete(new String[0]);
				}
			}
			
			//---
			
			if (hasSubpackages) {
				assertTrue("old package does not exist anymore", getRoot().getPackageFragment(packageNames[0]).exists());
			} else {
				assertTrue("package not renamed", ! getRoot().getPackageFragment(packageNames[0]).exists());
			}
			IPackageFragment newPackage= getRoot().getPackageFragment(newPackageName);
			assertTrue("new package does not exist", newPackage.exists());
			
			for (int i= 0; i < packageFileNames.length; i++){
				String packageName= (i == 0) 
								? newPackageName.replace('.', '/') + "/"
								: packageNames[i].replace('.', '/') + "/";
				for (int j= 0; j < packageFileNames[i].length; j++){
					String s1= getFileContents(getOutputTestFileName(packageFileNames[i][j], packageName));
					ICompilationUnit cu= 
						(i == 0) 
							? newPackage.getCompilationUnit(packageFileNames[i][j] + ".java")
							: cus[i][j];
					//DebugUtils.dump("cu:" + cu.getElementName());		
					String s2= cu.getSource();
					
					//DebugUtils.dump("expected:" + s1);
					//DebugUtils.dump("was:" + s2);
					assertEqualLines("invalid update in file " + cu.getElementName(), s1,	s2);
				}
			}
	}
	
	class PackageRename {
		final String[] fPackageNames;
		final String[][] fPackageFileNames;
		final String fNewPackageName;

		final IPackageFragment[] fPackages;
		final ICompilationUnit[][] fCus;

		public PackageRename (String[] packageNames, String[][] packageFileNames, String newPackageName) throws Exception {
			fPackageNames= packageNames;
			fPackageFileNames= packageFileNames;
			fNewPackageName= newPackageName;
			
			fPackages= new IPackageFragment[packageNames.length];
			fCus= new ICompilationUnit[packageFileNames.length][];
			for (int i= 0; i < packageNames.length; i++){
				fPackages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
				fCus[i]= new ICompilationUnit[packageFileNames[i].length];
				for (int j= 0; j < packageFileNames[i].length; j++){
					fCus[i][j]= createCUfromTestFile(fPackages[i], packageFileNames[i][j], packageNames[i].replace('.', '/') + "/");
				}
			}
		}
		
		public void execute() throws Exception {
			IPackageFragment thisPackage= fPackages[0];
			RenameJavaElementDescriptor descriptor= createRefactoringDescriptor(thisPackage, fNewPackageName);
			descriptor.setUpdateReferences(fUpdateReferences);
			descriptor.setUpdateTextualOccurrences(fUpdateTextualMatches);
			setFilePatterns(descriptor);
			descriptor.setUpdateHierarchy(fRenameSubpackages);
			RefactoringStatus result= performRefactoring(descriptor);
			assertEquals("preconditions were supposed to pass", null, result);
			
			assertTrue("package not renamed: " + fPackageNames[0], ! getRoot().getPackageFragment(fPackageNames[0]).exists());
			IPackageFragment newPackage= getRoot().getPackageFragment(fNewPackageName);
			assertTrue("new package does not exist", newPackage.exists());
			
			for (int i= 0; i < fPackageFileNames.length; i++){
				String packageName= getNewPackageName(fPackageNames[i]);
				String packagePath= packageName.replace('.', '/') + "/";
				
				for (int j= 0; j < fPackageFileNames[i].length; j++){
					String expected= getFileContents(getOutputTestFileName(fPackageFileNames[i][j], packagePath));
					ICompilationUnit cu= getRoot().getPackageFragment(packageName).getCompilationUnit(fPackageFileNames[i][j] + ".java");
					String actual= cu.getSource();
					assertEqualLines("invalid update in file " + cu.getElementName(), expected,	actual);
				}
			}
		}

		public String getNewPackageName(String oldPackageName) {
			if (oldPackageName.equals(fPackageNames[0]))
				return fNewPackageName;
			
			if (fRenameSubpackages && oldPackageName.startsWith(fPackageNames[0] + "."))
				return fNewPackageName + oldPackageName.substring(fPackageNames[0].length());
			
			return oldPackageName;
		}
	}
	
	/**
	 * Custom project and source folder structure.
	 * @param roots source folders
	 * @param packageNames package names per root
	 * @param newPackageName the new package name for packageNames[0][0]
	 * @param cuNames cu names per package
	 * @throws Exception
	 */
	private void helperMultiProjects(IPackageFragmentRoot[] roots, String[][] packageNames, String newPackageName, String[][][] cuNames) throws Exception{
		ICompilationUnit[][][] cus=new ICompilationUnit[roots.length][][]; 
		IPackageFragment thisPackage= null;

		for (int r= 0; r < roots.length; r++) {
			IPackageFragment[] packages= new IPackageFragment[packageNames[r].length];
			cus[r]= new ICompilationUnit[packageNames[r].length][];
			for (int pa= 0; pa < packageNames[r].length; pa++){
				packages[pa]= roots[r].createPackageFragment(packageNames[r][pa], true, null);
				cus[r][pa]= new ICompilationUnit[cuNames[r][pa].length];
				if (r == 0 && pa == 0)
					thisPackage= packages[pa];
				for (int typ= 0; typ < cuNames[r][pa].length; typ++){
					cus[r][pa][typ]= createCUfromTestFile(packages[pa], cuNames[r][pa][typ],
							roots[r].getElementName() + "/" + packageNames[r][pa].replace('.', '/') + "/");
				}
			}
		}
		
		RenameJavaElementDescriptor descriptor= createRefactoringDescriptor(thisPackage, newPackageName);
		descriptor.setUpdateReferences(fUpdateReferences);
		descriptor.setUpdateTextualOccurrences(fUpdateTextualMatches);
		setFilePatterns(descriptor);
		descriptor.setUpdateHierarchy(fRenameSubpackages);
		RefactoringStatus result= performRefactoring(descriptor);
		assertEquals("preconditions were supposed to pass", null, result);
		
		assertTrue("package not renamed", ! roots[0].getPackageFragment(packageNames[0][0]).exists());
		IPackageFragment newPackage= roots[0].getPackageFragment(newPackageName);
		assertTrue("new package does not exist", newPackage.exists());
		
		for (int r = 0; r < cuNames.length; r++) {
			for (int pa= 0; pa < cuNames[r].length; pa++){
				String packageName= roots[r].getElementName() + "/"	+ 
						((r == 0 && pa == 0) ? newPackageName : packageNames[r][pa]).replace('.', '/') + "/";
				for (int typ= 0; typ < cuNames[r][pa].length; typ++){
					String s1= getFileContents(getOutputTestFileName(cuNames[r][pa][typ], packageName));
					ICompilationUnit cu= (r == 0 && pa == 0)
							? newPackage.getCompilationUnit(cuNames[r][pa][typ] + ".java")
							: cus[r][pa][typ];
					//DebugUtils.dump("cu:" + cu.getElementName());		
					String s2= cu.getSource();
					
					//DebugUtils.dump("expected:" + s1);
					//DebugUtils.dump("was:" + s2);
					assertEqualLines("invalid update in file " + cu.toString(), s1,	s2);
				}
			}
		}
	}
		
	/**
	 * 2 Projects with a root each:
	 * Project RenamePack2 (root: srcTest) requires project RenamePack1 (root: srcPrg).
	 * @param packageNames package names per root
	 * @param newPackageName the new package name for packageNames[0][0]
	 * @param cuNames cu names per package
	 * @throws Exception
	 */
	private void helperProjectsPrgTest(String[][] packageNames, String newPackageName, String[][][] cuNames) throws Exception{
		IJavaProject projectPrg= null;
		IJavaProject projectTest= null;
		try {
			projectPrg= JavaProjectHelper.createJavaProject("RenamePack1", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectPrg));
			IPackageFragmentRoot srcPrg= JavaProjectHelper.addSourceContainer(projectPrg, "srcPrg");
			Map optionsPrg= projectPrg.getOptions(false);
			JavaProjectHelper.set15CompilerOptions(optionsPrg);
			projectPrg.setOptions(optionsPrg);

			projectTest= JavaProjectHelper.createJavaProject("RenamePack2", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectTest));
			IPackageFragmentRoot srcTest= JavaProjectHelper.addSourceContainer(projectTest, "srcTest");
			Map optionsTest= projectTest.getOptions(false);
			JavaProjectHelper.set15CompilerOptions(optionsTest);
			projectTest.setOptions(optionsTest);

			JavaProjectHelper.addRequiredProject(projectTest, projectPrg);

			helperMultiProjects(new IPackageFragmentRoot[] { srcPrg, srcTest }, packageNames, newPackageName, cuNames);
		} finally {
			JavaProjectHelper.delete(projectPrg);
			JavaProjectHelper.delete(projectTest);
		}
	}
	
	/*
	 * Multiple source folders in the same project.
	 * @param newPackageName the new package name for packageNames[0][0]
	 */
	private void helperMultiRoots(String[] rootNames, String[][] packageNames, String newPackageName, String[][][] typeNames) throws Exception{
		IPackageFragmentRoot[] roots= new IPackageFragmentRoot[rootNames.length];
		try {
			for (int r= 0; r < roots.length; r++)
				roots[r]= JavaProjectHelper.addSourceContainer(getRoot().getJavaProject(), rootNames[r]);
			helperMultiProjects(roots, packageNames, newPackageName, typeNames);
		} catch (CoreException e) {
		}
		for (int r= 0; r < roots.length; r++)
			JavaProjectHelper.removeSourceContainer(getRoot().getJavaProject(), rootNames[r]);
	}
	
	private void setFilePatterns(RenameJavaElementDescriptor descriptor) {
		descriptor.setUpdateQualifiedNames(fQualifiedNamesFilePatterns != null);
		if (fQualifiedNamesFilePatterns != null)
			descriptor.setFileNamePatterns(fQualifiedNamesFilePatterns);
	}

	// ---------- tests -------------	
	public void testHierarchical01() throws Exception {
		fRenameSubpackages= true;
		
		PackageRename rename= new PackageRename(new String[]{"my", "my.a", "my.b"}, new String[][]{{"MyA"},{"ATest"},{"B"}}, "your");
		IPackageFragment thisPackage= rename.fPackages[0];
		
		ParticipantTesting.reset();
		List toRename= new ArrayList(Arrays.asList(JavaElementUtil.getPackageAndSubpackages(thisPackage)));
		toRename.add(thisPackage.getResource());
		String[] renameHandles= ParticipantTesting.createHandles(toRename.toArray());
		
		rename.execute();
		
		ParticipantTesting.testRename(renameHandles, new RenameArguments[] {
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[0]), true),
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[1]), true),
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[2]), true),
				new RenameArguments("your", true)
		});
	}
	
	public void testHierarchical02() throws Exception {
		if (true) {
			printTestDisabledMessage("package can't be renamed to a package that already exists.");
			return;
		}
		fRenameSubpackages= true;
		
		PackageRename rename= new PackageRename(new String[]{"my", "my.a", "my.b", "your"}, new String[][]{{"MyA"},{"ATest"},{"B"}, {"Y"}}, "your");
		IPackageFragment thisPackage= rename.fPackages[0];
		IPath srcPath= thisPackage.getParent().getPath();
		IFolder target= ResourcesPlugin.getWorkspace().getRoot().getFolder(srcPath.append("your"));
		
		ParticipantTesting.reset();
		String[] createHandles= ParticipantTesting.createHandles(target.getFolder("a"), target.getFolder("b"));
		String[] deleteHandles= ParticipantTesting.createHandles(thisPackage.getResource());
		String[] moveHandles= ParticipantTesting.createHandles(new Object[] {
				rename.fCus[0][0].getResource(),
				rename.fCus[1][0].getResource(),
				rename.fCus[2][0].getResource(),
		});
		String[] renameHandles= ParticipantTesting.createHandles(JavaElementUtil.getPackageAndSubpackages(thisPackage));
		
		rename.execute();
		
		ParticipantTesting.testCreate(createHandles);
		ParticipantTesting.testDelete(deleteHandles);
		ParticipantTesting.testMove(moveHandles, new MoveArguments[] {
				new MoveArguments(target, true),
				new MoveArguments(target.getFolder("a"), true),
				new MoveArguments(target.getFolder("b"), true),
		});
		ParticipantTesting.testRename(renameHandles, new RenameArguments[] {
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[0]), true),
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[1]), true),
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[2]), true),
		});
	}
	
	public void testHierarchical03() throws Exception {
		fRenameSubpackages= true;
		fUpdateTextualMatches= true;
		
		PackageRename rename= new PackageRename(new String[]{"my", "my.pack"}, new String[][]{{},{"C"}}, "your");
		IPackageFragment thisPackage= rename.fPackages[0];
		
		ParticipantTesting.reset();
		
		List toRename= new ArrayList(Arrays.asList(JavaElementUtil.getPackageAndSubpackages(thisPackage)));
		toRename.add(thisPackage.getResource());
		String[] renameHandles= ParticipantTesting.createHandles(toRename.toArray());
		
		rename.execute();
		
		ParticipantTesting.testRename(renameHandles, new RenameArguments[] {
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[0]), true),
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[1]), true),
				new RenameArguments("your", true)
		});
	}
	
	public void testHierarchicalDisabledImport() throws Exception {
		fRenameSubpackages= true;
		fUpdateTextualMatches= true;
		
		PackageRename rename= new PackageRename(new String[]{"my", "my.pack"}, new String[][]{{},{"C"}}, "your");
		IPackageFragment thisPackage= rename.fPackages[0];
		
		ParticipantTesting.reset();
		
		List toRename= new ArrayList(Arrays.asList(JavaElementUtil.getPackageAndSubpackages(thisPackage)));
		toRename.add(thisPackage.getResource());
		String[] renameHandles= ParticipantTesting.createHandles(toRename.toArray());
		
		rename.execute();
		
		ParticipantTesting.testRename(renameHandles, new RenameArguments[] {
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[0]), true),
				new RenameArguments(rename.getNewPackageName(rename.fPackageNames[1]), true),
				new RenameArguments("your", true)
		});
	}
	
	public void testHierarchicalJUnit() throws Exception {
		fRenameSubpackages= true;
		
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		Assert.assertTrue(junitSrcArchive != null && junitSrcArchive.exists());
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainerWithImport(getRoot().getJavaProject(), "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		
		String[] packageNames= new String[]{"junit", "junit.extensions", "junit.framework", "junit.runner", "junit.samples", "junit.samples.money", "junit.tests", "junit.tests.extensions", "junit.tests.framework", "junit.tests.runner", "junit.textui"};
		ICompilationUnit[][] cus= new ICompilationUnit[packageNames.length][];
		for (int i= 0; i < cus.length; i++) {
			cus[i]= src.getPackageFragment(packageNames[i]).getCompilationUnits();
		}
		IPackageFragment thisPackage= src.getPackageFragment("junit");
		
		ParticipantTesting.reset();
		PackageRename rename= new PackageRename(packageNames, new String[packageNames.length][0],"jdiverge");
		
		RenameArguments[] renameArguments= new RenameArguments[packageNames.length + 1];
		for (int i= 0; i < packageNames.length; i++) {
			renameArguments[i]= new RenameArguments(rename.getNewPackageName(packageNames[i]), true);
		}
		renameArguments[packageNames.length]= new RenameArguments("jdiverge", true);
		String[] renameHandles= new String[packageNames.length + 1]; 
		System.arraycopy(ParticipantTesting.createHandles(JavaElementUtil.getPackageAndSubpackages(thisPackage)), 0, renameHandles, 0, packageNames.length);
		renameHandles[packageNames.length]= ParticipantTesting.createHandles(thisPackage.getResource())[0];
		
		// --- execute:
		RenameJavaElementDescriptor descriptor= createRefactoringDescriptor(thisPackage, "jdiverge");
		descriptor.setUpdateReferences(fUpdateReferences);
		descriptor.setUpdateTextualOccurrences(fUpdateTextualMatches);
		setFilePatterns(descriptor);
		descriptor.setUpdateHierarchy(fRenameSubpackages);
		Refactoring ref= createRefactoring(descriptor);

		performDummySearch();
		IUndoManager undoManager= getUndoManager();
		CreateChangeOperation create= new CreateChangeOperation(
			new CheckConditionsOperation(ref, CheckConditionsOperation.ALL_CONDITIONS),
			RefactoringStatus.FATAL);
		PerformChangeOperation perform= new PerformChangeOperation(create);
		perform.setUndoManager(undoManager, ref.getName());
		ResourcesPlugin.getWorkspace().run(perform, new NullProgressMonitor());
		RefactoringStatus status= create.getConditionCheckingStatus();
		assertTrue("Change wasn't executed", perform.changeExecuted());
		Change undo= perform.getUndoChange();
		assertNotNull("Undo doesn't exist", undo);
		assertTrue("Undo manager is empty", undoManager.anythingToUndo());
		
		assertFalse(status.hasError());
		assertTrue(status.hasWarning());
		RefactoringStatusEntry[] statusEntries= status.getEntries();
		for (int i= 0; i < statusEntries.length; i++) {
			RefactoringStatusEntry entry= statusEntries[i];
			assertTrue(entry.isWarning());
			assertTrue(entry.getCode() == RefactoringStatusCodes.MAIN_METHOD);
		}
		
		assertTrue("package not renamed: " + rename.fPackageNames[0], ! src.getPackageFragment(rename.fPackageNames[0]).exists());
		IPackageFragment newPackage= src.getPackageFragment(rename.fNewPackageName);
		assertTrue("new package does not exist", newPackage.exists());
		// ---
		
		ParticipantTesting.testRename(renameHandles, renameArguments);
		
		PerformChangeOperation performUndo= new PerformChangeOperation(undo);
		ResourcesPlugin.getWorkspace().run(performUndo, new NullProgressMonitor());
		
		assertTrue("new package still exists", ! newPackage.exists());
		assertTrue("original package does not exist: " + rename.fPackageNames[0], src.getPackageFragment(rename.fPackageNames[0]).exists());
		
		ZipInputStream zis= new ZipInputStream(new BufferedInputStream(new FileInputStream(junitSrcArchive)));
		ZipTools.compareWithZipped(src, zis, JavaProjectHelper.JUNIT_SRC_ENCODING);
	}
	
	public void testFail0() throws Exception{
		helper1(new String[]{"r"}, new String[][]{{"A"}}, "9");
	}
	
	public void testFail1() throws Exception{
		printTestDisabledMessage("needs revisiting");
		//helper1(new String[]{"r.p1"}, new String[][]{{"A"}}, "r");
	}
	
	public void testFail2() throws Exception{
		helper1(new String[]{"r.p1", "fred"}, "fred");
	}	
	
	public void testFail3() throws Exception{
		helper1(new String[]{"r"}, new String[][]{{"A"}}, "fred");
	}
	
	public void testFail4() throws Exception{
		helper1();
	}
	
	public void testFail5() throws Exception{
		helper1();
	}
	
	public void testFail6() throws Exception{
		helper1();
	}
	
	public void testFail7() throws Exception{
		//printTestDisabledMessage("1GK90H4: ITPJCORE:WIN2000 - search: missing package reference");
		printTestDisabledMessage("corner case - name obscuring");
//		helper1(new String[]{"r", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	public void testFail8() throws Exception{
		printTestDisabledMessage("corner case - name obscuring");
//		helper1(new String[]{"r", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	//native method used r.A as a parameter
	public void testFail9() throws Exception{
		printTestDisabledMessage("corner case - qualified name used  as a parameter of a native method");
		//helper1(new String[]{"r", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	public void testFail10() throws Exception{
		helper1(new String[]{"r.p1", "r"}, new String[][]{{"A"}, {"A"}}, "r");
	}

	public void testFail11() throws Exception{
		helper1(new String[]{"q.p1", "q", "r.p1"}, new String[][]{{"A"}, {"A"}, {}}, "r.p1");
	}
	
	//-------
	public void test0() throws Exception{
		if (BUG_54962_71267) {
			printTestDisabledMessage("bugs 54962, 71267");
			return;
		}
		fIsPreDeltaTest= true;
		helper2(new String[]{"r"}, new String[][]{{"A"}}, "p1");
	}
	
	public void test1() throws Exception{
		fIsPreDeltaTest= true;
		helper2(new String[]{"r"}, new String[][]{{"A"}}, "p1");
	}
	
	public void test2() throws Exception{
		fIsPreDeltaTest= true;
		helper2(new String[]{"r", "fred"}, new String[][]{{"A"}, {"A"}}, "p1");
	}
	
	public void test3() throws Exception{
		fIsPreDeltaTest= true;
		helper2(new String[]{"fred", "r.r"}, new String[][]{{"A"}, {"B"}}, "r");
	}
	
	public void test4() throws Exception{
		fIsPreDeltaTest= true;
		
		fQualifiedNamesFilePatterns= "*.txt";
		String textFileName= "Textfile.txt";
		
		String textfileContent= getFileContents(getTestPath() + getName() + TEST_INPUT_INFIX + textFileName);
		IFile textfile= getRoot().getJavaProject().getProject().getFile(textFileName);
		textfile.create(new ByteArrayInputStream(textfileContent.getBytes()), true, null);
		
		helper2(new String[]{"r.p1", "r"}, new String[][]{{"A"}, {"A"}}, "q");
		
		InputStreamReader reader= new InputStreamReader(textfile.getContents(true));
		StringBuffer newContent= new StringBuffer();
		try {
			int ch;
			while((ch= reader.read()) != -1)
				newContent.append((char)ch);
		} finally {
			reader.close();
		}
		String definedContent= getFileContents(getTestPath() + getName() + TEST_OUTPUT_INFIX + textFileName);
		assertEqualLines("invalid updating", definedContent, newContent.toString());
	}
	
	public void test5() throws Exception{
		fUpdateReferences= false;
		fIsPreDeltaTest= true;
		helper2(new String[]{"r"}, new String[][]{{"A"}}, "p1");
	}
	
	public void test6() throws Exception{ //bug 66250
		fUpdateReferences= false;
		fUpdateTextualMatches= true;
		fIsPreDeltaTest= true;
		helper2(new String[]{"r"}, new String[][]{{"A"}}, "p1");
	}
	
	public void test7() throws Exception{
		helper2(new String[]{"r", "r.s"}, new String[][]{{"A"}, {"B"}}, "q");
	}
	
	public void testReadOnly() throws Exception{
		if (BUG_6054) {
			printTestDisabledMessage("see bug#6054 (renaming a read-only package resets the read-only flag)");
			return;
		}
		
		fIsPreDeltaTest= true;
		String[] packageNames= new String[]{"r"};
		String[][] packageFileNames= new String[][]{{"A"}};
		String newPackageName= "p1";
		IPackageFragment[] packages= new IPackageFragment[packageNames.length];

		ICompilationUnit[][] cus= new ICompilationUnit[packageFileNames.length][packageFileNames[0].length];
		for (int i= 0; i < packageNames.length; i++){
			packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
			for (int j= 0; j < packageFileNames[i].length; j++){
				cus[i][j]= createCUfromTestFile(packages[i], packageFileNames[i][j], packageNames[i].replace('.', '/') + "/");
			}
		}
		IPackageFragment thisPackage= packages[0];
		final IResource resource= thisPackage.getCorrespondingResource();
		final ResourceAttributes attributes= resource.getResourceAttributes();
		if (attributes != null)
			attributes.setReadOnly(true);
		RefactoringStatus result= performRefactoring(createRefactoringDescriptor(thisPackage, newPackageName));
		assertEquals("preconditions were supposed to pass", null, result);
		
		assertTrue("package not renamed", ! getRoot().getPackageFragment(packageNames[0]).exists());
		IPackageFragment newPackage= getRoot().getPackageFragment(newPackageName);
		assertTrue("new package does not exist", newPackage.exists());
		assertTrue("new package should be read-only", attributes == null || attributes.isReadOnly());
	}
	
	public void testImportFromMultiRoots1() throws Exception {
		fUpdateTextualMatches= true;
		helperProjectsPrgTest(
			new String[][] {
				new String[] { "p.p" }, new String[] { "p.p", "tests" }
				},
			"q",
			new String[][][] {
				new String[][] { new String[] { "A" }},
				new String[][] { new String[] { "ATest" }, new String[] { "AllTests" }}
		});
	}
	
	public void testImportFromMultiRoots2() throws Exception {
		helperProjectsPrgTest(
				new String[][] {
							new String[]{"p.p"},
							new String[]{"p.p", "tests"}
							},
			"q",
			new String[][][] {
							  new String[][] {new String[]{"A"}},
							  new String[][] {new String[]{"ATest", "TestHelper"}, new String[]{"AllTests", "QualifiedTests"}}
							  }
			);
	}

	public void testImportFromMultiRoots3() throws Exception {
		helperMultiRoots(new String[]{"srcPrg", "srcTest"}, 
			new String[][] {
							new String[]{"p.p"},
							new String[]{"p.p"}
							},
			"q",
			new String[][][] {
							  new String[][] {new String[]{"ToQ"}},
							  new String[][] {new String[]{"Ref"}}
							  }
			);
	}

	public void testImportFromMultiRoots4() throws Exception {
		//circular buildpath references
		IJavaProject projectPrg= null;
		IJavaProject projectTest= null;
		Hashtable options= JavaCore.getOptions();
		Object cyclicPref= JavaCore.getOption(JavaCore.CORE_CIRCULAR_CLASSPATH);
		try {
			projectPrg= JavaProjectHelper.createJavaProject("RenamePack1", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectPrg));
			IPackageFragmentRoot srcPrg= JavaProjectHelper.addSourceContainer(projectPrg, "srcPrg");

			projectTest= JavaProjectHelper.createJavaProject("RenamePack2", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectTest));
			IPackageFragmentRoot srcTest= JavaProjectHelper.addSourceContainer(projectTest, "srcTest");

			options.put(JavaCore.CORE_CIRCULAR_CLASSPATH, JavaCore.WARNING);
			JavaCore.setOptions(options);
			JavaProjectHelper.addRequiredProject(projectTest, projectPrg);
			JavaProjectHelper.addRequiredProject(projectPrg, projectTest);
			
			helperMultiProjects(new IPackageFragmentRoot[] {srcPrg, srcTest},
				new String[][] {
						new String[]{"p"},
						new String[]{"p"}
				},
				"a.b.c",
				new String[][][] {
						new String[][] {new String[]{"A", "B"}},
						new String[][] {new String[]{"ATest"}}
				}
			);
		} finally {
			options.put(JavaCore.CORE_CIRCULAR_CLASSPATH, cyclicPref);
			JavaCore.setOptions(options);	
			JavaProjectHelper.delete(projectPrg);
			JavaProjectHelper.delete(projectTest);
		}
	}
	
	public void testImportFromMultiRoots5() throws Exception {
		//rename srcTest-p.p to q => ATest now must import p.p.A
		IJavaProject projectPrg= null;
		IJavaProject projectTest= null;
		try {
			projectPrg= JavaProjectHelper.createJavaProject("RenamePack1", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectPrg));
			IPackageFragmentRoot srcPrg= JavaProjectHelper.addSourceContainer(projectPrg, "srcPrg");

			projectTest= JavaProjectHelper.createJavaProject("RenamePack2", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectTest));
			IPackageFragmentRoot srcTest= JavaProjectHelper.addSourceContainer(projectTest, "srcTest");

			JavaProjectHelper.addRequiredProject(projectTest, projectPrg);

			helperMultiProjects(new IPackageFragmentRoot[] { srcTest, srcPrg },
				new String[][] {
					new String[] {"p.p"}, new String[] {"p.p"}
				},
				"q",
				new String[][][] {
					new String[][] {new String[] {"ATest"}},
					new String[][] {new String[] {"A"}}
				}
			);
		} finally {
			JavaProjectHelper.delete(projectPrg);
			JavaProjectHelper.delete(projectTest);
		}
		
	}
	
	public void testImportFromMultiRoots6() throws Exception {
		//rename srcTest-p.p to a.b.c => ATest must retain import p.p.A
		helperMultiRoots(new String[]{"srcTest", "srcPrg"}, 
				new String[][] {
								new String[]{"p.p"},
								new String[]{"p.p"}
								},
				"cheese",
				new String[][][] {
								  new String[][] {new String[]{"ATest"}},
								  new String[][] {new String[]{"A"}}
								  }
		);
	}

	public void testImportFromMultiRoots7() throws Exception {
		IJavaProject prj= null;
		IJavaProject prjRef= null;
		IJavaProject prjOther= null;
		try {
			prj= JavaProjectHelper.createJavaProject("prj", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(prj));
			IPackageFragmentRoot srcPrj= JavaProjectHelper.addSourceContainer(prj, "srcPrj"); //$NON-NLS-1$

			prjRef= JavaProjectHelper.createJavaProject("prj.ref", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(prjRef));
			IPackageFragmentRoot srcPrjRef= JavaProjectHelper.addSourceContainer(prjRef, "srcPrj.ref"); //$NON-NLS-1$

			prjOther= JavaProjectHelper.createJavaProject("prj.other", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(prjOther));
			IPackageFragmentRoot srcPrjOther= JavaProjectHelper.addSourceContainer(prjRef, "srcPrj.other"); //$NON-NLS-1$

			JavaProjectHelper.addRequiredProject(prjRef, prj);
			JavaProjectHelper.addRequiredProject(prjRef, prjOther);

			helperMultiProjects(
				new IPackageFragmentRoot[] { srcPrj, srcPrjRef, srcPrjOther },
				new String[][] {
					new String[] {"pack"},
					new String[] {"pack", "pack.man"},
					new String[] {"pack"}
				},
				"com.packt",
				new String[][][] {
					new String[][] {new String[] {"DingsDa"}},
					new String[][] {new String[] {"Referer"}, new String[] {"StarImporter"}},
					new String[][] {new String[] {"Namesake"}}
				}
			);
		} finally {
			JavaProjectHelper.delete(prj);
			JavaProjectHelper.delete(prjRef);
			JavaProjectHelper.delete(prjOther);
		}
	}

	public void testStatic1() throws Exception {
		helper2(new String[]{"s1.j.l", "s1"}, new String[][]{{"S"},{"B"}}, "s1.java.lang");
	}
	
	public void testStaticMultiRoots1() throws Exception {
		helperProjectsPrgTest(
			new String[][] {
				new String[] { "p.p" }, new String[] { "p.p", "tests" }
				},
			"q",
			new String[][][] {
				new String[][] { new String[] { "A" }},
				new String[][] { new String[] { "ATest" }, new String[] { "AllTests" }}
		});
	}
}
