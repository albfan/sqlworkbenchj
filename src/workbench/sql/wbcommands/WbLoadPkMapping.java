/*
 * WbLoadPkMapping.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbLoadPkMapping
	extends SqlCommand
{
	public final static String VERB = "WBLOADPKMAP";
	public final static String FORMATTED_VERB = "WbLoadPKMap";
	private ArgumentParser cmdLine;
	
	public WbLoadPkMapping()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("file");
	}

	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String sql = stripVerb(aSql);//aSql.trim().substring(this.getVerb().length()).trim();
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
			result.addMessage(ResourceMgr.getString("ErrPkLoadNoFile"));
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
