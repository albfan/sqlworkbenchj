/*
 * WbListPkDef.java
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

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.PkMapping;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbListPkDef
	extends SqlCommand
{
	public static final String VERB = "WBLISTPKDEF";
	public static final String FORMATTED_VERB = "WblistPkDef";
	
	public WbListPkDef()
	{
	}
	
	public String getVerb() { return VERB; }
	
	protected boolean isConnectionRequired() { return false; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		
		result.setSuccess();
		
		String info = PkMapping.getInstance().getMappingAsText();
		if (info != null)
		{
			result.addMessage(ResourceMgr.getString("MsgPkDefinitions"));
			result.addMessage("");
			result.addMessage(info);
			result.addMessage(ResourceMgr.getString("MsgPkDefinitionsEnd"));
		}
		else
		{
			result.addMessage(ResourceMgr.getString("MsgPkDefinitionsEmpty"));
		}
		return result;
	}	
}
