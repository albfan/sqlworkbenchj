package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.LineTokenizer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbOraExecute extends SqlCommand
{
	public static final WbOraExecute EXEC = new WbOraExecute("EXEC");
	public static final WbOraExecute EXECUTE = new WbOraExecute("EXECUTE");
	
	private String sqlcommand; 
	
	private WbOraExecute(String aVerb)
	{
		this.sqlcommand = aVerb;
	}
	
	public String getVerb() { return this.sqlcommand; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		LineTokenizer tok = new LineTokenizer(aSql.trim(), " ");
		String verb = tok.nextToken(); 
		if (!this.sqlcommand.equalsIgnoreCase(verb)) throw new WbException("Wrong syntax. " + sqlcommand + " expected!");
		
		String upper = aSql.toUpperCase();
		int startpos = upper.indexOf(this.sqlcommand.toUpperCase());
		String realSql = "";
		if (startpos > 0)
		{
			realSql = aSql.substring(0, startpos - 1);
		}
		realSql = realSql + "{call " + aSql.substring(startpos + this.sqlcommand.length() + 1) + "}";
		
		result.addMessage(ResourceMgr.getString("MsgProcCallConverted") + " " + realSql);
		
		try
		{
			this.currentStatement = aConnection.createStatement();
			boolean hasResult = this.currentStatement.execute(realSql);
			DataStore ds = null;
			if (hasResult) 
			{
				ResultSet rs = this.currentStatement.getResultSet();
				ds = new DataStore(rs, aConnection);
				result.addDataStore(ds);
			}
			
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
		}
		finally
		{
			this.done();
		}
		
		return result;
	}	
	
}
