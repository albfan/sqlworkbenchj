/*
 * ConsoleStatusBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.io.PrintStream;
import workbench.interfaces.StatusBar;

/**
 * An implementation of the {@link workbench.interfaces.StatusBar} interface
 * to display information in Console mode
 * 
 * @author Thomas Kellerer
 */
public class ConsoleStatusBar
	implements StatusBar
{
	private PrintStream output;
	private String lastMessage;
	
	public ConsoleStatusBar()
	{
		output = System.out;
	}
	
	private String createDeleteString(String original)
	{
		if (original == null) return "\r";
		StringBuilder result = new StringBuilder(original.length()+2);
		result.append('\r');
		for (int i = 0; i < original.length(); i++)
		{
			result.append(' ');
		}
		result.append('\r');
		return result.toString();
	}

	@Override
	public void setStatusMessage(String message, int duration)
	{
		setStatusMessage(message);
	}

	@Override
	public void setStatusMessage(String message)
	{
		if (lastMessage != null)
		{
			output.print(createDeleteString(lastMessage));
		}
		output.print('\r');
		output.print(message);
		this.lastMessage = message;
	}

	public void clearStatusMessage()
	{
		if (lastMessage != null)
		{
			output.print(createDeleteString(lastMessage));
			lastMessage = null;
		}
		else
		{
			output.print("\r");
		}
	}

	public void repaint()
	{
	}

	public String getText()
	{
		return lastMessage;
	}
	
}
