/*
 * WbLoadPkMapping.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbLoadPkMapping
	extends SqlCommand
{
	public final static String VERB = "WBLOADPKMAP";
	public final static String FORMATTED_VERB = "WbLoadPKMap";

	public WbLoadPkMapping()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("file");
	}

	public String getVerb() { return VERB; }

	protected boolean isConnectionRequired() { return false; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String sql = SqlUtil.stripVerb(aSql);
		cmdLine.parse(sql);
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

		PkMapping.getInstance().loadMapping(file);
		String msg = ResourceMgr.getString("MsgPkMappingLoaded");
		File f = new File(file);
		msg = StringUtil.replace(msg, "%filename%", f.getAbsolutePath());
		result.addMessage(msg);
		result.addMessage("");

		String info = PkMapping.getInstance().getMappingAsText();
		if (info != null)
		{
			result.addMessage(info);
			result.addMessage(ResourceMgr.getString("MsgPkDefinitionsEnd"));
		}

		result.setSuccess();
		return result;
	}

}
