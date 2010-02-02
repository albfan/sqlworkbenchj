/*
 * WbCall.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
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
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Support for running stored procedures that have OUT parameters. For this
 * command to work properly the JDBC driver needs to either implement
 * CallableStatement.getParameterMetaData() correctly, or return proper information
 * about the columns of a procedure using DatabaseMetaData.getProcedureColumns()
 *
 * @author Thomas Kellerer
 */
public class WbCall
	extends SqlCommand
{
	public static final String EXEC_VERB_SHORT = "EXEC";
	public static final String EXEC_VERB_LONG = "EXECUTE";
	public static final String VERB = "WBCALL";
	private Map<Integer, ParameterDefinition> refCursor = null;

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
			refCursor = null;

			result.addMessage(ResourceMgr.getString("MsgProcCallConverted") + " " + realSql);
			CallableStatement cstmt = currentConnection.getSqlConnection().prepareCall(realSql);
			this.currentStatement = cstmt;

			boolean hasParameters = (realSql.indexOf('?') > -1);
			boolean namesAvailable = false;

			Savepoint sp = null;
			if (hasParameters)
			{
				try
				{
					if (currentConnection.getDbSettings().useSavePointForDML())
					{
						sp = currentConnection.setSavepoint();
					}
					outParameters = checkParametersFromStatement(cstmt);

					// The JDBC ParameterMetaData class does not expose parameter names
					// so they cannot be displayed in the dialog

					// TODO: another call to retrieve the parameter names through JDBC
					// in order to be able to display them, even if ParameterMetaData is used.
					namesAvailable = false;
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
					if (currentConnection.getDbSettings().useSavePointForDML())
					{
						sp = currentConnection.setSavepoint();
					}

					outParameters = checkParametersFromDatabase(cleanSql);

					// When retrieving the actual procedure parameters we do have
					// the parameter names available, so we can show them in the dialog
					namesAvailable = true;

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
				boolean ok = ParameterEditor.showParameterDialog(input, namesAvailable);
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

			if (refCursor != null)
			{
				int outIndex = 0;
				for (Map.Entry<Integer, ParameterDefinition> refs : refCursor.entrySet())
				{
					try
					{
						ResultSet rs = (ResultSet)cstmt.getObject(refs.getKey().intValue());

						// processResults will close the result set
						if (rs != null) processResults(result, true, rs);
						List<DataStore> results = result.getDataStores();
						if (CollectionUtil.isNonEmpty(results) && refs.getValue() != null)
						{
							DataStore ds = results.get(results.size() - 1);
							ds.setGeneratingSql(aSql);
							if (ds.getResultName() == null)
							{
								String name = refs.getValue().getParameterName();
								if (StringUtil.isNonBlank(name))
								{
									ds.setResultName(name);
								}
							}
						}
						outIndex ++;
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
					if (refCursor != null && refCursor.containsKey(Integer.valueOf(def.getIndex()))) continue;

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
				resultData.resetStatus();
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
		if (this.refCursor != null) this.refCursor.clear();
		this.refCursor = null;
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

				if (mode == ParameterMetaData.parameterModeOut ||
						mode == ParameterMetaData.parameterModeInOut)
				{
					cstmt.registerOutParameter(i + 1, type);
					parameterNames.add(def);
				}
				else
				{
					inputParameters.add(def);
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

		List<String> sqlParams = CollectionUtil.arrayList();

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

			// Analyze the parameters given..
			while (n != null && !n.getContents().equals("("))
			{
				n = l.getNextToken(false, false);
			}
			n = l.getNextToken(false, false);
			while (n != null && !n.getContents().equals(")"))
			{
				if (!n.getContents().equals(","))
				{
					if (n.getContents().equals("?"))
					{
						sqlParams.add("?");
					}
					else
					{
						sqlParams.add("literal");
					}
				}
				n = l.getNextToken(false, false);
			}
		}
		catch (IOException e)
		{
			LogMgr.logError("WbCall.checkParametersFromDatabase", "Error checking SQL", e);
			return null;
		}

		DbMetadata meta = this.currentConnection.getMetadata();
		ArrayList<ParameterDefinition> parameterNames = null;

		TableIdentifier name = new TableIdentifier(schema, procname);
		name.setPreserveQuotes(true);

		String schemaToUse = StringUtil.trimQuotes(meta.adjustSchemaNameCase(name.getSchema(), true));
		if (schemaToUse == null)
		{
			schemaToUse = meta.getCurrentSchema();
		}
		String nameToUse = StringUtil.trimQuotes(meta.adjustObjectnameCase(name.getObjectName()));

		DataStore params = meta.getProcedureReader().getProcedureColumns(null, schemaToUse, nameToUse);

		boolean needFuncCall = meta.isPostgres() && returnsRefCursor(params);
		CallableStatement cstmt = currentConnection.getSqlConnection().prepareCall(getSqlToPrepare(sql, needFuncCall));
		this.currentStatement = cstmt;


		int definedParamCount = params.getRowCount();
		if (definedParamCount != sqlParams.size())
		{
			LogMgr.logWarning("WbCall.checkParametersFromDatabase", "Number of parameters reported for procedure (" + definedParamCount + ") is different than the number supplied in actual sql ("+ sqlParams.size() + ")");
			sqlParams = null;
		}

		if (definedParamCount > 0)
		{
			int realParamIndex = 1;

			parameterNames = new ArrayList<ParameterDefinition>(definedParamCount);
			for (int i = 0; i < params.getRowCount(); i++)
			{
				int dataType = params.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, -1);

				String typeName = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String resultType = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				String paramName = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);

				ParameterDefinition def = new ParameterDefinition(realParamIndex, dataType);
				def.setParameterName(paramName);

				boolean needsInput = resultType.equals("IN");
				if (resultType.equals("INOUT"))
				{
					needsInput = !isRefCursor(typeName);
				}

				// Only real input parameters need to be added to the dialog
				if (needsInput)
				{
					if (sqlParams != null)
					{
						// only add the parameter as an input parameter
						// if a place holder was specified
						if (sqlParams.get(i).equals("?"))
						{
							inputParameters.add(def);
							realParamIndex ++;
						}
					}
					else
					{
						// for some reason parsing the argument list did
						// not give the correct numbers. We cannot know if
						// the parameter was supplied or not.
						inputParameters.add(def);
						realParamIndex ++;
					}
				}
				else if (resultType != null && resultType.endsWith("OUT") || (needFuncCall && StringUtil.equalString(resultType, "RETURN")))
				{
					if (isRefCursor(typeName))
					{
						// these parameters should not be added to the regular parameter list
						// as they have to be retrieved in a different manner.
						// type == -10 is Oracles CURSOR Datatype
						int dbmsTypeOverride = currentConnection.getDbSettings().getRefCursorDataType();
						if (dbmsTypeOverride != Integer.MIN_VALUE) dataType = dbmsTypeOverride;
						if (refCursor == null)
						{
							refCursor = new HashMap<Integer, ParameterDefinition>();
						}
						refCursor.put(Integer.valueOf(realParamIndex), def);
					}
					else
					{
						parameterNames.add(def);
					}
					cstmt.registerOutParameter(realParamIndex, dataType);
					realParamIndex++;
				}
			}
		}

		return parameterNames;
	}

	private boolean isRefCursor(String type)
	{
		List<String> refTypes = currentConnection.getDbSettings().getRefCursorTypeNames();
		return refTypes.contains(type);
	}

	private boolean returnsRefCursor(DataStore params)
	{
		// A function in Postgres that returns a refcursor
		// must be called using {? = call('procname')} in order
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
