/*
 * WbListProfiles.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;
import workbench.sql.macros.MacroManager;
import workbench.sql.macros.MacroStorage;
import workbench.sql.wbcommands.CommonArgs;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;


/**
 * Create a new macro.
 *
 * @author Thomas Kellerer
 */
public class WbDefineMacro
	extends SqlCommand
{
	public static final String VERB = "WbDefineMacro";

	public static final String ARG_NAME = "name";
	public static final String ARG_TEXT = "text";
	public static final String ARG_GROUP = "group";
	public static final String ARG_EXPAND = "autoExpand";

	private int macroClientId = MacroManager.DEFAULT_STORAGE;

	public WbDefineMacro()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_NAME);
		cmdLine.addArgument(ARG_TEXT);
		cmdLine.addArgument(CommonArgs.ARG_FILE, ArgumentType.Filename);
		cmdLine.addArgument(ARG_GROUP);
		cmdLine.addArgument(ARG_EXPAND, ArgumentType.BoolSwitch);
		CommonArgs.addEncodingParameter(cmdLine);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	public void setMacroClientId(int clientId)
	{
		this.macroClientId = clientId;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();

		cmdLine.parse(getCommandLine(sql));

		if (!cmdLine.hasArguments())
		{
			result.setFailure();
			result.addMessageByKey("ErrDefineMacro");
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, null);
			return result;
		}
		String macroText = cmdLine.getValue(ARG_TEXT);
		WbFile sourceFile = evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_FILE));
		String groupName = StringUtil.trimQuotes(cmdLine.getValue(ARG_GROUP));
		String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING, null);
		String macroName = cmdLine.getValue(ARG_NAME);
		boolean expand = cmdLine.getBoolean(ARG_EXPAND);

		if (StringUtil.isBlank(macroName))
		{
			result.setFailure();
			result.addMessageByKey("ErrMacroNameReq");
			return result;
		}

		MacroStorage storage = MacroManager.getInstance().getMacros(macroClientId);
		List<MacroGroup> groups = storage.getGroups();

		MacroGroup groupToUse = null;

		if (sourceFile != null && sourceFile.exists())
		{
			macroText = FileUtil.readFile(sourceFile, encoding);
		}
		else
		{
			macroText = StringUtil.trimQuotes(macroText);
		}

		String msg =  null;

		MacroDefinition def = storage.getMacro(macroName);
		if (def == null)
		{
			def = new MacroDefinition(macroName, macroText);
			msg = ResourceMgr.getFormattedString("MsgMacroAdded", macroName);
		}
		else
		{
			def.setText(macroText);
			msg = ResourceMgr.getFormattedString("MsgMacroRedef", macroName);
		}

		def.setExpandWhileTyping(expand);

		if (StringUtil.isNonEmpty(groupName))
		{
			for (MacroGroup grp : groups)
			{
				if (grp.getName().equalsIgnoreCase(groupName))
				{
					groupToUse = grp;
				}
			}
		}
		else if (groups.isEmpty())
		{
			groupName = ResourceMgr.getString("LblDefGroup");
		}
		else
		{
			groupToUse = groups.get(0);
		}

		if (groupToUse == null)
		{
			groupToUse = new MacroGroup(groupName);
		}

		storage.addMacro(groupToUse, def);

		MacroManager.getInstance().save();

		result.addMessage(msg);
		result.setSuccess();

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
