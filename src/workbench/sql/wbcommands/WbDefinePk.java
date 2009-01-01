/*
 * WbDefinePk.java
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

import java.sql.SQLException;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.PkMapping;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbDefinePk
	extends SqlCommand
{
	public static final String VERB = "WBDEFINEPK";

	public String getVerb() { return VERB; }

	protected boolean isConnectionRequired() { return false; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String sql = SqlUtil.stripVerb(aSql);

		WbStringTokenizer tok = new WbStringTokenizer("=", true, "\"'", false);
		tok.setSourceString(sql);
		String columns = null;
		String table = null;

		if (tok.hasMoreTokens()) table = tok.nextToken();

		if (table == null)
		{
			result.addMessage(ResourceMgr.getString("ErrPkDefWrongParameter"));
			result.setFailure();
			return result;
		}

		if (tok.hasMoreTokens()) columns = tok.nextToken();
		String msg = null;
		if (columns == null)
		{
			PkMapping.getInstance().removeMapping(currentConnection, table);
			msg = ResourceMgr.getString("MsgPkDefinitionRemoved");
		}
		else
		{
			PkMapping.getInstance().addMapping(table, columns);
			msg = ResourceMgr.getString("MsgPkDefinitionAdded");
		}
		msg = StringUtil.replace(msg, "%table%", table);
		result.setSuccess();
		result.addMessage(msg);
		return result;
	}
}
