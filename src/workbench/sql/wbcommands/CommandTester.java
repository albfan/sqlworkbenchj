/*
 * CommandTester.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import workbench.sql.wbcommands.console.WbAbout;
import workbench.sql.wbcommands.console.WbCreateProfile;
import workbench.sql.wbcommands.console.WbDefineDriver;
import workbench.sql.wbcommands.console.WbDefineMacro;
import workbench.sql.wbcommands.console.WbDeleteMacro;
import workbench.sql.wbcommands.console.WbDeleteProfile;
import workbench.sql.wbcommands.console.WbDisconnect;
import workbench.sql.wbcommands.console.WbDisplay;
import workbench.sql.wbcommands.console.WbListDrivers;
import workbench.sql.wbcommands.console.WbListMacros;
import workbench.sql.wbcommands.console.WbListProfiles;
import workbench.sql.wbcommands.console.WbRun;
import workbench.sql.wbcommands.console.WbStoreProfile;

import workbench.util.CaseInsensitiveComparator;

/**
 * A class to test whether a given SQL Verb is an internal
 * Workbench command.
 *
 * This is used by the SqlFormatter, because the verbs for WbXXXX commands should be be not formatted in uppercase.
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

	private Map<String, String> commands;

	public CommandTester()
	{
		commands = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
		putVerb(WbCall.VERB);
		putVerb(WbConfirm.VERB);
		putVerb(WbCopy.VERB);
		putVerb(WbDataDiff.VERB);
		putVerb(WbDefinePk.VERB);
		putVerb(WbDefineVar.VERB);
		putVerb(WbDeleteProfile.VERB);
		putVerb(WbDescribeObject.VERB);
		putVerb(WbDescribeObject.VERB_LONG);
		putVerb(WbDisableOraOutput.VERB);
		putVerb(WbDisplay.VERB);
		putVerb(WbEnableOraOutput.VERB);
		putVerb(WbEndBatch.VERB);
		putVerb(WbExport.VERB);
		putVerb(WbFeedback.VERB);
		putVerb(WbImport.VERB);
		putVerb(WbInclude.VERB);
		putVerb(WbListPkDef.VERB);
		putVerb(WbListVars.VERB);
		putVerb(WbList.VERB);
		putVerb(WbListProcedures.VERB);
		putVerb(WbListCatalogs.VERB);
		putVerb(WbListCatalogs.VERB_ALTERNATE);
		putVerb(WbListSchemas.VERB);
		putVerb(WbLoadPkMapping.VERB);
		putVerb(WbRemoveVar.VERB);
		putVerb(WbSavePkMapping.VERB);
		putVerb(WbSchemaDiff.VERB);
		putVerb(WbSchemaReport.VERB);
		putVerb(WbSelectBlob.VERB);
		putVerb(WbStartBatch.VERB);
		putVerb(WbXslt.VERB);
		putVerb(WbConnect.VERB);
		putVerb(WbDisconnect.VERB);
		putVerb(WbHideWarnings.VERB);
		putVerb(WbListProfiles.VERB);
		putVerb(WbStoreProfile.VERB);
		putVerb(WbHelp.VERB);
		putVerb(WbProcSource.VERB);
		putVerb(WbRun.VERB);
		putVerb(WbListTriggers.VERB);
		putVerb(WbTriggerSource.VERB);
		putVerb(WbTableSource.VERB);
		putVerb(WbViewSource.VERB);
		putVerb(WbGrepSource.VERB);
		putVerb(WbGrepData.VERB);
		putVerb(WbMode.VERB);
		putVerb(WbFetchSize.VERB);
		putVerb(WbAbout.VERB);
		putVerb(WbRunLB.VERB);
		putVerb(WbIsolationLevel.VERB);
		putVerb(WbConnInfo.VERB);
		putVerb(WbSysExec.VERB);
		putVerb(WbShowProps.VERB);
		putVerb(WbShowProps.ALTERNATE_VERB);
		putVerb(WbOraShow.VERB);
		putVerb(WbGenDrop.VERB);
		putVerb(WbSetProp.VERB);
		putVerb(WbSetProp.ALTERNATE_VERB);
		putVerb(WbGenerateScript.VERB);
		putVerb(WbSysOpen.VERB);
		putVerb(WbGenDelete.VERB);
		putVerb(WbGenInsert.VERB);
		putVerb(WbEcho.VERB);
		putVerb(WbHistory.VERB);
		putVerb(WbDefineMacro.VERB);
		putVerb(WbDeleteMacro.VERB);
		putVerb(WbListMacros.VERB);
		putVerb(WbListIndexes.VERB);
		putVerb(WbRowCount.VERB);
		putVerb(WbDefineDriver.VERB);
		putVerb(WbListDrivers.VERB);
		putVerb(WbCreateProfile.VERB);
		putVerb(WbShowEncoding.VERB);
	}

	private void putVerb(String verb)
	{
		commands.put(verb, verb);
	}

	public Collection<String> getCommands()
	{
		return Collections.unmodifiableSet(commands.keySet());
	}

	public boolean isWbCommand(String verb)
	{
		if (verb == null)
		{
			return false;
		}
		return commands.containsKey(verb.trim());
	}

	public String formatVerb(String verb)
	{
		return commands.get(verb);
	}
}
