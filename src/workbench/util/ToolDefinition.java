/*
 * ToolDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import workbench.util.WbStringTokenizer;

/**
 * @author support@sql-workbench.net
 */
public class ToolDefinition
{
	private String appPath;
	private String name;
	
	public ToolDefinition()
	{
	}

	public ToolDefinition(String exe, String name)
	{
		setApplicationPath(exe);
		setName(name);
	}
	
	public String getName()
	{
		return name;
	}

	public void setName(String appName)
	{
		this.name = appName;
	}

	public String getApplicationPath()
	{
		return appPath;
	}

	public void setApplicationPath(String path)
	{
		this.appPath = path;
	}

	public String toString()
	{
		return getName();
	}
	
	public void runApplication(String arg)
		throws IOException
	{
		List<String> appDef = tokenizePath();
		String[] cmd = new String[appDef.size() + 1];
		for (int i = 0; i < appDef.size(); i++)
		{
			cmd[i] = appDef.get(i);
		}
		cmd[appDef.size()] = arg;
		Runtime.getRuntime().exec(cmd, null);
	}
	
	public boolean executableExists()
	{
		List<String> appDef = tokenizePath();
		String prgPath = appDef.get(0);
		File f = new File(prgPath);
		return f.exists();
	}
	
	private List<String> tokenizePath()
	{
		WbStringTokenizer tok = new WbStringTokenizer(this.appPath, " ", true, "\"", true);
		return tok.getAllTokens();
	}
	
	public boolean equals(Object other)
	{
		if (this.name == null) return false;
		if (other instanceof ToolDefinition)
		{
			ToolDefinition t = (ToolDefinition)other;
			return this.name.equals(t.getName());
		}
		else if (other instanceof String)
		{
			return this.name.equals((String)other);
		}
		return false;
	}
}
