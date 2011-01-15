/*
 * WbDocument.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.PlainDocument;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDocument
	extends PlainDocument
	implements DocumentListener
{
	private boolean modified;
	public WbDocument(Content c)
	{
		super(c);
		addDocumentListener(this);
	}

	public WbDocument()
	{
		super();
		addDocumentListener(this);
	}

	public void resetModified()
	{
		modified = false;
	}
	
	public boolean isModified()
	{
		return modified;
	}
	
	public void insertUpdate(DocumentEvent e)
	{
		modified = true;
	}

	public void removeUpdate(DocumentEvent e)
	{
		modified = true;
	}

	public void changedUpdate(DocumentEvent e)
	{
		modified = true;
	}

}
