/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.profiles.ProfileKey;

import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class CommandLineConnectionHandler
{
	private ArgumentParser cmdLine;
	private String profileNameArgument;
	private String profileGroupArgument;
	private String connectionArgument;

	public CommandLineConnectionHandler(ArgumentParser cmdLine, String profileNameArgument, String profileGroupArgument, String connectionArgument)
	{
		this.cmdLine = cmdLine;
		this.profileNameArgument = profileNameArgument;
		this.profileGroupArgument = profileGroupArgument;
		this.connectionArgument = connectionArgument;
	}

	public WbConnection getConnection(StatementRunnerResult result, WbConnection currentConnection, String baseDir, String id)
	{
    String globalValue = WbManager.getInstance().getCommandLine().getValue(connectionArgument, null);
		String desc = cmdLine.getValue(connectionArgument, globalValue);
		if (StringUtil.isNonBlank(desc))
		{
			try
			{
				ConnectionDescriptor parser = new ConnectionDescriptor(baseDir);
				ConnectionProfile profile = parser.parseDefinition(desc, currentConnection);
				if (profile != null)
				{
					return ConnectionMgr.getInstance().getConnection(profile, id);
				}
			}
			catch (InvalidConnectionDescriptor icd)
			{
				LogMgr.logError("CommandLineConnectionHandler.getConnection()", "Error connecting to database", icd);
				result.addErrorMessage(icd.getLocalizedMessage());
				return null;
			}
			catch (Exception e)
			{
				LogMgr.logError("CommandLineConnectionHandler.getConnection()", "Error connecting to database", e);
				result.addMessage(ResourceMgr.getFormattedString("ErrConnectDescriptor", desc));
				result.addErrorMessage(ExceptionUtil.getDisplay(e));
				return null;
			}
		}

		// No "short connection" specified, fallback to old profile key based connection.

		return getConnectionFromKey(currentConnection, result, id);
	}

	private WbConnection getConnectionFromKey(WbConnection currentConnection, StatementRunnerResult result, String id)
	{
		ProfileKey profileKey = getProfileKey();

		if (profileKey == null || (currentConnection != null && currentConnection.getProfile().isProfileForKey(profileKey)))
		{
			return currentConnection;
		}
		else
		{
			ConnectionProfile tprof = ConnectionMgr.getInstance().getProfile(profileKey);
			if (tprof == null)
			{
				result.addErrorMessageByKey("ErrProfileNotFound", profileKey.toString());
				return null;
			}

			try
			{
				return ConnectionMgr.getInstance().getConnection(profileKey, id);
			}
			catch (Exception e)
			{
				LogMgr.logError("CommandLineConnectionHandler.getConnectionFromKey()", "Error connecting to database", e);
				result.addMessage(ResourceMgr.getFormattedString("ErrConnectProfile", profileKey.toString()));
				result.addErrorMessage(ExceptionUtil.getDisplay(e));
				return null;
			}
		}
	}

	public ProfileKey getProfileKey()
	{
		String sourceProfile = cmdLine.getValue(profileNameArgument, WbManager.getInstance().getCommandLine().getValue(profileNameArgument));
		String sourceGroup = cmdLine.getValue(profileGroupArgument, WbManager.getInstance().getCommandLine().getValue(profileGroupArgument));
		ProfileKey key = null;
		if (sourceProfile != null)
		{
			key = new ProfileKey(sourceProfile, sourceGroup);
		}
		return key;
	}

}
