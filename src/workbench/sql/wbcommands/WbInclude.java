/*
 * WbInclude.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 * @author  info@sql-workbench.net
 */
public class WbInclude
	extends SqlCommand
{
	public static final WbInclude INCLUDE_LONG = new WbInclude("WBINCLUDE");
	public static final WbInclude INCLUDE_SHORT = new WbInclude("@");
	public static final WbInclude INCLUDE_FB = new WbInclude("INPUT");
	
	private final String verb;
	private BatchRunner batchRunner;
	
	private WbInclude(String aVerb)
	{
		this.verb = aVerb;
	}
	
	public String getVerb() { return verb; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();
		
		String file = this.getFilename(aSql);
		try
		{
			batchRunner = new BatchRunner(file);
			batchRunner.setConnection(aConnection);
			batchRunner.setResultLogger(this.resultLogger);
			batchRunner.setVerboseLogging(this.runner.getVerboseLogging());
			batchRunner.setRowMonitor(this.rowMonitor);
			batchRunner.execute();
			result.setSuccess();
		}
		catch (Throwable th)
		{
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(th));
		}
		return result;
	}
	
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (batchRunner != null)
		{
			batchRunner.cancel();
		}
	}
	
	private String getFilename(String parameter)
	{
		String name = parameter.substring(this.verb.length()).trim();
		name = StringUtil.trimQuotes(name);
		return name;
	}
	
}
