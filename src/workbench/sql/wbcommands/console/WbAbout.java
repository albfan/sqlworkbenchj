/*
 * WbAbout.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.MemoryWatcher;
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
		long freeMem = (long)(MemoryWatcher.getFreeMemory() / (1024*1024) );
		long maxMem = (long)(MemoryWatcher.MAX_MEMORY / (1024*1024) );
		result.addMessage(ResourceMgr.getString("LblMemory") + " " + freeMem + "MB/" + maxMem + "MB");
		result.setSuccess();
		return result;
	}

}
