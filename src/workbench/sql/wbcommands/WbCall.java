/*
 * WbCall.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.db.ProcedureReader;
import workbench.gui.preparedstatement.ParameterEditor;
import workbench.log.LogMgr;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.preparedstatement.ParameterDefinition;
import workbench.sql.preparedstatement.StatementParameters;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Support for running stored procedures that have OUT parameters. For this
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
	private List<Integer> refCursorIndex = null;

	// Stores all parameters that need an input
	private List<ParameterDefinition> inputParameters = new ArrayList<ParameterDefinition>(5);

	@Override
	public String getVerb()
	{
		return VERB;
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
	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);

		String cleanSql = SqlUtil.stripVerb(aSql);
		String realSql = getSqlToPrepare(cleanSql, false);

		this.inputParameters.clear();

		List<ParameterDefinition> outParameters = null;

		try
		{
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
					outParameters = checkParametersFromStatement(cstmt);
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
			if (outParameters == null || outParameters.size() == 0)
			{
				try
				{
					if (currentConnection.getDbSettings().useSavePointForDDL())
					{
						sp = currentConnection.setSavepoint();
					}

					outParameters = checkParametersFromDatabase(cleanSql);

					// checkParametersFromDatabase will re-create the callable statement
					// and assign it to currentStatement
					// This is necessary to avoid having two statements open on the same
					// connection as some jdbc drivers do not like this
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

			if (hasParameters && !WbManager.getInstance().isBatchMode() && this.inputParameters.size() > 0)
			{
				StatementParameters input = new StatementParameters(this.inputParameters);
				boolean ok = ParameterEditor.showParameterDialog(input);
				if (!ok)
				{
					result.addMessage(ResourceMgr.getString("MsgStatementCancelled"));
					result.setFailure();
					return result;
				}

				for (int i=0; i < inputParameters.size(); i++)
				{
					int type = inputParameters.get(i).getType();
					Object value = inputParameters.get(i).getValue();
					cstmt.setObject(i + 1, value, type);
				}
			}

			boolean hasResult = (cstmt != null ? cstmt.execute() : false);
			result.setSuccess();

			if (refCursorIndex != null)
			{
				for (Integer index : refCursorIndex)
				{
					try
					{
						ResultSet rs = (ResultSet)cstmt.getObject(index.intValue());

						// processResults will close the result set
						if (rs != null) processResults(result, true, rs);
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
			if (outParameters != null && outParameters.size() > 0)
			{
				String[] cols = new String[]{"PARAMETER", "VALUE"};
				int[] types = new int[]{Types.VARCHAR, Types.VARCHAR};
				int[] sizes = new int[]{35, 35};

				DataStore resultData = new DataStore(cols, types, sizes);
				ParameterDefinition.sortByIndex(outParameters);

				for (ParameterDefinition def : outParameters)
				{
					if (refCursorIndex != null && refCursorIndex.contains(Integer.valueOf(def.getIndex()))) continue;

					Object parmValue = cstmt.getObject(def.getIndex());
					if (parmValue instanceof ResultSet)
					{
						processResults(result, true, (ResultSet)parmValue);
					}
					else
					{
						int row = resultData.addRow();
						resultData.setValue(row, 0, def.getParameterName());
						resultData.setValue(row, 1, parmValue == null ? "NULL" : parmValue.toString());
					}
				}
				result.addDataStore(resultData);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCall.execute()", "Error calling stored procedure", e);
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
		}
		finally
		{
			done();
		}

		return result;
	}

	@Override
	public void done()
	{
		super.done();
		if (this.refCursorIndex != null) this.refCursorIndex.clear();
		this.refCursorIndex = null;
		this.inputParameters.clear();
	}

	private List<ParameterDefinition> checkParametersFromStatement(CallableStatement cstmt)
		throws SQLException
	{
		ArrayList<ParameterDefinition> parameterNames = null;

		ParameterMetaData parmData = cstmt.getParameterMetaData();
		if (parmData != null)
		{
			parameterNames = new ArrayList<ParameterDefinition>();

			for (int i = 0; i < parmData.getParameterCount(); i++)
			{
				int mode = parmData.getParameterMode(i + 1);
				int type = parmData.getParameterType(i + 1);

				ParameterDefinition def = new ParameterDefinition(i + 1, type);
				inputParameters.add(def);

				if (mode == ParameterMetaData.parameterModeOut ||
						mode == ParameterMetaData.parameterModeInOut)
				{
					cstmt.registerOutParameter(i + 1, type);
					parameterNames.add(def);
				}
			}
		}

		return parameterNames;
	}

	private List<ParameterDefinition> checkParametersFromDatabase(String sql)
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
		ArrayList<ParameterDefinition> parameterNames = null;

		DataStore params = meta.getProcedureReader().getProcedureColumns(null, meta.adjustSchemaNameCase(schema), meta.adjustObjectnameCase(procname));

		boolean needFuncCall = meta.isPostgres() && returnsRefCursor(params);
		CallableStatement cstmt = currentConnection.getSqlConnection().prepareCall(getSqlToPrepare(sql, needFuncCall));
		this.currentStatement = cstmt;

		int parameterCount = 0;

		if (params.getRowCount() > 0)
		{
			parameterNames = new ArrayList<ParameterDefinition>(params.getRowCount());
			for (int i = 0; i < params.getRowCount(); i++)
			{
				int dataType = params.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, -1);

				ParameterDefinition def = new ParameterDefinition(i + 1, dataType);
				inputParameters.add(def);

				String typeName = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String resultType = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);

				if (resultType != null && resultType.endsWith("OUT") || (needFuncCall && StringUtil.equalString(resultType, "RETURN")))
				{
					parameterCount++;
					def.setParameterName(params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME));
					parameterNames.add(def);
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
						refCursorIndex.add(Integer.valueOf(parameterCount));
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
