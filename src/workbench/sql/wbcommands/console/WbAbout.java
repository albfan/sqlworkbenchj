/*
 * WbAbout.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import workbench.db.DbMetadata;
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


		String info = getConnectionInfo();
		if (info != null)
		{
			result.addMessageNewLine();
			String l = ResourceMgr.getString("LblConnInfo");
			result.addMessageByKey(l);
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
		StringBuilder content = new StringBuilder(100);

		try
		{
			DatabaseMetaData meta = currentConnection.getSqlConnection().getMetaData();
			DbMetadata wbmeta = currentConnection.getMetadata();

			content.append(ResourceMgr.getString("LblDbProductName") + ": " + wbmeta.getProductName() + "\n");
			content.append(ResourceMgr.getString("LblDbProductVersion") + ": " + currentConnection.getDatabaseVersion() + "\n");
			content.append(ResourceMgr.getString("LblDbProductInfo") + ": " + meta.getDatabaseProductVersion() + "\n");
			content.append(ResourceMgr.getString("LblDriverInfoName") + ": " + meta.getDriverName() + "\n");
			content.append(ResourceMgr.getString("LblDriverInfoClass") + ": " + currentConnection.getProfile().getDriverclass() + "\n");
			content.append(ResourceMgr.getString("LblDriverInfoVersion") + ": " + currentConnection.getDriverVersion() + "\n");
			content.append(ResourceMgr.getString("LblDbURL") + ": " + currentConnection.getUrl() + "\n");
			content.append(ResourceMgr.getString("LblUsername") + ": " + currentConnection.getCurrentUser() + "\n");
			String term = wbmeta.getSchemaTerm();
			String s = StringUtil.capitalize(term);
			String schema = currentConnection.getCurrentSchema();
			if (schema != null)
			{
				if (!"schema".equalsIgnoreCase(term))
				{
					s += " (" + ResourceMgr.getString("LblSchema") + ")";
				}
				content.append(s + ": " + schema + "\n");
			}

			term = wbmeta.getCatalogTerm();
			s = StringUtil.capitalize(term);
			String catalog = currentConnection.getCurrentCatalog();
			if (catalog != null)
			{
				if (!"catalog".equalsIgnoreCase(term))
				{
					s += " (" +  ResourceMgr.getString("LblCatalog") + ")";
				}
				content.append(s + ": " + catalog + "\n");
			}
			content.append("Workbench DBID: " + wbmeta.getDbId() + " \n");
		}
		catch (Exception e)
		{
			LogMgr.logError("WbAbout.getConnectionInfo()", "Error retrieving connection info", e);
			return null;
		}
		return content.toString();
	}
}
