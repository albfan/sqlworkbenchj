/*
 * OracleStatementHook.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.StatementHook;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.wbcommands.*;
import workbench.sql.wbcommands.console.WbAbout;
import workbench.sql.wbcommands.console.WbDeleteProfile;
import workbench.sql.wbcommands.console.WbDisconnect;
import workbench.sql.wbcommands.console.WbDisplay;
import workbench.sql.wbcommands.console.WbStoreProfile;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A StatementHook that mimic's the autotrace feature from SQL*PLus.
 *
 * @author Thomas Kellerer
 */
public class OracleStatementHook
	implements StatementHook
{

	private static final String sql =
			"select a.name, s.value, s.statistic# \n" +
			"from v$sesstat s \n" +
			"  join v$statname a on a.statistic# = s.statistic# \n" +
			"where sid = userenv('SID') \n" +
			"and a.name in";

	/**
	 * A list of statistic names formatted to be used inside an IN clause.
	 */
	private static final String defaultStats =
		"'recursive calls', \n" +
		"'db block gets', \n" +
		"'consistent gets', \n" +
		"'physical reads', \n" +
		"'redo size', \n" +
		"'bytes sent via SQL*Net to client', \n" +
		"'bytes received via SQL*Net from client', \n" +
		"'SQL*Net roundtrips to/from client', \n" +
		"'sorts (memory)', \n" +
		"'sorts (disk)', 'db block changes'";

	// See: http://docs.oracle.com/cd/E11882_01/server.112/e26088/statements_9010.htm#SQLRF54985
	private static final Set<String> explainable = CollectionUtil.caseInsensitiveSet("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER");

	private static final Set<String> noStatistics = CollectionUtil.caseInsensitiveSet(
		WbFetchSize.VERB, WbAbout.VERB, WbConfirm.VERB, WbConnInfo.VERB, WbDefinePk.VERB, WbDefineVar.VERB,
		WbDeleteProfile.VERB, WbStoreProfile.VERB, WbDisconnect.VERB, WbDisplay.VERB,
		WbDisableOraOutput.VERB, WbEnableOraOutput.VERB, WbStartBatch.VERB, WbEndBatch.VERB, WbHelp.VERB,
		WbIsolationLevel.VERB, WbLoadPkMapping.VERB, WbListPkDef.VERB, WbMode.VERB, WbListVars.VERB, WbSetProp.VERB,
		WbSysProps.VERB, WbXslt.VERB);

	/**
	 * Stores the statistic values before the execution of the statement.
	 */
	private Map<String, Long> values;
	private boolean statisticViewsAvailable;
	private boolean autotrace;
	private boolean traceOnly;
	private boolean showExecutionPlan;
	private boolean showStatistics;

	@Override
	public void preExec(StatementRunner runner, String sql)
	{
		retrieveSessionState(runner);
		if (!autotrace || !traceStatement(sql))
		{
			return;
		}

		if (showStatistics)
		{
			storeSessionStats(runner);
		}
	}

	private void storeSessionStats(StatementRunner runner)
	{
		WbConnection con = runner.getConnection();
		values = new HashMap<String, Long>(10);
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(buildQuery());
			while (rs.next())
			{
				values.put(rs.getString(1), Long.valueOf(rs.getLong(2)));
			}
			statisticViewsAvailable = true;
			LogMgr.logDebug("OracleStatementHook.preExec()", "Retrieved " + values.size() + " statistic values");
		}
		catch (SQLException ex)
		{
			if (ex.getErrorCode() == 942)
			{
				statisticViewsAvailable = false;
			}
			LogMgr.logError("OracleStatementHook.preExec()", "Could not retrieve session statistics", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private String buildQuery()
	{
		String stats = Settings.getInstance().getProperty("workbench.db.oracle.autotrace.statname", defaultStats);
		String query = sql + " (" + stats + ")";
		LogMgr.logDebug("OracleStatementHook.buildQuery()", "Using SQL=" + query);
		return query;
	}

	@Override
	public void postExec(StatementRunner runner, String sql, StatementRunnerResult result)
	{
		if (!autotrace || !traceStatement(sql))
		{
			return;
		}

		// Retrieving statistics MUST be called before retrieving the execution plan!
		// Otherwise the statistics for retrieving the plan will be counted as well!
		if (showStatistics)
		{
			if (!statisticViewsAvailable)
			{
				result.addMessageByKey("ErrNoAutoTrace");
				return;
			}
			DataStore stats = retrieveStatistics(runner, sql);
			if (stats != null)
			{
				long rows = runner.getResult().getRowsProcessed();
				int row = stats.addRow();
				stats.setValue(row, 0, "rows processed");
				stats.setValue(row, 1, Long.valueOf(rows));
				stats.setGeneratingSql(sql);
				stats.resetStatus();
				result.addDataStore(stats);
			}
		}
		if (showExecutionPlan)
		{
			DataStore plan = retrieveExecutionPlan(runner, sql);
			if (plan != null)
			{
				result.addDataStore(plan);
			}
		}
	}

	private boolean traceStatement(String sql)
	{
		SQLLexer lexer = new SQLLexer(sql);
		SQLToken verb = lexer.getNextToken(false, false);
		if (verb == null) return false;
		String sqlVerb = verb.getContents();
		if ("SET".equalsIgnoreCase(sqlVerb))
		{
			return false;
		}
		return true;
	}

	private DataStore retrieveExecutionPlan(StatementRunner runner, String sql)
	{
		if (!canExplain(sql))
		{
			return null;
		}

		WbConnection con = runner.getConnection();

		String explainSql = "EXPLAIN PLAN FOR " + sql;
		String retrievePlan = "SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY())";

		Statement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = con.createStatementForQuery();
			stmt.execute(explainSql);
			rs = stmt.executeQuery(retrievePlan);
			result = new DataStore(rs, true);
			result.setGeneratingSql(sql);
			result.setResultName("Execution plan");
			result.resetStatus();
		}
		catch (SQLException ex)
		{
			LogMgr.logError("OracleStatementHook.preExec()", "Could not retrieve session statistics", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private DataStore retrieveStatistics(StatementRunner runner, String sql)
	{
		String verb = SqlUtil.getSqlVerb(sql);
		if (noStatistics.contains(verb))
		{
			return null;
		}
		WbConnection con = runner.getConnection();

		Statement stmt = null;
		ResultSet rs = null;
		DataStore statValues = createResult();
		try
		{
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(buildQuery());
			while (rs.next())
			{
				String statName = rs.getString(1);
				Long value = rs.getLong(2);
				if (statName != null && value != null)
				{
					Long startValue = values.get(statName);
					if (startValue != null)
					{
						int row = statValues.addRow();
						statValues.setValue(row, 0, statName);
						statValues.setValue(row, 1, (value - startValue));
					}
				}
			}
			statValues.setResultName("Statistics");
		}
		catch (SQLException ex)
		{
			LogMgr.logError("OracleStatementHook.preExec()", "Could not retrieve session statistics", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return statValues;
	}

	/**
	 * Check if the SQL can be EXPLAIN'ed.
	 *
	 * See: http://docs.oracle.com/cd/E11882_01/server.112/e26088/statements_9010.htm#SQLRF54985

	 * @param sql the sql to run
	 * @return true if EXPLAIN PLAN supports the statement.
	 */
	private boolean canExplain(String sql)
	{
		SQLLexer lexer = new SQLLexer(sql);
		SQLToken verb = lexer.getNextToken(false, false);
		if (verb == null) return false;

		if (!explainable.contains(verb.getContents())) return false;

		String sqlVerb = verb.getContents();
		if ("CREATE".equalsIgnoreCase(sqlVerb))
		{
			SQLToken type = lexer.getNextToken(false, false);
			if (type == null) return false;
			String typeName = type.getContents();
			return typeName.equalsIgnoreCase("TABLE") || typeName.equalsIgnoreCase("INDEX");
		}
		if ("ALTER".equalsIgnoreCase(sqlVerb))
		{
			SQLToken token = lexer.getNextToken(false, false);
			if (token == null) return false;
			token = lexer.getNextToken(false, false);
			if (token == null) return false;
			return "REBUILD".equalsIgnoreCase(token.getContents());
		}
		return true;
	}

	@Override
	public boolean displayResults()
	{
		if (!autotrace) return true;
		return !traceOnly;
	}

	@Override
	public boolean fetchResults()
	{
		if (!autotrace) return true;
		if (traceOnly)
		{
			return showStatistics;
		}
		return true;
	}

	private void retrieveSessionState(StatementRunner runner)
	{
		String trace = runner.getSessionAttribute("autotrace");
		if (trace == null)
		{
			autotrace = false;
			return;
		}
		Set<String> flags = CollectionUtil.caseInsensitiveSet();
		flags.addAll(StringUtil.stringToList(trace, ",", true, true, false, false));
		this.traceOnly = flags.contains("traceonly");
		this.autotrace = flags.contains("on") || traceOnly;
		this.showExecutionPlan =  flags.contains("explain") || (autotrace && flags.size() == 1);
		this.showStatistics = flags.contains("statistics")  || (autotrace && flags.size() == 1);
	}

	private DataStore createResult()
	{
		String[] columnNames = new String[] {"STATISTIC", "VALUE" };
		int[] types = new int[] {Types.VARCHAR, Types.BIGINT };
		DataStore result = new DataStore(columnNames, types);
		return result;
	}

}
