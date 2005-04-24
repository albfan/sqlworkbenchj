/*
 * CommandTester.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to test whether a given SQL Verb is an internal 
 * Workbench command
 * @author info@sql-workbench.net
 */
public class CommandTester
{
	private final List commands;
	
	public CommandTester()
	{
		commands = new ArrayList();
		commands.add(WbCopy.VERB);
		commands.add(WbExport.VERB);
		commands.add(WbImport.VERB);
		commands.add(WbDiff.VERB);
		commands.add(WbSchemaReport.VERB);
		commands.add(WbInclude.INCLUDE_LONG);
		commands.add(WbXslt.VERB);
	}
	
	public boolean isWbCommand(String verb)
	{
		if (verb == null) return false;
		return commands.contains(verb.trim().toUpperCase());
	}
	
}
