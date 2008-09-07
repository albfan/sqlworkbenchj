/*
 * ToolDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.IOException;
import java.util.List;

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
		setCommandLine(exe);
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

	/**
	 * The commndline for this external tool, including possible (static)
	 * parameters.
	 */
	public String getCommandLine()
	{
		return appPath;
	}

	/**
	 * The command line that should be used to run the external tool. This may
	 * include parameters to the application, therefor a File object cannot be passed
	 * 
	 * Parameters are separated with spaces from the actual program path.
	 * Program paths with spaces are expected to be enclosed with double quotes
	 *
	 * The method is invoked through reflection from the {@link workbench.gui.settings.ToolDefinitionPanel}
	 * by a {@link workbench.gui.components.StringPropertyEditor}
	 *
	 * @param path
	 */
	public void setCommandLine(String path)
	{
		this.appPath = path;
	}

	public String toString()
	{
		return getName();
	}

	public WbFile getExecutable()
	{
		if (this.appPath == null) return null;
		// as the commandline may include parameters, we assume the first token is the actual program. 
		List<String> appDef = tokenizePath();
		String prgPath = appDef.get(0);
		WbFile f = new WbFile(prgPath);
		return f;
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
		WbFile f = getExecutable();
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

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 43 * hash + (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}
}
