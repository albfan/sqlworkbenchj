/*
 * WbAbout.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
import workbench.db.ConnectionInfoBuilder;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.MemoryWatcher;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbAbout
	extends SqlCommand
{
	public static final String VERB = "WBABOUT";

	public WbAbout()
	{
		super();
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

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.addMessage(ResourceMgr.getBuildInfo());
		result.addMessage(ResourceMgr.getString("TxtJavaVersion") + ": " + System.getProperty("java.runtime.version"));

		WbFile f = Settings.getInstance().getConfigFile();
		String s = ResourceMgr.getFormattedString("LblSettingsLocation", f.getFullPath());
		result.addMessage(s);

		f = LogMgr.getLogfile();
		result.addMessage("Logfile: " + (f == null ? "": f.getFullPath()));
		long freeMem = MemoryWatcher.getFreeMemory() / (1024*1024);
		long maxMem = MemoryWatcher.MAX_MEMORY / (1024*1024);
		result.addMessage(ResourceMgr.getString("LblMemory") + " " + freeMem + "MB/" + maxMem + "MB");


		String info = getConnectionInfo();
		if (info != null)
		{
			result.addMessageNewLine();
			String l = ResourceMgr.getString("LblConnInfo");
			result.addMessage(l);
			String line = StringUtil.padRight("", l.length(), '-');
			result.addMessage(line);
			result.addMessage(info);
		}
		result.setSuccess();
		return result;
	}

	private String getConnectionInfo()
	{
		if (this.currentConnection == null) return null;

		ConnectionInfoBuilder builder = new ConnectionInfoBuilder();
		boolean busy = currentConnection.isBusy();
		String result = null;
		try
		{
			// ConnectionBuilder will not call certain functions if the connection is busy to avoid deadlocks.
			// But we know that the connection isn't "really" busy while this is running
			// So we can safely reset the flag so that the ConnectionInfoBuilder can return everything
			currentConnection.setBusy(false);
			result = builder.getPlainTextDisplay(currentConnection, 0);
		}
		finally
		{
			currentConnection.setBusy(busy);
		}
		return result;
	}
}
