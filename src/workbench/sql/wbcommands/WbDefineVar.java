package workbench.sql.wbcommands;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.SqlParameterPool;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbDefineVar extends SqlCommand
{
	public static final WbDefineVar DEFINE_LONG = new WbDefineVar("WBVARDEFINE");
	public static final WbDefineVar DEFINE_SHORT = new WbDefineVar("WBVARDEF");

	private String verb = null;
	private WbDefineVar(String aVerb)
	{
		this.verb = aVerb;
	}

	public String getVerb() { return verb; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(getVerb());
		String sql = aSql.trim().substring(this.getVerb().length()).trim();

		String msg = null;

		WbStringTokenizer tok = new WbStringTokenizer("=", true, "\"'", false);
		tok.setSourceString(sql);
		String value = null;
		String var = null;

		if (tok.hasMoreTokens()) var = tok.nextToken();

		if (var == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorVarDefWrongParameter"));
			result.setFailure();
			return result;
		}

		if (tok.hasMoreTokens()) value = tok.nextToken();

		if ("-file".equalsIgnoreCase(var))
		{
			if (value != null)
			{
				try
				{
					File f = new File(value);
					SqlParameterPool.getInstance().readFromFile(value);
					msg = ResourceMgr.getString("MsgVarDefFileLoaded");
					msg = StringUtil.replace(msg, "%file%", f.getAbsolutePath());
					result.addMessage(msg);
					result.setSuccess();
				}
				catch (Exception e)
				{
					File f = new File(value);
					LogMgr.logError("WbDefineVar.execute()", "Error reading definition file: " + value, e);
					msg = ResourceMgr.getString("ErrorReadingVarDefFile");
					msg = StringUtil.replace(msg, "%file%", f.getAbsolutePath());
					msg = msg + " " + ExceptionUtil.getDisplay(e);
					result.addMessage(msg);
					result.setFailure();
				}
			}
			else
			{
				result.addMessage(ResourceMgr.getString("ErrorReadingVarDefFile"));
				result.setFailure();
			}
			return result;
		}

		result.setSuccess();

		if (value != null)
		{
			if (value.startsWith("@"))
			{
				String valueSql = value.substring(1);
				value = this.evaluateSql(aConnection, valueSql);
			}
			msg = ResourceMgr.getString("MsgVarDefVariableDefined");
			try
			{
				SqlParameterPool.getInstance().setParameterValue(var, value);
				msg = msg.replaceAll("%var%", var);
				msg = msg.replaceAll("%value%", value);
				msg = msg.replaceAll("%varname%", StringUtil.quoteRegexMeta(SqlParameterPool.getInstance().buildVarName(var, false)));
			}
			catch (IllegalArgumentException e)
			{
				result.setFailure();
				msg = ResourceMgr.getString("ErrorVarDefWrongName");
			}

		}
		else
		{
			msg = ResourceMgr.getString("MsgVarDefVariableRemoved");
			msg = msg.replaceAll("%var%", var);
		}

		result.addMessage(msg);

		return result;
	}
	
	/**
	 *	Return the result of the given SQL string and return 
	 *	the value of the first column of the first row 
	 *	as a string value.
	 *
	 *	If the SQL gives an error, an empty string will be returned
	 */
	private String evaluateSql(WbConnection conn, String sql)
	{
		ResultSet rs = null;
		String result = StringUtil.EMPTY_STRING;
		try
		{
			this.currentStatement = conn.createStatement();
			this.currentStatement.setMaxRows(1);
			if (sql.endsWith(";"))
			{
				sql = sql.substring(0, sql.length() - 1);
			}
			rs = this.currentStatement.executeQuery(sql);
			if (rs.next())
			{
				result = rs.getString(1);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbDefineVar.evaluateSql()", "Error executing SQL: " + sql, e);
			result = StringUtil.EMPTY_STRING;
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
		}
		
		return result;
	}

}