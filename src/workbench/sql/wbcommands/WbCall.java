/*
 * WbCall.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
 * Support for running stored procedures that have out parameters. For this 
 * command to work properly the JDBC driver needs to either implement 
 * CallableStatement.getParameterMetaData() correctly, or return proper information
 * about the columns of a procedure using DatabaseMetaData.getProcedureColumns()
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
	private List<Integer> refCursorIndex = null;
	
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

	private String getSqlToPrepare(String cleanSql, boolean funcCall)
	{
		if (funcCall) return "{ ? =  call " + cleanSql + "}";
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
		String realSql = getSqlToPrepare(cleanSql, false);

		try
		{
			ArrayList<String> parameterNames = null;
			refCursorIndex = null;
			
			result.addMessage(ResourceMgr.getString("MsgProcCallConverted") + " " + realSql);
			CallableStatement cstmt = currentConnection.getSqlConnection().prepareCall(realSql);
			this.currentStatement = cstmt;
			
			boolean hasParameters = (realSql.indexOf('?') > -1);
			
			Savepoint sp = null;
			if (hasParameters)
			{
				try
				{
					if (currentConnection.getDbSettings().useSavePointForDDL())
					{
						sp = currentConnection.setSavepoint();
					}
					parameterNames = checkParametersFromStatement(cstmt);
					currentConnection.releaseSavepoint(sp);
				}
				catch (Throwable e)
				{
					// Some drivers do not work properly if this happens, so 
					// we have to close and re-open the statement
					LogMgr.logWarning("WbCall.execute()", "Could not get parameters from statement!", e);
					SqlUtil.closeStatement(cstmt);
					currentConnection.rollback(sp);
				}
				finally
				{
					sp = null;
				}
			}
			
			// The called "procedure" could also be a function
			if (parameterNames == null || parameterNames.size() == 0)
			{
				// checkParametersFromDatabase will re-create the callable statement
				// and assign it to currentStatement
				// This is necessary to avoid having two statements open on the same
				// connection as some jdbc drivers do not like this
				try
				{
					if (currentConnection.getDbSettings().useSavePointForDDL())
					{
						sp = currentConnection.setSavepoint();
					}
					parameterNames = checkParametersFromDatabase(cleanSql);
					if (this.currentStatement != null)
					{
						cstmt = (CallableStatement)currentStatement;
					}
					currentConnection.releaseSavepoint(sp);
				}
				catch (Throwable e)
				{
					LogMgr.logError("WbCall.execute()", "Error during procedure check", e);
					currentConnection.rollback(sp);
				}
				finally
				{
					sp = null;
				}
			}
			
			boolean hasResult = (cstmt != null ? cstmt.execute() : false);
			result.setSuccess();
			
			int startColumn = 1;
			
			if (refCursorIndex != null)
			{
				for (Integer index : refCursorIndex)
				{
					try
					{
						ResultSet rs = (ResultSet)cstmt.getObject(index.intValue());
						
						// processResults will close the result set
						if (rs != null) processResults(result, true, rs);
						startColumn ++;
					}
					catch (Exception e)
					{
						result.addMessage(ExceptionUtil.getDisplay(e));
					}
				}
			}
			else
			{
				processResults(result, hasResult);
			}
			
			// Now process all single-value out parameters
			if (parameterNames != null && parameterNames.size() >= startColumn)
			{
				String[] cols = new String[]{"PARAMETER", "VALUE"};
				int[] types = new int[]{Types.VARCHAR, Types.VARCHAR};
				int[] sizes = new int[]{35, 35};

				DataStore resultData = new DataStore(cols, types, sizes);
				for (int i = startColumn; i <= parameterNames.size(); i++)
				{
					if (refCursorIndex != null && refCursorIndex.contains(new Integer(i))) continue;
					
					Object parmValue = cstmt.getObject(i);
					if (parmValue instanceof ResultSet)
					{
						processResults(result, true, (ResultSet)parmValue);
					}
					else
					{
						int row = resultData.addRow();
						resultData.setValue(row, 0, parameterNames.get(i - 1));
						resultData.setValue(row, 1, parmValue == null ? "NULL" : parmValue.toString());
					}
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

		boolean needFuncCall = meta.isPostgres() && returnsRefCursor(params);
		CallableStatement cstmt = currentConnection.getSqlConnection().prepareCall(getSqlToPrepare(sql, needFuncCall));
		this.currentStatement = cstmt;

		int parameterCount = 0;

		if (params.getRowCount() > 0)
		{
			parameterNames = new ArrayList<String>(params.getRowCount());
			for (int i = 0; i < params.getRowCount(); i++)
			{
				int dataType = params.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, -1);
				String typeName = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String resultType = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if (StringUtil.equalString(resultType, "OUT") || (needFuncCall && StringUtil.equalString(resultType, "RETURN")))
				{
					parameterCount++;
					parameterNames.add(params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME));
					if (isRefCursor(typeName))
					{
						// these parameters should not be added to the regular parameter list
						// as they have to be retrieved in a different manner.
						// type == -10 is Oracles CURSOR Datatype
						int newType = currentConnection.getDbSettings().getRefCursorDataType();
						if (newType != Integer.MIN_VALUE) dataType = newType;
						if (refCursorIndex == null) 
						{
							refCursorIndex = new LinkedList<Integer>();
						}
						refCursorIndex.add(new Integer(parameterCount));
					}
					cstmt.registerOutParameter(parameterCount, dataType);
				}
			}
		}
		
		return parameterNames;
	}

	private boolean isRefCursor(String type)
	{
		String dbType = currentConnection.getDbSettings().getRefCursorTypeName();
		return StringUtil.equalString(type, dbType);
	}

	private boolean returnsRefCursor(DataStore params)
	{
		// A function in Postgres that returns a refcursor
		// mus be called using { ? = call('procname')} in order
		// to be able to retrieve the result set from the refcursor
		for (int i=0; i < params.getRowCount(); i++)
		{
			String typeName = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
			String resultType = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
			if (isRefCursor(typeName) && "RETURN".equals(resultType)) return true;
		}
		return false;
	}
}
