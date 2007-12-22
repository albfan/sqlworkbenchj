/*
 * WbConfirm.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 * A SQL Statement to halt a script and confirm execution by the user
 * 
 * @author support@sql-workbench.net
 */
public class WbConfirm
	extends SqlCommand
{
	public static final String VERB = "WBCONFIRM";
	
	public WbConfirm()
	{
		this.isUpdatingCommand = false;
	}

	public String getVerb() { return VERB; }
	
	protected boolean isConnectionRequired() { return false; }
	
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setStopScript(false);
		result.setSuccess();

		if (WbManager.getInstance().isBatchMode()) return result;
		
		String msg = getCommandLine(sql);
		
		if (StringUtil.isEmptyString(msg))
		{
			msg = ResourceMgr.getString("MsgConfirmContinue");
		}
		
		boolean continueScript = WbSwingUtilities.getYesNo(WbManager.getInstance().getCurrentWindow(), StringUtil.trimQuotes(msg));
		if (!continueScript)
		{
			result.setStopScript(true);
		}
		
		return result;
	}
	
}
