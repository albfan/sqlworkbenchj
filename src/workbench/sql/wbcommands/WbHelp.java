/*
 * WbHelp.java
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
import java.util.Collection;
import java.util.ResourceBundle;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbHelp
	extends SqlCommand
{
	public static final String VERB = "WBHELP";

	public WbHelp()
	{
		super();
	}

	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		Collection<String> commands = this.runner.getAllWbCommands();
		StringBuffer msg = new StringBuffer(commands.size() * 25);
		ResourceBundle bundle = ResourceBundle.getBundle("language/cmdhelp", Settings.getInstance().getLanguage());
		commands.remove("DESC"); // only the "long" Verb is needed
		if (currentConnection != null && !currentConnection.getMetadata().isOracle())
		{
			commands.remove("ENABLEOUT");
			commands.remove("DISABLEOUT");
		}
		
		for (String verb : commands)
		{
			msg.append(verb);
			try
			{
				String text = bundle.getString(verb);
				msg.append(" - ");
				msg.append(text);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("WbHelp.execute()", "Error getting command short help from ResourceBundle", e);
			}
			msg.append('\n');
		}
		result.addMessage(msg);
		result.setSuccess();
		return result;
	}

}
