/*
 * PlainEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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
 * A simple text editor based on a JTextArea.
 * The panel displays also a checkbox to turn word wrapping on and off
 * and optionally an information label.
 *
 * @author support@sql-workbench.net
 */
public class PlainEditor
	extends JPanel
	implements ActionListener, TextContainer, Restoreable
{
	private JTextArea editor;
	private JCheckBox wordWrap;
	private Color enabledBackground;
	private JLabel infoText;
	private JPanel toolPanel;

	public PlainEditor()
	{
		super();
		editor = new JTextArea();
		this.enabledBackground = editor.getBackground();
		editor.putClientProperty("JTextArea.infoBackground", Boolean.TRUE);
		TextComponentMouseListener l = new TextComponentMouseListener(this.editor);

		JScrollPane scroll = new JScrollPane(editor);
		editor.setLineWrap(true);
		editor.setWrapStyleWord(true);
		editor.setFont(Settings.getInstance().getEditorFont());
		this.setLayout(new BorderLayout());
		toolPanel = new JPanel();
		toolPanel.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
		wordWrap = new JCheckBox(ResourceMgr.getString("LblWordWrap"));
		wordWrap.addActionListener(this);
		toolPanel.add(wordWrap);

		this.add(toolPanel, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);
		this.setFocusable(false);
		Document d = editor.getDocument();
		if (d != null)
		{
			int tabSize = Settings.getInstance().getEditorTabWidth();
			d.putProperty(PlainDocument.tabSizeAttribute, Integer.valueOf(tabSize));
		}
		SearchAndReplace replacer = new SearchAndReplace(this, this);
		l.addAction(replacer.getFindAction());
		l.addAction(replacer.getFindAgainAction());
		l.addAction(replacer.getReplaceAction());
	}

	public int getCaretPosition() { return this.editor.getCaretPosition(); }
	public int getSelectionEnd() { return this.editor.getSelectionEnd(); }
	public int getSelectionStart() { return this.editor.getSelectionStart(); }
	public void select(int start, int end) { this.editor.select(start, end); }

	public void setInfoText(String text)
	{
		if (this.infoText == null)
		{
			this.infoText = new JLabel();
			this.toolPanel.add(Box.createHorizontalStrut(10));
			this.toolPanel.add(infoText);
		}
		this.infoText.setText(text);
	}

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
		this.editor.setBackground(enabledBackground);
	}


}
