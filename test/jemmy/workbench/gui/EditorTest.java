/*
 * EditorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;

import workbench.resource.Settings;

import workbench.gui.sql.EditorPanel;

import org.junit.Test;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JComponentOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JListOperator;
import org.netbeans.jemmy.operators.JMenuBarOperator;
import org.netbeans.jemmy.operators.JMenuItemOperator;
import org.netbeans.jemmy.operators.JMenuOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class EditorTest

{
	private GuiTestUtil testUtil;

	public EditorTest()
	{
		this.testUtil = new GuiTestUtil("EditorTest");
	}

	private void startApplication()
	{
		try
		{
			testUtil.startApplication();
			JemmyProperties.getCurrentTimeouts().load(getClass().getResourceAsStream("guitests.timeouts"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void commentText()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JComponentOperator editorComp = new JComponentOperator(mainWindow, chooser);
		EditorPanel editor = (EditorPanel)editorComp.getSource();

		editor.setText("select nr, firstname, lastname\nfrom person;");
		editor.setCaretPosition(0);
//		editor.selectNone();

		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);
		JMenuOperator editMenu = new JMenuOperator(mainMenu.getMenu(1));
		JMenuItem commentItem = (JMenuItem)editMenu.getMenuComponent(20);
		JMenuItem uncommentItem = (JMenuItem)editMenu.getMenuComponent(21);

		JMenuItemOperator comment = new JMenuItemOperator(commentItem);
		JMenuItemOperator unComment = new JMenuItemOperator(uncommentItem);

		QueueTool tool = new QueueTool();

		//editor.select(0, 15);
		comment.push();
		tool.waitEmpty();

		String text = editor.getText();
		assertTrue(text.startsWith("--"));

		//editor.select(0, 15);

		// test toggle
		comment.push();
		tool.waitEmpty();

		text = editor.getText();
		assertFalse(text.startsWith("--"));

		editor.setText("-- first line\n-- second line\n");
		editor.selectAll();
		tool.waitEmpty();
		unComment.push();
		tool.waitEmpty();
		text = editor.getText();

		assertEquals("first line\nsecond line\n", text);
	}

	private void copySnippet()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JComponentOperator editorComp = new JComponentOperator(mainWindow, chooser);
		final EditorPanel editor = (EditorPanel)editorComp.getSource();

		editor.setText("select nr, firstname, lastname\nfrom person;");
		editor.setCaretPosition(0);

		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);
//		JMenuOperator sqlMenu = new JMenuOperator(mainMenu.getMenu(4));

		QueueTool tool = new QueueTool();
		editor.selectAll();
		tool.waitEmpty();

		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();

		try
		{
			mainMenu.pushMenu("SQL|Code tools|Copy Code Snippet", "|");
			tool.waitEmpty();

			final String text = (String)clp.getData(DataFlavor.stringFlavor);
//			System.out.println("clipboard: " + text);
			assertTrue(text.startsWith("String sql = \"select nr,"));

			editor.setText(text);
			editor.selectAll();
			mainMenu.pushMenu("SQL|Code tools|Clean Java Code", "|");
			tool.waitEmpty();
			String newText = editor.getText();
//			System.out.println("new text: " + newText);
			assertTrue(newText.startsWith("select nr,"));

		}
		catch(Exception e)
		{
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

		setComboboxText(dialog, "searchtext", "person");
		QueueTool tool = new QueueTool();
		tool.waitEmpty();

		JButtonOperator ok = new JButtonOperator(dialog, "OK");
		ok.push();
		tool.waitEmpty();

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

		QueueTool tool = new QueueTool();

		setCheckbox(dialog, "regex", false);
		setCheckbox(dialog, "ignorecase", true);
		setCheckbox(dialog, "wordsonly", false);
		setCheckbox(dialog, "selectedtext", false);

		setComboboxText(dialog, "searchtext", "*");
		tool.waitEmpty();
		setComboboxText(dialog, "replacetext", "nr, firstname, lastname");
		tool.waitEmpty();

		chooser.setName("replaceallbutton");
		JButtonOperator replaceAll = new JButtonOperator(dialog, chooser);
		replaceAll.push();

		tool.waitEmpty();

		chooser.setName("closebutton");
		JButtonOperator close = new JButtonOperator(dialog, chooser);
		close.push();

		assertEquals("select nr, firstname, lastname from person;", editor.getText());
	}

	private void changeWordSep(String newSep)
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		new JMenuBarOperator(mainWindow).pushMenuNoBlock("Tools|Options", "|");
		JDialogOperator dialog = new JDialogOperator(mainWindow, "Settings");
		final JListOperator pages = new JListOperator(dialog);

		pages.selectItem(1); // select the editor page
		setTextField(dialog, "nowordsep", newSep);
		new JButtonOperator(dialog, "OK").push();
		QueueTool tool = new QueueTool();
		tool.waitEmpty();
	}

	private void checkWordSep()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JComponentOperator editorComp = new JComponentOperator(mainWindow, chooser);
		EditorPanel editor = (EditorPanel)editorComp.getSource();

		QueueTool tool = new QueueTool();

		changeWordSep("");
		assertEquals("Wrong word separator", "", Settings.getInstance().getEditorNoWordSep());

		editor.setText("my_person;");
		editor.setCaretPosition(0);

		tool.waitEmpty();

		editorComp.pushKey(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK);
		tool.waitEmpty();

		int pos = editor.getCaretPosition();
		assertEquals("Wrong word jump", 2, pos);

		changeWordSep("_");
		assertEquals("Wrong word separator", "_", Settings.getInstance().getEditorNoWordSep());

		editor.setCaretPosition(0);
		editorComp.pushKey(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK);
		tool.waitEmpty();

		pos = editor.getCaretPosition();
		assertEquals("Wrong word jump", 9, pos);
	}

	private void setComboboxText(JDialogOperator dialog, String name, String newText)
	{
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName(name);
		JComboBoxOperator comp = new JComboBoxOperator(dialog, chooser);
		comp.setSelectedItem(newText);
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

	@Test
	public void testEditor()
	{
		try
		{
			startApplication();
			replaceText();
			findText();
			commentText();
			copySnippet();
			checkWordSep();
		}
		finally
		{
			testUtil.stopApplication();
		}
	}
}
