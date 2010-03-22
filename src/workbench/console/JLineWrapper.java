/*
 * JLineWrapper
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.console;

import java.io.IOException;
import jline.ConsoleReader;
import jline.History;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class JLineWrapper
	implements WbConsoleReader
{
	private ConsoleReader reader;

	public JLineWrapper()
		throws IOException
	{
		reader = new ConsoleReader();
		reader.setUseHistory(true);
		reader.setUsePagination(false);
		reader.setBellEnabled(false);
		History history = reader.getHistory();
		if (history != null)
		{
			String filename = Settings.getInstance().getProperty("workbench.console.history.file", null);
			WbFile historyfile = null;
			if (StringUtil.isBlank(filename))
			{
				historyfile = new WbFile(Settings.getInstance().getConfigDir(), ".sqlworkbench_history");
			}
			else
			{
				historyfile = new WbFile(filename);
			}
			LogMgr.logDebug("JLineWrapper.<init>", "Using history file: " + historyfile.getFullPath());
			history.setHistoryFile(historyfile);
		}
	}

	@Override
	public String readPassword(String prompt)
	{
		try
		{
			return reader.readLine(prompt, Character.valueOf('*'));
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	public String readLine(String prompt)
	{
		try
		{
			return reader.readLine(prompt);
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	public void shutdown()
	{
		History h = reader.getHistory();
		if (h != null)
		{
			FileUtil.closeQuietely(h.getOutput());
		}
	}
}
