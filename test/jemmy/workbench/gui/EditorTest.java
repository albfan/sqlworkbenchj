/*
 * MainWindowTest.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui;

import junit.framework.TestCase;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JComponentOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JMenuBarOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import workbench.gui.sql.EditorPanel;

/**
 * @author support@sql-workbench.net
 */
public class EditorTest
	extends TestCase
{
	private GuiTestUtil testUtil;

	public EditorTest(String testName)
	{
		super(testName);
		this.testUtil = new GuiTestUtil("EditorTest");
	}

	private void startApplication()
	{
		try
		{
			testUtil.startApplication();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void findText()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JComponentOperator editorComp = new JComponentOperator(mainWindow, chooser);
		EditorPanel editor = (EditorPanel)editorComp.getSource();

		editor.setText("select * from person;");
		editor.setCaretPosition(0);

		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);
		mainMenu.pushMenuNoBlock("Edit|Find");
		JDialogOperator dialog = new JDialogOperator(mainWindow, "Find");

		setCheckbox(dialog, "regex", false);
		setCheckbox(dialog, "ignorecase", false);
		setCheckbox(dialog, "wholeword", false);

		setTextField(dialog, "searchtext", "person");

		JButtonOperator ok = new JButtonOperator(dialog, "OK");
		ok.push();

		assertEquals("person", editor.getSelectedText());
	}

	private void replaceText()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JComponentOperator editorComp = new JComponentOperator(mainWindow, chooser);
		EditorPanel editor = (EditorPanel)editorComp.getSource();

		editor.setText("select * from person;");
		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);
		mainMenu.pushMenuNoBlock("Edit|Replace");
		JDialogOperator dialog = new JDialogOperator(mainWindow, "Replace");

		setCheckbox(dialog, "regex", false);
		setCheckbox(dialog, "ignorecase", true);
		setCheckbox(dialog, "wordsonly", false);
		setCheckbox(dialog, "selectedtext", false);

		setTextField(dialog, "searchtext", "*");
		setTextField(dialog, "replacetext", "nr, firstname, lastname");

		chooser.setName("replaceallbutton");
		JButtonOperator replaceAll = new JButtonOperator(dialog, chooser);
		replaceAll.push();

		new JButtonOperator(dialog, "Close").push();

		assertEquals("select nr, firstname, lastname from person;", editor.getText());
	}

	private void setTextField(JDialogOperator dialog, String name, String newText)
	{
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName(name);
		JTextFieldOperator searchtext = new JTextFieldOperator(dialog, chooser);
		searchtext.setText(newText);
	}

	private void setCheckbox(JDialogOperator dialog, String cbxName, boolean selected)
	{
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName(cbxName);
		JCheckBoxOperator cbx = new JCheckBoxOperator(dialog, chooser);
		cbx.setSelected(selected);
	}

	private void execute(Runnable r)
	{
		QueueTool tool = new QueueTool();
		tool.invokeAndWait(r);
		tool.waitEmpty();
	}

	public void testWindow()
	{
		try
		{
			startApplication();
			replaceText();
			findText();
			testUtil.stopApplication();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
