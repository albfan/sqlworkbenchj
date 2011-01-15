/*
 * WbEndBatch.java
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

import java.sql.SQLException;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * @author  Thomas Kellerer
 */
public class WbEndBatch
	extends SqlCommand
{
	public static final String VERB = "WBENDBATCH";

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();
		result.addMessage(ResourceMgr.getString("MsgJdbcBatchProcessingEnded"));
		return result;
	}

}
