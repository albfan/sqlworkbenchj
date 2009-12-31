/*
 * WbListPkDef.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.PkMapping;

/**
 *
 * @author Thomas Kellerer
 */
public class WbListPkDef
	extends SqlCommand
{
	public static final String VERB = "WBLISTPKDEF";
	public static final String FORMATTED_VERB = "WbListPkDef";

	public String getVerb() { return VERB; }

	protected boolean isConnectionRequired() { return false; }

	public StatementRunnerResult execute(String aSql)
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
