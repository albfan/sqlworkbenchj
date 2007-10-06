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
import workbench.util.StringUtil;

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

	private String getSqlToPrepare(String cleanSql)
	{
		return "{call " + cleanSql + "}";
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
		String realSql = getSqlToPrepare(cleanSql);
		
		result.addMessage(ResourceMgr.getString("MsgProcCallConverted") + " " + realSql);

		try
		{
			CallableStatement cstmt = currentConnection.getSqlConnection().prepareCall(realSql);

			ArrayList<String> parameterNames = null;
			try
			{
				parameterNames = checkParametersFromStatement(cstmt);
				this.currentStatement = cstmt;
			}
			catch (Throwable e)
			{
				// Some drivers do not work properly if this happens, so 
				// we have to close and re-open the statement
				LogMgr.logWarning("WbCall.execute()", "Could not get parameters from statement!", e);
				SqlUtil.closeStatement(cstmt);
			}

			if (parameterNames == null || parameterNames.size() == 0)
			{
				// checkParametersFromDatabase will re-create the callable statement
				// and assign it to currentStatement
				// This is necessary to avoid having two statements open on the same
				// connection as some jdbc drivers do not like this
				result.addMessage(ResourceMgr.getString("MsgProcCallNoStmtParms"));
				parameterNames = checkParametersFromDatabase(cleanSql);
				if (this.currentStatement != null)
				{
					cstmt = (CallableStatement)currentStatement;
				}
			}

			boolean hasResult = (cstmt != null ? cstmt.execute() : false);
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
		throws SQLException
	{
		ArrayList<String> parameterNames = null;
		
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
		
		return parameterNames;
	}

	private ArrayList<String> checkParametersFromDatabase(String sql)
		throws SQLException
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
		
		DataStore params = meta.getProcedureColumns(null, meta.adjustSchemaNameCase(schema),meta.adjustObjectnameCase(procname));

		CallableStatement cstmt = currentConnection.getSqlConnection().prepareCall(getSqlToPrepare(sql));
		this.currentStatement = cstmt;
		
		if (params.getRowCount() > 0)
		{
			parameterNames = new ArrayList<String>(params.getRowCount());
			int parameterCount = 0;
			for (int i = 0; i < params.getRowCount(); i++)
			{
				int dataType = params.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, -1);
				String resultType = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if (StringUtil.equalString(resultType, "OUT"))
				{
					parameterCount++;
					cstmt.registerOutParameter(parameterCount, dataType);
					parameterNames.add(params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME));
				}
			}
		}
		
		return parameterNames;
	}
}