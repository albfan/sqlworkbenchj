/*
 * WbSavePkMapping.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.PkMapping;
import workbench.util.ArgumentParser;
import workbench.util.FileDialogUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbSavePkMapping
	extends SqlCommand
{
	public static final String VERB = "WBSAVEPKMAP";
	public static final String FORMATTED_VERB = "WbSavePKMap";
	
	public WbSavePkMapping()
	{
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
			file = StringUtil.replace(file, FileDialogUtil.CONFIG_DIR_KEY, Settings.getInstance().getConfigDir());
		}
		
		if (file == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrPkSaveNoFile"));
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
