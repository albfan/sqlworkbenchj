package workbench.sql.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DdlCommand extends SqlCommand
{
	public static final SqlCommand CREATE = new DdlCommand("CREATE");
	public static final SqlCommand DROP = new DdlCommand("DROP");
	public static final SqlCommand ALTER = new DdlCommand("ALTER");
	public static final SqlCommand GRANT = new DdlCommand("GRANT");
	public static final SqlCommand REVOKE = new DdlCommand("REVOKE");

	public static final List DDL_COMMANDS = new ArrayList();
	
	static
	{
		DDL_COMMANDS.add(DROP);
		DDL_COMMANDS.add(CREATE);
		DDL_COMMANDS.add(ALTER);
		DDL_COMMANDS.add(GRANT);
		DDL_COMMANDS.add(REVOKE);
	}
	
	private String verb;

	public DdlCommand(String aVerb)
	{
		this.verb = aVerb;
	}
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		try
		{
			Statement stmt = aConnection.createStatement();
			stmt.execute(aSql);
			String msg = null;
			
			if ("DROP".equals(verb))
			{
				msg = ResourceMgr.getString("MsgDropSuccess");
			}
			else if ("CREATE".equals(verb))
			{
				msg = ResourceMgr.getString("MsgCreateSuccess");
			}
			else
			{
				msg = this.verb + " " + ResourceMgr.getString("MsgKnownStatementOK");
			}
			result.addMessage(msg);
			StringBuffer warnings = new StringBuffer();
			if (this.appendWarnings(aConnection, stmt, warnings))
			{
				result.addMessage(warnings.toString());
			}
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
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
	
	public String getVerb()
	{
		return verb;
	}
	
}
