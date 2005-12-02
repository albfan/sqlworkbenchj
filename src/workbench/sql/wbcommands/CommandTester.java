/*
 * CommandTester.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class to test whether a given SQL Verb is an internal 
 * Workbench command. This is used by the SqlFormatter, because
 * the verbs for WbXXXX commands are not formatted in uppercase.
 * @see workbench.sql.formatter.SqlFormatter
 * @author support@sql-workbench.net
 */
public class CommandTester
{
	private final Set commands;
	private final Map formattedWords;
	
	public CommandTester()
	{
		commands = new HashSet();
		commands.add(WbCopy.VERB);
		commands.add(WbExport.VERB);
		commands.add(WbImport.VERB);
		commands.add(WbDiff.VERB);
		commands.add(WbSchemaReport.VERB);
		commands.add(WbInclude.INCLUDE_LONG);
		commands.add(WbXslt.VERB);
		commands.add(WbDefineVar.DEFINE_LONG);
		commands.add(WbDefineVar.DEFINE_SHORT);
		commands.add(WbRemoveVar.VERB);
		commands.add(WbFeedback.VERB);
		commands.add(WbSelectBlob.VERB);
		commands.add(WbStartBatch.VERB);
		commands.add(WbEndBatch.VERB);
		commands.add(WbDefinePk.VERB);
		commands.add(WbListPkDef.VERB);
		commands.add(WbSavePkMapping.VERB);
		commands.add(WbLoadPkMapping.VERB);
		
		formattedWords = new HashMap();
		formattedWords.put(WbSavePkMapping.VERB, WbSavePkMapping.FORMATTED_VERB);
		formattedWords.put(WbLoadPkMapping.VERB, WbLoadPkMapping.FORMATTED_VERB);
		formattedWords.put(WbDefineVar.DEFINE_LONG.getVerb(), "WbVarDefine");
		formattedWords.put(WbDefineVar.DEFINE_SHORT.getVerb(), "WbVarDef");
		formattedWords.put(WbListPkDef.VERB, WbListPkDef.FORMATTED_VERB);
		formattedWords.put(WbEndBatch.VERB, "WbEndBatch");
		formattedWords.put(WbStartBatch.VERB, "WbStartBatch");
	}
	
	public boolean isWbCommand(String verb)
	{
		if (verb == null) return false;
		return commands.contains(verb.trim().toUpperCase());
	}
	
	public String formatVerb(String verb)
	{
		String f = (String)formattedWords.get(verb.toUpperCase());
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
