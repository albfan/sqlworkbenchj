/*
 * WbOraShow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.JdbcUtils;
import workbench.db.oracle.OracleErrorInformationReader;

import workbench.storage.DataStore;

import workbench.sql.ErrorDescriptor;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.DdlObjectInfo;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An implementation of various SQL*Plus "show" commands.
 *
 * Currently supported commands:
 * <ul>
 *    <li>parameters</li>
 *    <li>user</li>
 *    <li>errors</li>
 *    <li>sga</li>
 *    <li>recyclebin</li>
 *    <li>autocommit</li>
 * </ul>
 * @author Thomas Kellerer
 */
public class WbOraShow
	extends SqlCommand
{
	public static final String VERB = "SHOW";

	private final long ONE_KB = 1024;
	private final long ONE_MB = ONE_KB * 1024;

	private final Set<String> types = CollectionUtil.caseInsensitiveSet(
		"FUNCTION", "PROCEDURE", "PACKAGE", "PACKAGE BODY", "TRIGGER", "VIEW", "TYPE", "TYPE BODY", "DIMENSION",
		"JAVA SOURCE", "JAVA CLASS");

	private Map<String, String> propertyUnits = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

  public static final String SHOW_PDBS_QUERY = "select con_id, name, open_mode, restricted from gv$pdbs";


	public WbOraShow()
	{
		propertyUnits.put("result_cache_max_size", "kb");
		propertyUnits.put("sga_max_size", "mb");
		propertyUnits.put("sga_target", "mb");
		propertyUnits.put("memory_max_target", "mb");
		propertyUnits.put("memory_target", "mb");
		propertyUnits.put("db_recovery_file_dest_size", "mb");
		propertyUnits.put("db_recycle_cache_size", "mb");
		propertyUnits.put("db_cache_size", "mb");
		propertyUnits.put("result_cache_max_size", "mb");
		propertyUnits.put("java_pool_size", "mb");
		propertyUnits.put("pga_aggregate_target", "mb");
	}


	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		String clean = getCommandLine(sql);
		SQLLexer lexer = SQLLexerFactory.createLexer(currentConnection, clean);
		SQLToken token = lexer.getNextToken(false, false);
		if (token == null)
		{
			result.addMessage(ResourceMgr.getString("ErrOraShow"));
			result.setFailure();
			return result;
		}
		String verb = token.getText().toLowerCase();
		if (verb.startsWith("parameter"))
		{
			SQLToken name = lexer.getNextToken(false, false);
			String parm = null;
			if (name != null)
			{
				parm = clean.substring(name.getCharBegin());
			}
			return getParameterValues(parm);
		}
		else if (verb.equals("sga"))
		{
			return getSGAInfo(true);
		}
		else if (verb.equals("sgainfo"))
		{
			return getSGAInfo(false);
		}
		else if (verb.equals("logsource"))
		{
			return getLogSource();
		}
		else if (verb.equals("recyclebin"))
		{
			return showRecycleBin();
		}
		else if (verb.equals("user"))
		{
			result.addMessage("USER is " + currentConnection.getCurrentUser());
		}
		else if (verb.equals("appinfo"))
		{
			return getAppInfo(sql);
		}
		else if (verb.equals("autocommit"))
		{
			if (currentConnection.getAutoCommit())
			{
				result.addMessage("autocommit ON");
			}
			else
			{
				result.addMessage("autocommit OFF");
			}
		}
		else if (verb.startsWith("error"))
		{
			return getErrors(lexer, sql);
		}
		else if (verb.equals("edition"))
		{
			return showUserEnv("SESSION_EDITION_NAME", "EDITION");
		}
		else if (verb.equals("pdbs") && JdbcUtils.hasMinimumServerVersion(currentConnection, "12.0"))
		{
			return showPdbs();
		}
    else if (verb.startsWith("con_") && JdbcUtils.hasMinimumServerVersion(currentConnection, "12.0"))
    {
      return showUserEnv(verb);
    }
		else
		{
			result.addMessage(ResourceMgr.getString("ErrOraShow"));
			result.setFailure();
		}
		return result;
	}

  private StatementRunnerResult showUserEnv(String attribute)
  {
    return showUserEnv(attribute, attribute);
  }

  private StatementRunnerResult showUserEnv(String attribute, String displayName)
  {
    String query = "select sys_context('userenv', '" + attribute.toUpperCase()+ "') as " + displayName + " from dual";

    StatementRunnerResult result = new StatementRunnerResult("SHOW " + attribute);
    DataStore ds = SqlUtil.getResult(currentConnection, query);
    result.addDataStore(ds);

    return result;
  }

  private StatementRunnerResult showPdbs()
  {
    StatementRunnerResult result = new StatementRunnerResult("SHOW pdbs");

    try
    {
      DataStore ds = SqlUtil.getResultData(currentConnection, SHOW_PDBS_QUERY, false);
      ds.setResultName("PDBS");
      result.addDataStore(ds);
    }
    catch (SQLException ex)
    {
      result.setFailure();
      result.addMessage(ex.getMessage());
    }
    return result;
  }

	private StatementRunnerResult showRecycleBin()
	{
		StatementRunnerResult result = new StatementRunnerResult("SHOW RECYCLEBIN");
		String sql =
				"SELECT original_name as \"ORIGINAL NAME\", \n" +
				"       object_name as \"RECYCLEBIN NAME\", \n" +
				"       type as \"OBJECT TYPE\", \n" +
				"       droptime as \"DROP TIME\" \n" +
				"FROM user_recyclebin \n" +
				"WHERE can_undrop = 'YES' \n" +
				"ORDER BY original_name, \n" +
				"         droptime desc, \n" +
				"         object_name";

		ResultSet rs = null;

		try
		{
			currentStatement = this.currentConnection.createStatementForQuery();
			rs = currentStatement.executeQuery(sql);
			processResults(result, true, rs);
			if (result.hasDataStores() && result.getDataStores().get(0).getRowCount() == 0)
			{
				result.clear();
				result.addMessageByKey("MsgRecyclebinEmpty");
			}
			result.setSuccess();
		}
		catch (SQLException ex)
		{
			result.setFailure();
			result.addMessage(ex.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, currentStatement);
		}
		return result;
	}

	private StatementRunnerResult getErrors(SQLLexer lexer, String sql)
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		SQLToken token = lexer.getNextToken(false, false);

		String schema = null;
		String object = null;
		String type = null;

		if (token != null && types.contains(token.getText()))
		{
			type = token.getContents();
			token = lexer.getNextToken(false, false);
		}

		if (token != null)
		{
			String v = token.getText();
			int pos = v.indexOf('.');

			if (pos > 0)
			{
				schema = v.substring(0, pos - 1);
				object = v.substring(pos);
			}
			else
			{
				object = v;
			}
		}

		if (object == null)
		{
			DdlObjectInfo info = currentConnection.getLastDdlObjectInfo();
			if (info != null)
			{
				object = info.getObjectName();
				type = info.getObjectType();
			}
		}

		ErrorDescriptor errors = null;

		if (object != null)
		{
			OracleErrorInformationReader reader = new OracleErrorInformationReader(currentConnection);
			errors  = reader.getErrorInfo(schema, object, type, true);
		}

		if (errors != null)
		{
			result.addMessage(errors.getErrorMessage());
		}
		else
		{
			result.addMessage(ResourceMgr.getString("TxtOraNoErr"));
		}
		return result;
	}

	private StatementRunnerResult getAppInfo(String sql)
	{
		String query = "SELECT module FROM v$session WHERE audsid = USERENV('SESSIONID')";
		Statement stmt = null;
		ResultSet rs = null;
		StatementRunnerResult result = new StatementRunnerResult(sql);

		try
		{
			stmt = this.currentConnection.createStatementForQuery();
			rs = stmt.executeQuery(query);
			if (rs.next())
			{
				String appInfo = rs.getString(1);
				if (appInfo == null)
				{
					result.addMessage("appinfo is OFF");
				}
				else
				{
					result.addMessage("appinfo is \"" + appInfo + "\"");
				}
			}
		}
		catch (SQLException ex)
		{
			result.setFailure();
			result.addMessage(ex.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private StatementRunnerResult getParameterValues(String parameter)
	{
		boolean hasDisplayValue = JdbcUtils.hasMinimumServerVersion(currentConnection, "10.0");
		boolean useDisplayValue = Settings.getInstance().getBoolProperty("workbench.db.oracle.showparameter.display_value", hasDisplayValue);

		String query =
			"select name,  \n" +
			"       case type \n" +
			"         when 1 then 'boolean'  \n" +
			"         when 2 then 'string' \n" +
			"         when 3 then 'integer' \n" +
			"         when 4 then 'parameter file' \n" +
			"         when 5 then 'reserved' \n" +
			"         when 6 then 'big integer' \n" +
			"         else to_char(type) \n" +
			"       end as type,  \n" +
			"       " + (useDisplayValue ? "display_value" : "value") + " as value, \n" +
			"       description, \n"  +
			"       update_comment \n" +
			"from v$parameter \n ";
		ResultSet rs = null;

		List<String> names = StringUtil.stringToList(parameter, ",", true, true, false, false);

		if (names.size() > 0)
		{
			query +="where";

			for (int i=0; i < names.size(); i++)
			{
				if (i > 0) query += "  or";
				query += " name like lower('%" + names.get(i) + "%') \n";
			}
		}
		query += "order by name";
		StatementRunnerResult result = new StatementRunnerResult(query);

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("WbOraShow.getParameterValues()", "Retrieving system parameters using:\n" + query);
		}

		try
		{
			// processResults needs currentStatement
			currentStatement = this.currentConnection.createStatementForQuery();
			rs = currentStatement.executeQuery(query);
			processResults(result, true, rs);
			if (result.hasDataStores())
			{
				DataStore ds = result.getDataStores().get(0);
				for (int row=0; row < ds.getRowCount(); row++)
				{
					String property = ds.getValueAsString(row, 0);
					String value = ds.getValueAsString(row, 2);
					if (!useDisplayValue)
					{
						String formatted = formatMemorySize(property, value);
						if (formatted != null)
						{
							ds.setValue(row, 2, formatted);
						}
					}
				}
				ds.resetStatus();
			}
		}
		catch (SQLException ex)
		{
			result.setFailure();
			result.addMessage(ex.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, currentStatement);
		}
		return result;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	protected String formatMemorySize(String property, String value)
	{
		String unit = propertyUnits.get(property);
		if (unit == null) return null;
		try
		{
			long lvalue = Long.valueOf(value);
			if (lvalue == 0) return null;

			if ("kb".equals(unit))
			{
				return Long.toString(roundToKb(lvalue)) + "K";
			}
			if ("mb".equals(unit))
			{
				return Long.toString(roundToMb(lvalue)) + "M";
			}
		}
		catch (NumberFormatException nfe)
		{
		}
		return null;
	}

	protected StatementRunnerResult getLogSource()
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String sql =
			"select destination \n" +
			"from V$ARCHIVE_DEST \n "+
			"where status = 'VALID'";

		Statement stmt = null;
		ResultSet rs = null;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("WbOraShow.getLogSource()", "Using SQL: " + sql);
		}

		try
		{
			stmt = this.currentConnection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			DataStore ds = new DataStore(new String[] {"LOGSOURCE", "VALUE"}, new int[] {Types.VARCHAR, Types.VARCHAR});
			while (rs.next())
			{
				String dest = rs.getString(1);
				if ("USE_DB_RECOVERY_FILE_DEST".equals(dest))
				{
					dest = "";
				}
				int row = ds.addRow();
				ds.setValue(row, 0, "LOGSOURCE");
				ds.setValue(row, 1, dest);
			}
			ds.setGeneratingSql("show logsource");
			ds.setResultName("LOGSOURCE");
			ds.resetStatus();
			result.addDataStore(ds);
			result.setSuccess();
		}
		catch (SQLException ex)
		{
			LogMgr.logError("WbOraShow.getSGAInfo()", "Could not retrieve SGA info", ex);
			result.setFailure();
			result.addMessage(ex.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	protected StatementRunnerResult getSGAInfo(boolean sqlPlusMode)
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String sql = null;
		if (sqlPlusMode)
		{
			sql =
				"select 'Total System Global Area' as \"Memory\", \n" +
				"       sum(VALUE) as \"Value\", \n" +
				"       'bytes' as unit \n" +
				"from V$SGA \n" +
				"union all \n" +
				"select NAME, \n" +
				"       VALUE, \n" +
				"       'bytes' \n" +
				"from V$SGA";
		}
		else
		{
			sql = "select * from v$sgainfo";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("WbOraShow.getSGAInfo()", "Using SQL: " + sql);
		}

		try
		{
			DataStore ds = SqlUtil.getResultData(currentConnection, sql, false);
			ds.setGeneratingSql(sqlPlusMode ? "show sga" : "show sgainfo");
			ds.setResultName("SGA Size");
			result.addDataStore(ds);
			result.setSuccess();
		}
		catch (SQLException ex)
		{
			LogMgr.logError("WbOraShow.getSGAInfo()", "Could not retrieve SGA info", ex);
			result.setFailure();
			result.addMessage(ex.getMessage());
		}
		return result;
	}

	private long roundToKb(long input)
	{
		if (input < ONE_KB) return input;
		return input / ONE_KB;
	}

	private long roundToMb(long input)
	{
		if (input < ONE_MB) return input;
		return input / ONE_MB;
	}

}
