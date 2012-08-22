/*
 * ClipboardWrapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.text.JTextComponent;
import workbench.interfaces.ClipboardSupport;

/**
 * @author Thomas Kellerer
 */
public class ClipboardWrapper
	implements ClipboardSupport
{
	private JTextComponent client;

	public ClipboardWrapper(JTextComponent aClient)
	{
		this.client = aClient;
	}

	@Override
	public void copy()
	{
		this.client.copy();
	}

	@Override
	public void clear()
	{
		if (this.client.isEditable())
		{
			this.client.replaceSelection("");
		}
	}

	@Override
	public void cut()
	{
		this.client.cut();
	}

	@Override
	public void paste()
	{
		this.client.paste();
	}

	@Override
	public void selectAll()
	{
		this.client.select(0, this.client.getText().length());
	}

}
