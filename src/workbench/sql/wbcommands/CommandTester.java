/*
 * CommandTester.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import workbench.sql.wbcommands.console.WbDisconnect;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import workbench.sql.wbcommands.console.WbAbout;
import workbench.sql.wbcommands.console.WbDeleteProfile;
import workbench.sql.wbcommands.console.WbDisplay;
import workbench.sql.wbcommands.console.WbListProfiles;
import workbench.sql.wbcommands.console.WbRun;
import workbench.sql.wbcommands.console.WbStoreProfile;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;

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
 * @author Thomas Kellerer
 */
public class CommandTester
{

	private final Set<String> commands;
	private Map<String, String> formattedWords;

	public CommandTester()
	{
		commands = CollectionUtil.caseInsensitiveSet();
		commands.add(WbCall.VERB);
		commands.add(WbConfirm.VERB);
		commands.add(WbCopy.VERB);
		commands.add(WbDataDiff.VERB);
		commands.add(WbDefinePk.VERB);
		commands.add(WbDefineVar.VERB);
		commands.add(WbDeleteProfile.VERB);
		commands.add(WbDescribeObject.VERB);
		commands.add(WbDescribeObject.VERB_LONG);
		commands.add(WbDisableOraOutput.VERB);
		commands.add(WbDisplay.VERB);
		commands.add(WbEnableOraOutput.VERB);
		commands.add(WbEndBatch.VERB);
		commands.add(WbExport.VERB);
		commands.add(WbFeedback.VERB);
		commands.add(WbImport.VERB);
		commands.add(WbInclude.VERB);
		commands.add(WbListPkDef.VERB);
		commands.add(WbListVars.VERB);
		commands.add(WbListTables.VERB);
		commands.add(WbListProcedures.VERB);
		commands.add(WbListCatalogs.VERB);
		commands.add(WbListCatalogs.VERB_ALTERNATE);
		commands.add(WbListSchemas.VERB);
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
		commands.add(WbListProfiles.VERB);
		commands.add(WbStoreProfile.VERB);
		commands.add(WbHelp.VERB);
		commands.add(WbProcSource.VERB);
		commands.add(WbRun.VERB);
		commands.add(WbListTriggers.VERB);
		commands.add(WbTriggerSource.VERB);
		commands.add(WbGrepSource.VERB);
		commands.add(WbGrepData.VERB);
		commands.add(WbMode.VERB);
		commands.add(WbFetchSize.VERB);
		commands.add(WbAbout.VERB);
		commands.add(WbRunLB.VERB);
		commands.add(WbIsolationLevel.VERB);
		commands.add(WbConnInfo.VERB);
		commands.add(WbSysExec.VERB);
		commands.add(WbSysProps.VERB);
		commands.add(WbOraShow.VERB);
		commands.add(WbGenDrop.VERB);
		commands.add(WbSetProp.VERB);
	}

	private Map<String, String> getFormattedVerbs()
	{
		if (formattedWords == null)
		{
			formattedWords = new TreeMap<String, String>(CaseInsensitiveComparator.INSTANCE);
			formattedWords.put(WbSavePkMapping.VERB, WbSavePkMapping.FORMATTED_VERB);
			formattedWords.put(WbLoadPkMapping.VERB, WbLoadPkMapping.FORMATTED_VERB);
			formattedWords.put(WbDefineVar.VERB, "WbVarDef");
			formattedWords.put(WbListPkDef.VERB, WbListPkDef.FORMATTED_VERB);
			formattedWords.put(WbEndBatch.VERB, "WbEndBatch");
			formattedWords.put(WbStartBatch.VERB, "WbStartBatch");
			formattedWords.put(WbSchemaDiff.VERB, "WbSchemaDiff");
			formattedWords.put(WbSchemaReport.VERB, "WbSchemaReport");
			formattedWords.put(WbDataDiff.VERB, "WbDataDiff");
			formattedWords.put(WbDescribeObject.VERB, WbDescribeObject.VERB.toUpperCase());
			formattedWords.put(WbDescribeObject.VERB_LONG, WbDescribeObject.VERB_LONG.toUpperCase());
			formattedWords.put(WbHideWarnings.VERB, "WbEnableWarnings");
			formattedWords.put(WbStoreProfile.VERB, "WbStoreProfile");
			formattedWords.put(WbDeleteProfile.VERB, "WbDeleteProfile");
			formattedWords.put(WbListProfiles.VERB, "WbListProfiles");
			formattedWords.put(WbDefinePk.VERB, "WbDefinePK");
			formattedWords.put(WbSelectBlob.VERB, "WbSelectBlob");
			formattedWords.put(WbRemoveVar.VERB, "WbVarDelete");
			formattedWords.put(WbProcSource.VERB, "WbProcSource");
			formattedWords.put(WbListProcedures.VERB, "WbListProcs");
			formattedWords.put(WbListTriggers.VERB, WbListTriggers.FORMATTED_VERB);
			formattedWords.put(WbTriggerSource.VERB, WbTriggerSource.FORMATTED_VERB);
			formattedWords.put(WbListCatalogs.VERB, "WbListDB");
			formattedWords.put(WbListCatalogs.VERB_ALTERNATE, "WbListCat");
			formattedWords.put(WbListSchemas.VERB, "WbListSchemas");
			formattedWords.put(WbGrepData.VERB, "WbGrepData");
			formattedWords.put(WbGrepSource.VERB, "WbGrepSource");
			formattedWords.put(WbFetchSize.VERB, "WbFetchSize");
			formattedWords.put(WbRunLB.VERB, "WbRunLB");
			formattedWords.put(WbIsolationLevel.VERB, "WbIsolationLevel");
			formattedWords.put(WbConnInfo.VERB, "WbConnInfo");
			formattedWords.put(WbSysExec.VERB, "WbSysExec");
			formattedWords.put(WbGenDrop.VERB, "WbGenerateDrop");
			formattedWords.put(WbSetProp.VERB, "WbSetProp");
		}
		return formattedWords;
	}

	public Collection<String> getCommands()
	{
		return Collections.unmodifiableSet(commands);
	}

	public boolean isWbCommand(String verb)
	{
		if (verb == null)
		{
			return false;
		}
		return commands.contains(verb.trim());
	}

	public String formatVerb(String verb)
	{
		if (verb == null)
		{
			return null;
		}
		String f = getFormattedVerbs().get(verb.trim());
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
		if (!verb.toLowerCase().startsWith("wb"))
		{
			return verb;
		}
		String s = "Wb" + Character.toUpperCase(verb.charAt(2)) + verb.substring(3).toLowerCase();
		return s;
	}
}
