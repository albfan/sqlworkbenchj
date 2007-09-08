/*
 * WbOraExecute.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import workbench.db.DbMetadata;
import workbench.db.ProcedureReader;
import workbench.log.LogMgr;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.storage.DataStore;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;

/**
 * Support for running stored procedures that have out or in/out parameters
 *
 * @author  support@sql-workbench.net
 */
public class WbCall
	extends SqlCommand
{
	public static final String EXEC_VERB_SHORT = "EXEC";
	public static final String EXEC_VERB_LONG = "EXECUTE";
	public static final String VERB = "WBCALL";
	private String realVerb = null;
	private boolean useParameterMetaData = true;

	public WbCall()
	{
		this(VERB);
	}

	protected WbCall(String v)
	{
		this.realVerb = v;
	}

	public String getVerb()
	{
		return realVerb;
	}

	/**
	 * Converts the passed sql to an Oracle compliant JDBC call and
	 * runs the statement.
	 */
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);

		String cleanSql = SqlUtil.stripVerb(aSql);
		String realSql = "{call " + cleanSql + "}";

		result.addMessage(ResourceMgr.getString("MsgProcCallConverted") + " " + realSql);

		try
		{
			CallableStatement cstmt = currentConnection.getSqlConnection().prepareCall(realSql);

			ArrayList<String> parameterNames = checkParametersFromStatement(cstmt);

			this.currentStatement = cstmt;

			if (parameterNames == null || parameterNames.size() == 0)
			{
				parameterNames = checkParametersFromDatabase(cleanSql, cstmt);
			}

			boolean hasResult = cstmt.execute();
			result.setSuccess();
			
			processResults(result, hasResult);

			if (parameterNames != null && parameterNames.size() > 0)
			{
				String[] cols = new String[]{"PARAMETER", "VALUE"};
				int[] types = new int[]{Types.VARCHAR, Types.VARCHAR};
				int[] sizes = new int[]{35, 35};

				DataStore resultData = new DataStore(cols, types, sizes);
				for (int i = 1; i <= parameterNames.size(); i++)
				{
					Object parmValue = cstmt.getObject(i);
					int row = resultData.addRow();
					resultData.setValue(row, 0, parameterNames.get(i - 1));
					resultData.setValue(row, 1, parmValue == null ? "NULL" : parmValue.toString());
				}
				result.addDataStore(resultData);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbOraExcecute.execute()", "Error calling stored procedure", e);
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

	private ArrayList<String> checkParametersFromStatement(CallableStatement cstmt)
	{
		ArrayList<String> parameterNames = null;
		try
		{
			// First we try to get the parameters from the CallableStatement
			ParameterMetaData parmData = cstmt.getParameterMetaData();
			if (parmData != null)
			{
				parameterNames = new ArrayList<String>();
				int parameterCount = 0;
				for (int i = 0; i < parmData.getParameterCount(); i++)
				{
					int type = parmData.getParameterType(i + 1);
					if (type == ParameterMetaData.parameterModeOut || 
							type == ParameterMetaData.parameterModeInOut)
					{
						parameterCount++;
						cstmt.registerOutParameter(parameterCount, type);
						parameterNames.add("$" + NumberStringCache.getNumberString(i + 1));
					}
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logDebug("WbcCall.execute()", "getParameterMetaData not supported", e);
		}
		return parameterNames;
	}

	private ArrayList<String> checkParametersFromDatabase(String sql, CallableStatement cstmt)
	{
		// Try to get the parameter information directly from the procedure definition
		SQLLexer l = new SQLLexer(sql);
		SQLToken t = l.getNextToken(false, false);

		// the first token could also be a schema/user name
		String schema = null;
		String procname = null;

		try
		{
			SQLToken n = l.getNextToken(false, false);
			if (n != null && n.getContents().equals("."))
			{
				n = l.getNextToken();
				procname = (n == null ? "" : n.getContents());
				schema = (t == null ? "" : t.getContents());
			}
			else
			{
				procname = (t == null ? "" : t.getContents());
			}
		}
		catch (IOException e)
		{
			LogMgr.logError("WbCall.checkParametersFromDatabase", "Error checking SQL", e);
			return null;
		}
		
		DbMetadata meta = this.currentConnection.getMetadata();
		ArrayList<String> parameterNames = null;
		
		try
		{
			DataStore params = meta.getProcedureColumns(null, meta.adjustSchemaNameCase(schema),meta.adjustObjectnameCase(procname));

			if (params.getRowCount() > 0)
			{
				parameterNames = new ArrayList<String>(params.getRowCount());
				int parameterCount = 0;
				for (int i = 0; i < params.getRowCount(); i++)
				{
					int dataType = params.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, -1);
					String resultType = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
					if (resultType != null && resultType.endsWith("OUT"))
					{
						parameterCount++;
						cstmt.registerOutParameter(parameterCount, dataType);
						parameterNames.add(params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME));
					}
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbCall.checkParametersFromDatabase", "Error checking procedure columns", e);
			return null;
		}
		return parameterNames;
	}
}