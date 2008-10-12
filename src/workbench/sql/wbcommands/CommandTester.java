/*
 * CommandTester.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import workbench.sql.wbcommands.console.WbDisconnect;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class to test whether a given SQL Verb is an internal 
 * Workbench command. This is used by the SqlFormatter, because
 * the verbs for WbXXXX commands are not formatted in uppercase.
 * 
 * This is also used by the code completion to check for WB specific commands.
 * 
 * @see workbench.sql.formatter.SqlFormatter
 * @see workbench.gui.completion.StatementContext
 * 
 * @author support@sql-workbench.net
 */
public class CommandTester
{
	private final Set<String> commands;
	private final Map<String, String> formattedWords;
	
	public CommandTester()
	{
		commands = new HashSet<String>();
		commands.add(WbCall.VERB);
		commands.add(WbConfirm.VERB);
		commands.add(WbCopy.VERB);
		commands.add(WbDataDiff.VERB);
		commands.add(WbDefinePk.VERB);
		commands.add(WbDefineVar.VERB);
		commands.add(WbDescribeTable.VERB);
		commands.add(WbDescribeTable.VERB_LONG);
		commands.add(WbDisableOraOutput.VERB);
		commands.add(WbEnableOraOutput.VERB);
		commands.add(WbEndBatch.VERB);
		commands.add(WbExport.VERB);
		commands.add(WbFeedback.VERB);
		commands.add(WbImport.VERB);
		commands.add(WbInclude.VERB);
		commands.add(WbListPkDef.VERB);
		commands.add(WbListVars.VERB);
		commands.add(WbListTables.VERB);
		commands.add(WbLoadPkMapping.VERB);
		commands.add(WbRemoveVar.VERB);
		commands.add(WbSavePkMapping.VERB);
		commands.add(WbSchemaDiff.VERB);
		commands.add(WbSchemaReport.VERB);
		commands.add(WbSelectBlob.VERB);
		commands.add(WbStartBatch.VERB);
		commands.add(WbXslt.VERB);
		commands.add(WbConnect.VERB);
		commands.add(WbDisconnect.VERB);
		commands.add(WbHideWarnings.VERB);
		
		formattedWords = new HashMap<String, String>();
		formattedWords.put(WbSavePkMapping.VERB, WbSavePkMapping.FORMATTED_VERB);
		formattedWords.put(WbLoadPkMapping.VERB, WbLoadPkMapping.FORMATTED_VERB);
		formattedWords.put(WbDefineVar.VERB, "WbVarDefine");
		formattedWords.put(WbListPkDef.VERB, WbListPkDef.FORMATTED_VERB);
		formattedWords.put(WbEndBatch.VERB, "WbEndBatch");
		formattedWords.put(WbStartBatch.VERB, "WbStartBatch");
		formattedWords.put(WbSchemaDiff.VERB, "WbSchemaDiff");
		formattedWords.put(WbDataDiff.VERB, "WbDataDiff");
		formattedWords.put(WbDescribeTable.VERB, WbDescribeTable.VERB.toUpperCase());
		formattedWords.put(WbDescribeTable.VERB_LONG, WbDescribeTable.VERB_LONG.toUpperCase());
		formattedWords.put(WbHideWarnings.VERB, "WbEnableWarnings");
	}
	
	public Collection<String> getCommands()
	{
		return Collections.unmodifiableSet(commands);
	}
	
	public boolean isWbCommand(String verb)
	{
		if (verb == null) return false;
		return commands.contains(verb.trim().toUpperCase());
	}
	
	public String formatVerb(String verb)
	{
		String f = formattedWords.get(verb.toUpperCase());
		if (f != null)
		{
			return f;
		}
		else
		{
			return fixCase(verb);
		}
	}
	
	private String fixCase(String verb)
	{
		if (!verb.toLowerCase().startsWith("wb")) return verb;
		String s = "Wb" + Character.toUpperCase(verb.charAt(2)) + verb.substring(3).toLowerCase();
		return s;
	}
	
}
