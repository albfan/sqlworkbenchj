/*
 * ClipboardWrapper.java
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

import javax.swing.text.JTextComponent;
import workbench.interfaces.ClipboardSupport;

/**
 * @author support@sql-workbench.net
 */
public class ClipboardWrapper
	implements ClipboardSupport
{
	private JTextComponent client;
	
	public ClipboardWrapper(JTextComponent aClient)
	{
		this.client = aClient;
	} 
	
	public void copy() 
	{ 
		this.client.copy(); 
	}
	
	public void clear() 
	{ 
		if (this.client.isEditable())
		{
			this.client.replaceSelection("");
		}
	}
	
	public void cut() 
	{  
		this.client.cut(); 
	}
	
	public void paste() 
	{ 
		this.client.paste(); 
	}
	
	public void selectAll() 
	{
		this.client.select(0, this.client.getText().length());
	}
	
}
