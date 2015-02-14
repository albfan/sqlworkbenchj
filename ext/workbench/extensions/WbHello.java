/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.extensions;

import java.sql.SQLException;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;


/**
 *
 * @author Thomas Kellerer
 */
public class WbHello
	extends SqlCommand
{
	public static final String VERB = "WbHello";

	public WbHello()
	{
	}

	@Override
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
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);
		result.addMessage("Hello, world");
		result.setSuccess();
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
