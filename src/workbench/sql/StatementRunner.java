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
	private SqlCommand currentConsumer;
	
	private int maxRows;
	
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
		
		sql = new WbSpoolCommand();
		cmdDispatch.put(sql.getVerb(), sql);
		
		cmdDispatch.put(WbListCatalogs.LISTCAT.getVerb(), WbListCatalogs.LISTCAT);
		cmdDispatch.put(WbListCatalogs.LISTDB.getVerb(), WbListCatalogs.LISTDB);
		
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
	
	public void runStatement(String aSql, int maxRows)
		throws SQLException, WbException
	{
		String cleanSql = SqlUtil.makeCleanSql(aSql, false);
		if (cleanSql == null || cleanSql.length() == 0)
		{
			if (this.result == null)
			{
				this.result = new StatementRunnerResult("");
			}
			this.result.clear();
			this.result.setSuccess();
			return;
		}
		
		String verb = SqlUtil.getSqlVerb(cleanSql).toUpperCase();
		
		// clean up the result from the last statement
		if (this.result != null) this.result.clear();
		
		this.currentCommand = (SqlCommand)this.cmdDispatch.get(verb);
		
		// if not mapping is found use the default implementation
		if (this.currentCommand == null) 
		{
			this.currentCommand = (SqlCommand)this.cmdDispatch.get("*");
		}
		this.currentCommand.setMaxRows(maxRows);
		this.result = this.currentCommand.execute(this.dbConnection, aSql);
		
		if (this.currentCommand.isResultSetConsumer())
		{
			this.currentConsumer = this.currentCommand;
		}
		else
		{
			if (this.currentConsumer != null)
			{
				this.currentConsumer.consumeResult(this.result);
			}
			this.currentConsumer = null;
		}
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
	
	public void done()
	{
		if (this.result != null) this.result.clear();
		if (this.currentCommand != null && this.currentCommand != this.currentConsumer) 
		{
			this.currentCommand.done();
		}
	}
	
}
