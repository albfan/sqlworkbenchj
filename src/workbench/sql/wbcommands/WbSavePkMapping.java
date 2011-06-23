/*
 * WbSavePkMapping.java
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

import java.io.File;
import java.sql.SQLException;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.PkMapping;
import workbench.util.ArgumentParser;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbSavePkMapping
	extends SqlCommand
{
	public static final String VERB = "WBSAVEPKMAP";
	public static final String FORMATTED_VERB = "WbSavePKMap";

	public WbSavePkMapping()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("file");
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
	public StatementRunnerResult execute(final String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		cmdLine.parse(getCommandLine(sql));
		String file = cmdLine.getValue("file");
		if (file == null)
		{
			file = Settings.getInstance().getPKMappingFilename();
		}
		else
		{
			WbFile cd = new WbFile(Settings.getInstance().getConfigDir());
			file = StringUtil.replace(file, FileDialogUtil.CONFIG_DIR_KEY, cd.getFullPath());
		}

		if (file == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrPkDefNoFile"));
			return result;
		}

		PkMapping.getInstance().saveMapping(file);
		String msg = ResourceMgr.getString("MsgPkMappingSaved");
		File f = new File(file);
		msg = StringUtil.replace(msg, "%filename%", f.getAbsolutePath());
		result.addMessage(msg);
		result.setSuccess();
		return result;
	}

}
