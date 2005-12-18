/*
 * ConsoleStatusBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.io.PrintStream;
import workbench.interfaces.StatusBar;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
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
		if (original == null) return StringUtil.EMPTY_STRING;
		StringBuffer result = new StringBuffer(original.length() * 2);
		result.append(original);
		result.append(original);
		int count = result.length();
		for (int i = 0; i < count; i++)
		{
			result.setCharAt(i, ' ');
		}
		for (int i = 0; i < count; i++)
		{
			result.setCharAt(i, (char)8);
		}
		return result.toString();
	}

	public void setStatusMessage(String message)
	{
		if (lastMessage != null)
		{
			output.print(createDeleteString(lastMessage));
		}
		output.print(message);
		this.lastMessage = message;
	}

	public void clearStatusMessage()
	{
		if (lastMessage != null)
		{
			output.print(createDeleteString(lastMessage));
		}
		output.println();
	}

	public void repaint()
	{
	}

	public String getText()
	{
		return lastMessage;
	}
	
}
