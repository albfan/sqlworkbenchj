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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import junit.framework.TestCase;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JComponentOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JMenuBarOperator;
import org.netbeans.jemmy.operators.JMenuItemOperator;
import org.netbeans.jemmy.operators.JMenuOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import workbench.gui.sql.EditorPanel;
import workbench.resource.Settings;

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

	private void commentText()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JComponentOperator editorComp = new JComponentOperator(mainWindow, chooser);
		EditorPanel editor = (EditorPanel)editorComp.getSource();

		editor.setText("select nr, firstname, lastname\nfrom person;");
		editor.setCaretPosition(0);
		editor.selectNone();
		
		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);
		JMenuOperator editMenu = new JMenuOperator(mainMenu.getMenu(1));
		JMenuItem commentItem = (JMenuItem)editMenu.getMenuComponent(17);
		JMenuItem uncommentItem = (JMenuItem)editMenu.getMenuComponent(18);
		
		JMenuItemOperator comment = new JMenuItemOperator(commentItem);
		JMenuItemOperator unComment = new JMenuItemOperator(uncommentItem);
		
		assertFalse(commentItem.isEnabled());		
		assertFalse(uncommentItem.isEnabled());		
		
		editor.select(0, 15);
		assertTrue(comment.isEnabled());		
		assertTrue(uncommentItem.isEnabled());
		
		comment.push();
		String text = editor.getText();
		assertTrue(text.startsWith("--"));
		editor.select(0, 15);
		unComment.push();
		text = editor.getText();
		assertFalse(text.startsWith("--"));
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
		JMenuOperator sqlMenu = new JMenuOperator(mainMenu.getMenu(4));
		
		QueueTool tool = new QueueTool();
		editor.selectAll();
		tool.waitEmpty();
		
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		
		try
		{
			mainMenu.pushMenu("SQL|Copy Code Snippet", "|");
			tool.waitEmpty();
			
			final String text = (String)clp.getData(DataFlavor.stringFlavor);
//			System.out.println("clipboard: " + text);
			assertTrue(text.startsWith("String sql = \"select nr,"));
			
			editor.setText(text);
			editor.selectAll();
			mainMenu.pushMenu("SQL|Clean Java Code", "|");
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

	private void checkWordSep()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JComponentOperator editorComp = new JComponentOperator(mainWindow, chooser);
		EditorPanel editor = (EditorPanel)editorComp.getSource();

		Settings.getInstance().setEditorNoWordSep("");
		editor.setText("my_person;");
		editor.setCaretPosition(0);
		
		QueueTool tool = new QueueTool();
		tool.waitEmpty();
		
		editorComp.pushKey(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK);
		tool.waitEmpty();
		
		int pos = editor.getCaretPosition();
		assertEquals("Wrong word jump", 2, pos);

		Settings.getInstance().setEditorNoWordSep("_");
		editor.setCaretPosition(0);
		editorComp.pushKey(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK);
		tool.waitEmpty();
		
		pos = editor.getCaretPosition();
		assertEquals("Wrong word jump", 9, pos);
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
			testUtil.stopApplication();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
