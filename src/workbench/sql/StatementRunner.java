/*
 * StatementRunner.java
 *
 * Created on 16. November 2002, 13:43
 */

package workbench.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.sql.StatementRunnerResult;
import workbench.sql.commands.*;
import workbench.sql.wbcommands.*;
import workbench.util.SqlUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class StatementRunner
{
	private WbConnection dbConnection;
	private StatementRunnerResult result;
	
	private HashMap cmdDispatch;
	
	private SqlCommand currentCommand;
	
	public StatementRunner()
	{
		cmdDispatch = new HashMap(10);
		cmdDispatch.put("*", new SqlCommand());
		
		SqlCommand sql = new WbListTables();
		cmdDispatch.put(sql.getVerb(), sql);
		
		sql = new WbListProcedures();
		cmdDispatch.put(sql.getVerb(), sql);
		
		sql = new WbDescribeTable();
		cmdDispatch.put(sql.getVerb(), sql);
		
		sql = new WbEnableOraOutput();
		cmdDispatch.put(sql.getVerb(), sql);
		
		sql = new SelectCommand();
		cmdDispatch.put(sql.getVerb(), sql);
		
		cmdDispatch.put(SingleVerbCommand.COMMIT.getVerb(), SingleVerbCommand.COMMIT);
		cmdDispatch.put(SingleVerbCommand.ROLLBACK.getVerb(), SingleVerbCommand.ROLLBACK);
		
		cmdDispatch.put(UpdatingCommand.DELETE.getVerb(), UpdatingCommand.DELETE);
		cmdDispatch.put(UpdatingCommand.INSERT.getVerb(), UpdatingCommand.INSERT);
		cmdDispatch.put(UpdatingCommand.UPDATE.getVerb(), UpdatingCommand.UPDATE);
		
		for (int i=0; i < DdlCommand.DDL_COMMANDS.size(); i ++)
		{
			sql = (SqlCommand)DdlCommand.DDL_COMMANDS.get(i);
			cmdDispatch.put(sql.getVerb(), sql);
		}
	}

	public void setConnection(WbConnection aConn)
	{
		this.dbConnection = aConn;
	}
	
	public StatementRunnerResult getResult()
	{
		return this.result;
	}
	
	public void runStatement(String aSql)
		throws SQLException, WbException
	{
		String cleanSql = SqlUtil.makeCleanSql(aSql, false);
		String verb = SqlUtil.getSqlVerb(cleanSql);
		
		// clean up the result from the last statement
		if (this.result != null) this.result.clear();
		
		this.currentCommand = (SqlCommand)this.cmdDispatch.get(verb);
		
		// if not mapping is found use the default implementation
		if (this.currentCommand == null) 
		{
			this.currentCommand = (SqlCommand)this.cmdDispatch.get("*");
		}
		
		this.result = this.currentCommand.execute(this.dbConnection, aSql);
	}

	public void cancel()
	{
		try
		{
			if (this.currentCommand != null)
			{
				this.currentCommand.cancel();
			}
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
	}
	
	private StatementRunnerResult executeSql(String aSql)
	{
		StatementRunnerResult result = new StatementRunnerResult();
		return result;
	}
}
