/*
 * OracleStatementHook.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

	private static final String wbSelectMarker = "select /* sqlwb_statistics */";

	private static final String retrieveStats =
			wbSelectMarker + " s.value, a.name, s.statistic# \n" +
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
		"'sorts (disk)'\n, " +
		"'db block changes'";

	// See: http://docs.oracle.com/cd/E11882_01/server.112/e26088/statements_9010.htm#SQLRF54985
	private static final Set<String> explainable = CollectionUtil.caseInsensitiveSet("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER");

	/**
	 * A list of SQL commands where no statistics should be shown.
	 */
	private static final Set<String> noStatistics = CollectionUtil.caseInsensitiveSet(
		WbFetchSize.VERB, WbAbout.VERB, WbConfirm.VERB, WbConnInfo.VERB, WbDefinePk.VERB, WbDefineVar.VERB,
		WbDeleteProfile.VERB, WbStoreProfile.VERB, WbDisconnect.VERB, WbDisplay.VERB,
		WbDisableOraOutput.VERB, WbEnableOraOutput.VERB, WbStartBatch.VERB, WbEndBatch.VERB, WbHelp.VERB,
		WbIsolationLevel.VERB, WbLoadPkMapping.VERB, WbListPkDef.VERB, WbMode.VERB, WbListVars.VERB, WbSetProp.VERB,
		WbSysProps.VERB, WbXslt.VERB, "SET");

	/**
	 * Stores the statistic values before the execution of the statement.
	 */
	private Map<String, Long> values;

	/** flag to indicate if the statisticsviews are available */
	private boolean statisticViewsAvailable;

	/** if true, autotrace is turned on */
	private boolean autotrace;

	private boolean traceOnly;
	private boolean showExecutionPlan;
	private boolean showRealPlan;
	private boolean showStatistics;

	private String lastExplainID;

	private PreparedStatement statisticsStmt;
	private final Object lock = new Object();

	private boolean useStatisticsHint = true;

	public OracleStatementHook()
	{
		useStatisticsHint = Settings.getInstance().getBoolProperty("workbench.db.oracle.realplan.usehint", true);
	}

	@Override
	public String preExec(StatementRunner runner, String sql)
	{
		if (!autotrace || !traceStatement(sql))
		{
			return sql;
		}

		lastExplainID = null;

		if (showRealPlan)
		{
			sql = adjustSql(sql);
			if (!useStatisticsHint)
			{
				enableStatistics(runner);
			}
		}

		if (showStatistics)
		{
			storeSessionStats(runner);
		}
		return sql;
	}


	@Override
	public void postExec(StatementRunner runner, String sql, StatementRunnerResult result)
	{
		checkRunnerSession(runner);
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
				result.setWarning(true);
			}
			else
			{
				DataStore stats = retrieveStatistics(runner, sql);
				if (stats != null)
				{
					int valueCol = getValueColIndex();
					int nameCol = getNameColIndex();

					long rows = runner.getResult().getRowsProcessed();
					int row = stats.addRow();
					stats.setValue(row, nameCol, "rows processed");
					stats.setValue(row, valueCol, Long.valueOf(rows));
					stats.setGeneratingSql(sql);
					stats.resetStatus();
					result.addDataStore(stats);
				}
			}
		}

		DataStore plan = null;
		if (showRealPlan && lastExplainID != null)
		{
			plan = retrieveRealExecutionPlan(runner, sql);
		}

		if (showExecutionPlan && plan == null)
		{
			plan = retrieveExecutionPlan(runner, sql);
		}

		if (plan != null)
		{
			result.addDataStore(plan);
		}
	}

	/**
	 * Change the SQL to be executed so that it can be found in V$SQL later.
	 *
	 * In order to be able to find the SQL statement in postExec() a comment with a unique marker string is added to the
	 * front of the statement so that it can correctly found in V$SQL and V$SQL_PLAN.
	 *
	 * Additionally the gather_plan_statistics hint is added to get detailed information
	 * in the generated plan.
	 *
	 * @param sql  the sql to execute
	 * @return the changed sql
	 * @see #getIDPrefix()
	 */
	private String adjustSql(String sql)
	{
		lastExplainID = UUID.randomUUID().toString();
		if (!useStatisticsHint)
		{
			return getIDPrefix() + "  " + sql;
		}
		SQLLexer lexer = new SQLLexer(sql);
		SQLToken verb = lexer.getNextToken(false, false);
		if (verb == null) return sql;
		int pos = verb.getCharEnd();
		sql = getIDPrefix() + "  " + sql.substring(0, pos) + " /*+ gather_plan_statistics */ " + sql.substring(pos + 1);
		return sql;
	}

	private String getIDPrefix() {
		if (lastExplainID == null)
		{
			return "";
		}
		return "/* wb$" + lastExplainID + " */";
	}

	private void storeSessionStats(StatementRunner runner)
	{
		WbConnection con = runner.getConnection();
		values = new HashMap<String, Long>(10);

		ResultSet rs = null;
		try
		{
			synchronized (lock)
			{
				prepareStatisticsStatement(con);
				rs = statisticsStmt.executeQuery();

				int valueCol = getValueColIndex();
				int nameCol = getNameColIndex();

				while (rs.next())
				{
					values.put(rs.getString(nameCol + 1), Long.valueOf(rs.getLong(valueCol + 1)));
				}
				statisticViewsAvailable = true;
			}
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
			SqlUtil.closeResult(rs);
		}
	}

	private String buildStatisticsQuery()
	{
		String stats = Settings.getInstance().getProperty("workbench.db.oracle.autotrace.statname", defaultStats);
		String query = null;
		if (showStatvalueFirst())
		{
			query = retrieveStats + " (" + stats + ")";
		}
		else
		{
			query = retrieveStats.replace("s.value, a.name", "a.name, s.value") + " (" + stats + ")";
		}
		return query;
	}

	private boolean showStatvalueFirst()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.autotrace.statistics.valuefirst", true);
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

	private DataStore retrieveRealExecutionPlan(StatementRunner runner, String sql)
	{
		WbConnection con = runner.getConnection();

		String findSql
			= wbSelectMarker + " sql.sql_id, sql.child_number \n" +
				"from v$sql sql \n" +
				"where sql_text like '" + getIDPrefix() + "%' \n" +
				"order by last_active_time desc";

		PreparedStatement planStatement = null;
		Statement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			String retrievePlan = "SELECT * FROM table(dbms_xplan.display_cursor(?, ?, 'BYTES COST NOTE ROWS ALLSTATS LAST'))";
			planStatement = con.getSqlConnection().prepareStatement(retrievePlan);

			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(findSql);
			if (rs.next())
			{
				String sqlId = rs.getString(1);
				int childNumber = rs.getInt(2);
				planStatement.setString(1, sqlId);
				planStatement.setInt(2, childNumber);

				SqlUtil.closeResult(rs);
				rs = planStatement.executeQuery();
				result = new DataStore(rs, true);
				result.setGeneratingSql(sql);
				result.setResultName("Execution plan");
				result.resetStatus();
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("OracleStatementHook.retrieveRealExecutionPlan()", "Could not retrieve real execution plan", ex);
		}
		finally
		{
			SqlUtil.closeStatement(planStatement);
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private void enableStatistics(StatementRunner runner)
	{
		WbConnection con = runner.getConnection();

		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			stmt.execute("alter session set statistics_level=all");
		}
		catch (SQLException ex)
		{
			LogMgr.logError("OracleStatementHook.preExec()", "Could not enable statistics level", ex);
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
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
			LogMgr.logError("OracleStatementHook.retrieveExecutionPlan()", "Could not retrieve session statistics", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private int getNameColIndex()
	{
		return showStatvalueFirst() ? 1 : 0;
	}

	private int getValueColIndex()
	{
		return showStatvalueFirst() ? 0 : 1;
	}

	private void prepareStatisticsStatement(WbConnection con)
		throws SQLException
	{
		if (statisticsStmt == null)
		{
			statisticsStmt = con.getSqlConnection().prepareStatement(buildStatisticsQuery());
		}
	}

	private DataStore retrieveStatistics(StatementRunner runner, String sql)
	{
		String verb = SqlUtil.getSqlVerb(sql);
		if (noStatistics.contains(verb))
		{
			return null;
		}

		WbConnection con = runner.getConnection();

		ResultSet rs = null;
		DataStore statValues = createResult();

		int valueCol = getValueColIndex();
		int nameCol = getNameColIndex();

		try
		{
			synchronized (lock)
			{
				prepareStatisticsStatement(con);
				rs = statisticsStmt.executeQuery();
				while (rs.next())
				{
					String statName = rs.getString(nameCol + 1);
					Long value = rs.getLong(valueCol + 1);
					if (statName != null && value != null)
					{
						Long startValue = values.get(statName);
						if (startValue != null)
						{
							int row = statValues.addRow();
							statValues.setValue(row, nameCol, statName);
							statValues.setValue(row, valueCol, (value - startValue));
						}
					}
				}
			}
			statValues.setResultName("Statistics");
		}
		catch (SQLException ex)
		{
			LogMgr.logError("OracleStatementHook.retrieveStatistics()", "Could not retrieve session statistics", ex);
		}
		finally
		{
			SqlUtil.closeResult(rs);
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

	private void checkRunnerSession(StatementRunner runner)
	{
		String trace = runner.getSessionAttribute("autotrace");
		if (trace == null)
		{
			if (autotrace)
			{
				// autotrace was turned off, so close the statistics statement.
				close();
			}
			autotrace = false;
			return;
		}
		Set<String> flags = CollectionUtil.caseInsensitiveSet();
		flags.addAll(StringUtil.stringToList(trace, ",", true, true, false, false));
		this.traceOnly = flags.contains("traceonly");
		this.autotrace = flags.contains("on") || traceOnly;
		this.showExecutionPlan =  flags.contains("explain") || (autotrace && flags.size() == 1);
		this.showStatistics = flags.contains("statistics")  || (autotrace && flags.size() == 1);
		this.showRealPlan =  autotrace && flags.contains("realplan");
		if (showRealPlan)
		{
			// enable "regular" explain plan as well in case retrieving the real plan doesn't work for some reason
			showExecutionPlan = showRealPlan;
		}
	}

	private DataStore createResult()
	{
		DataStore result = null;
		if (showStatvalueFirst())
		{
			String[] columnNames = new String[] { "VALUE", "STATISTIC" };
			int[] types = new int[] { Types.BIGINT, Types.VARCHAR };
			result = new DataStore(columnNames, types);
		}
		else
		{
			String[] columnNames = new String[] { "STATISTIC", "VALUE" };
			int[] types = new int[] { Types.VARCHAR, Types.BIGINT };
			result = new DataStore(columnNames, types);
		}
		return result;
	}

	@Override
	public void close()
	{
		SqlUtil.closeStatement(statisticsStmt);
		statisticsStmt = null;
	}

}
