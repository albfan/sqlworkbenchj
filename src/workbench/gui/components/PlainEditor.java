/*
 * PlainEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import workbench.interfaces.TextContainer;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class PlainEditor
	extends JPanel
	implements TextContainer, ActionListener
{
	private JTextArea editor;
	private JCheckBox wordWrap;
	
	/** Creates a new instance of PlainEditor */
	public PlainEditor()
	{
		editor = new JTextArea();
		editor.addMouseListener(new TextComponentMouseListener());
		JScrollPane scroll = new JScrollPane(editor);
		editor.setLineWrap(true);
		editor.setWrapStyleWord(true); 
		editor.setFont(Settings.getInstance().getDataFont());
		this.setLayout(new BorderLayout());
		wordWrap = new JCheckBox(ResourceMgr.getString("LabelWordWrap"));
		wordWrap.addActionListener(this);
		wordWrap.setSelected(true);
		this.add(wordWrap, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
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

}
