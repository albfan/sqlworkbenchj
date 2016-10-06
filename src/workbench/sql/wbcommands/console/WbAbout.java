/*
 * WbAbout.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import workbench.db.ConnectionMgr;
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
	public static final String VERB = "WbAbout";

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
  public boolean isWbCommand()
  {
    return true;
  }

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.addMessage(ResourceMgr.getBuildInfo());
		result.addMessage(ResourceMgr.getJavaInfo());
    result.addMessage("Java Home: " +  System.getProperty("java.home"));

		WbFile f = Settings.getInstance().getConfigFile();
		String s = ResourceMgr.getFormattedString("LblSettingsLocation", f.getFullPath());
		result.addMessage(s);

		s = ResourceMgr.getFormattedString("LblProfilesLocation", ConnectionMgr.getInstance().getProfilesPath());
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
