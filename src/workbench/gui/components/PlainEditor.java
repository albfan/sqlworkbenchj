/*
 * PlainEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import workbench.gui.editor.SearchAndReplace;
import workbench.interfaces.Restoreable;
import workbench.interfaces.TextContainer;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class PlainEditor
	extends JPanel
	implements ActionListener, TextContainer, Restoreable
{
	private JTextArea editor;
	private JCheckBox wordWrap;
	private	SearchAndReplace replacer;
	
	public PlainEditor(Window parent)
	{
		editor = new JTextArea();
		editor.putClientProperty("JTextArea.infoBackground", Boolean.TRUE);
		TextComponentMouseListener l = new TextComponentMouseListener(this.editor);
		
		JScrollPane scroll = new JScrollPane(editor);
		editor.setLineWrap(true);
		editor.setWrapStyleWord(true); 
		editor.setFont(Settings.getInstance().getDataFont());
		this.setLayout(new BorderLayout());
		wordWrap = new JCheckBox(ResourceMgr.getString("LblWordWrap"));
		wordWrap.addActionListener(this);
		//wordWrap.setSelected(true);
		this.add(wordWrap, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
		this.setFocusable(false);
		Document d = editor.getDocument();
		if (d != null)
		{
			int tabSize = Settings.getInstance().getEditorTabWidth();
			d.putProperty(PlainDocument.tabSizeAttribute, new Integer(tabSize));
		}
		replacer = new SearchAndReplace(this, this);
		l.addAction(replacer.getFindAction());
		l.addAction(replacer.getFindAgainAction());
		l.addAction(replacer.getReplaceAction());
	}
	
	public int getCaretPosition() { return this.editor.getCaretPosition(); }
	public int getSelectionEnd() { return this.editor.getSelectionEnd(); }
	public int getSelectionStart() { return this.editor.getSelectionStart(); }
	public void select(int start, int end) { this.editor.select(start, end); }
	
	public void requestFocus()
	{
		this.editor.requestFocus();
	}
	
	public boolean requestFocusInWindow()
	{
		return this.editor.requestFocusInWindow();
	}
	
	public void restoreSettings()
	{
		boolean wrap = Settings.getInstance().getPlainEditorWordWrap();
		wordWrap.setSelected(wrap);
		this.editor.setLineWrap(wrap);
	}
	
	public void saveSettings()
	{
		Settings.getInstance().setPlainEditorWordWrap(wordWrap.isSelected());
	}

	public void setSelectedText(String aText)
	{
		this.editor.replaceSelection(aText);
	}

	public String getText()
	{
		return this.editor.getText();
	}

	public String getSelectedText()
	{
		return this.editor.getSelectedText();
	}

	public void setText(String aText)
	{
		this.editor.setText(aText);
	}

	public void setCaretPosition(int pos)
	{
		this.editor.setCaretPosition(pos);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.editor.setLineWrap(this.wordWrap.isSelected());
	}

	public void setEditable(boolean flag)
	{
		this.editor.setEditable(flag);
	}


}
