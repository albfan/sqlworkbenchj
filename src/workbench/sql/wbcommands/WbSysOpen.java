/*
 * WbExec.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.log.LogMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.*;

/**
 * A workbench command to call an operating system program (or command)
 *
 * @author Thomas Kellerer
 */
public class WbSysOpen
	extends SqlCommand
{
	public static final String VERB = "WBSYSOPEN";

	public WbSysOpen()
	{
		super();
		cmdLine = new ArgumentParser();
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);
		String doc = getCommandLine(sql);
		if (StringUtil.isBlank(doc))
		{
			result.setFailure();
			result.addMessageByKey("ErrSysOpenNoParm");
			return result;
		}

		try
		{
			Desktop.getDesktop().open(new File(doc));
			result.setSuccess();
		}
		catch (IOException io)
		{
			result.setFailure();
			result.addMessage(io.getLocalizedMessage());
		}
		return result;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}
}
