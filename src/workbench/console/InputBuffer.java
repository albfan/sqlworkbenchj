/*
 * InputBuffer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import workbench.sql.DelimiterDefinition;

/**
 * A buffer that collects pieces of text entered by the user until
 * it is terminated with a {@link workbench.sql.DelimiterDefinition}
 * 
 * @author support@sql-workbench.net
 */
public class InputBuffer
{
	private StringBuilder script;
	private DelimiterDefinition delimiter;

	public InputBuffer()
	{
		this.delimiter = DelimiterDefinition.STANDARD_DELIMITER;
		script = new StringBuilder(1000);
	}

	public String getScript()
	{
		return script.toString();
	}

	public void clear()
	{
		script.setLength(0);
	}

	public void setDelimiter(DelimiterDefinition delim)
	{
		this.delimiter = delim;
	}

	public boolean addLine(String line)
	{
		script.append('\n');
		script.append(line);
		return isComplete();
	}

	public boolean isComplete()
	{
		return delimiter.terminatesScript(script.toString());
	}
}
