package workbench.sql.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;

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
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			this.currentStatement = aConnection.createStatement();
			this.currentStatement.execute(aSql);
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
			if (this.appendWarnings(aConnection, this.currentStatement, warnings))
			{
				result.addMessage(warnings.toString());
        this.addExtendErrorInfo(aConnection, aSql, result);
			}
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));
      this.addExtendErrorInfo(aConnection, aSql, result);
			result.setFailure();
		}
		
		return result;
	}
	

  private final static List TYPES;
  static
  {
    TYPES = new ArrayList();
    TYPES.add("TRIGGER");
    TYPES.add("PROCEDURE");
    TYPES.add("FUNCTION");
    TYPES.add("PACKAGE");
  }
  
  private boolean addExtendErrorInfo(WbConnection aConnection, String sql, StatementRunnerResult result)
  {
    String cleanSql = SqlUtil.makeCleanSql(sql, false).toUpperCase();
    String sqlverb = SqlUtil.getSqlVerb(cleanSql);
    if (!"CREATE".equals(sqlverb)) return false;
    String type = null;
    
    StringTokenizer tok = new StringTokenizer(cleanSql, " ");
    String word = null;
    String name = null;
    boolean nextTokenIsName = false;
    while (tok.hasMoreTokens())
    {
      word = tok.nextToken();
      if (nextTokenIsName)
      {
        name = word;
        break;
      }
      if (TYPES.contains(word))
      {
        type = word;
        nextTokenIsName = true;
      }
    }
		if (type == null || name == null) return false;
		
    String msg = aConnection.getMetadata().getExtendedErrorInfo(null, type, name);
		if (msg != null && msg.length() > 0)
		{
			result.addMessage(msg);
			return true;
		}
		else
		{
			return false;
		}
		
  }
  
	public String getVerb()
	{
		return verb;
	}
	
}
