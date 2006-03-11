/*
 * CompletionSearchField.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.completion;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A quick search field for the auto completion popup
 *
 * @author support@sql-workbench.net
 */
public class CompletionSearchField
	extends JTextField
	implements KeyListener, DocumentListener
{
	private CompletionPopup parent;
	
	public CompletionSearchField(CompletionPopup popup)
	{
		super();
		this.parent = popup;
		this.addKeyListener(this);
		this.getDocument().addDocumentListener(this);
	}

	public void keyTyped(KeyEvent e)
	{
	}

	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			parent.quickSearchValueSelected();
		}
		else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			this.parent.closeQuickSearch();
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void insertUpdate(DocumentEvent e)
	{
		this.parent.selectMatchingEntry(this.getText());
	}

	public void removeUpdate(DocumentEvent e)
	{
		this.parent.selectMatchingEntry(this.getText());
	}

	public void changedUpdate(DocumentEvent e)
	{
		this.parent.selectMatchingEntry(this.getText());
	}

}

