/*
 * ProcedureDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.sql.wbcommands.CommandTester;
import workbench.sql.wbcommands.WbCall;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ProcedureDefinition
	implements DbObject
{
	private String schema;
	private String catalog;
	private String procName;
	private String comment;
	private String displayName;

	/**
	 * The result type as returned by the JDBC driver.
	 * Corresponds to:
	 * <ul>
	 *   <li>DatabaseMetaData.procedureNoResult</li>
	 *   <li>DatabaseMetaData.procedureReturnsResult</li>
	 * </ul>
	 */
	private int resultType;

	private OracleType oracleType;
	private String oracleOverloadIndex;

	private CharSequence source;
	private List<String> parameterTypes;
	private String dbmsProcType;

	/**
	 * Creates a new ProcedureDefinition.
	 *
	 * @param schema the schema of the procedure
	 * @param name the name of the procedure
	 * @param packageName the name of the Oracle package, may be null
	 * @param type the return type of the procedure (DatabaseMetaData.procedureNoResult or DatabaseMetaData.procedureReturnsResult)
	 * @param remarks the comment for this procedure
	 * @return the new ProcedureDefinition
	 */
	public static ProcedureDefinition createOracleDefinition(String schema, String name, String packageName, int type, String remarks)
	{
		ProcedureDefinition def = new ProcedureDefinition(packageName, schema, name, type);
		if (StringUtil.isNonBlank(packageName))
		{
			if ("OBJECT TYPE".equals(remarks))
			{
				def.oracleType = OracleType.objectType;
			}
			else
			{
				def.oracleType = OracleType.packageType;
			}
		}
		return def;
	}

	public void setOracleOverloadIndex(String indicator)
	{
		oracleOverloadIndex = indicator;
	}

	public String getOracleOverloadIndex()
	{
		return oracleOverloadIndex;
	}

	public ProcedureDefinition(String name, int type)
	{
		procName = name;
		resultType = type;
	}

	public ProcedureDefinition(String cat, String schem, String name, int type)
	{
		schema = schem;
		catalog = cat;
		procName = name;
		resultType = type;
	}

	public void setDbmsProcType(String type)
	{
		dbmsProcType = type;
	}

	public String getDbmsProcType()
	{
		return dbmsProcType;
	}

	public void setDisplayName(String name)
	{
		displayName = name;
	}

	public String getDisplayName()
	{
		if (displayName == null) return procName;
		return displayName;
	}

	@Override
	public String getComment()
	{
		return comment;
	}

	@Override
	public void setComment(String cmt)
	{
		comment = cmt;
	}

	public synchronized List<String> getParameterTypes(WbConnection con)
	{
		if (parameterTypes == null)
		{
			ProcedureReader reader = con.getMetadata().getProcedureReader();
			try
			{
				DataStore ds = reader.getProcedureColumns(this);
				parameterTypes = new ArrayList<String>(ds.getRowCount());

				for (int i = 0; i < ds.getRowCount(); i++)
				{
					String type = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
					if ("IN".equalsIgnoreCase(type) || "INOUT".equalsIgnoreCase(type))
					{
						parameterTypes.add(ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE));
					}
				}
			}
			catch (SQLException s)
			{
			}
		}
		return Collections.unmodifiableList(this.parameterTypes);
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		DbMetadata meta = con.getMetadata();
		if (isOraclePackage())
		{
			return "DROP PACKAGE "  + meta.quoteObjectname(schema) + "." + meta.quoteObjectname(catalog);
		}
		if (isOracleObjectType())
		{
			String drop = "DROP TYPE " + meta.quoteObjectname(schema) + "." + meta.quoteObjectname(catalog);
			if (cascade)
			{
				drop += " FORCE";
			}
			return drop;
		}
		// Apply default statements
		return null;
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		if (oracleType != null)
		{
			return catalog;
		}
		boolean needParameters = con.getDbSettings().needParametersToDropFunction();
		if (!needParameters)
		{
			return getObjectExpression(con);
		}

		if (this.procName.indexOf('(') > -1) return procName;

		List<String> params = getParameterTypes(con);
		if (params.isEmpty()) return procName + "()";
		StringBuilder result = new StringBuilder(procName.length() + params.size() * 5 + 5);
		result.append(getObjectExpression(con));
		result.append('(');
		for (int i=0; i < params.size(); i++)
		{
			if (i > 0) result.append(',');
			result.append(params.get(i));
		}
		result.append(')');
		return result.toString();
	}

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (con == null) return null;
		if (this.source == null)
		{
			try
			{
				con.getMetadata().getProcedureReader().readProcedureSource(this);
			}
			catch (NoConfigException e)
			{
				this.source = "N/A";
			}
		}
		return this.source;
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.procName);
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return getObjectExpression(null);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, procName);
	}

	@Override
	public String getObjectName()
	{
		return procName;
	}

	public void setSource(CharSequence s)
	{
		this.source = s;
	}

	public CharSequence getSource()
	{
		return this.source;
	}

	public boolean isOraclePackage()
	{
		return oracleType == OracleType.packageType;
	}

	public boolean isOracleObjectType()
	{
		return oracleType == OracleType.objectType;
	}

	/**
	 * Returns the package or object type name of this definition
	 * This will return null if isOraclePackage() == false or isOracleObjectType() == false
	 */
	public String getPackageName()
	{
		if (oracleType != null) return catalog;
		return null;
	}

	@Override
	public String getCatalog()
	{
		return this.catalog;
	}

	@Override
	public String getSchema()
	{
		return this.schema;
	}

	public String getProcedureName()
	{
		return getObjectName();
	}

	public int getResultType()
	{
		return this.resultType;
	}

	@Override
	public String getObjectType()
	{
		if (this.isOraclePackage())
		{
			return "PACKAGE";
		}
		if (this.isOracleObjectType())
		{
			return "TYPE";
		}

		if (resultType == DatabaseMetaData.procedureReturnsResult)
		{
			return "FUNCTION";
		}
		else if (resultType == DatabaseMetaData.procedureNoResult)
		{
			return "PROCEDURE";
		}

		return "";
	}

	@Override
	public String toString()
	{
		String name = oracleType != null ? catalog + "." + procName : procName;
		if (CollectionUtil.isNonEmpty(parameterTypes))
		{
			return name + "( " + StringUtil.listToString(parameterTypes, ", ", false) + " )";
		}
		return name;
	}

	public String createSql(WbConnection con)
	{
		boolean hasOutParameters = false;
		boolean returnsRefCursor = false;
		boolean isFunction = false;
		DataStore params = null;

		try
		{
			params = con.getMetadata().getProcedureReader().getProcedureColumns(this);
			returnsRefCursor = returnsRefCursor(con, params);
			int rows = params.getRowCount();

			for (int i = 0; i < rows; i++)
			{
				String type = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);

				if (type.endsWith("OUT"))
				{
					hasOutParameters = true;
				}

				if (type.equals("RETURN"))
				{
					isFunction = true;
				}
			}

		}
		catch (Exception ex)
		{
			LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure definition", ex);
			return null;
		}

		if (isFunction && !returnsRefCursor && !hasOutParameters)
		{
			String sql = buildFunctionCall(con, params);
			if (sql != null)
			{
				return sql;
			}
		}
		return createWbCallStatement(params);
	}

	public String createWbCallStatement(DataStore params)
	{
		StringBuilder call = new StringBuilder(150);
		CommandTester c = new CommandTester();


		StringBuilder paramNames = new StringBuilder(50);

		call.append(c.formatVerb(WbCall.VERB));
		call.append(' ');
		call.append(oracleType != null ? catalog + "." + procName : procName);
		call.append("(");

		int numParams = 0;
		int rows = params.getRowCount();

		for (int i = 0; i < rows; i++)
		{
			String type = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
			String param = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);

			if (numParams > 0)
			{
				call.append(',');
			}

			// only append a ? for OUT or INOUT parameters, not for RETURN parameters
			if (type.equals("IN") || type.endsWith("OUT"))
			{
				if (numParams == 0)
				{
					paramNames.append("-- Parameters: ");
				}
				else
				{
					paramNames.append(", ");
				}
				paramNames.append(param);
				paramNames.append(" (");
				paramNames.append(type);
				paramNames.append(')');
				call.append('?');
				numParams ++;
			}
		}
		call.append(");");
		if (numParams > 0)
		{
			paramNames.append('\n');
			call.insert(0, paramNames);
		}

		return call.toString();
	}

	private String buildFunctionCall(WbConnection conn, DataStore params)
	{
		String template = conn.getDbSettings().getSelectForFunctionSQL();
		if (template == null)
		{
			return null;
		}

		StringBuilder call = new StringBuilder(150);
		call.append(oracleType != null ? catalog + "." + procName : procName);
		call.append("( ");

		int rows = params.getRowCount();
		int numParams = 0;

		for (int i = 0; i < rows; i++)
		{
			String type = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
			String param = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
			int dataType = params.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, Types.OTHER);

			if (numParams > 0)
			{
				call.append(", ");
			}

			if (!type.equals("RETURN"))
			{
				if (SqlUtil.isCharacterType(dataType))
				{
					call.append('\'');
				}
				call.append("$[?");
				call.append(param);
				call.append(']');
				if (SqlUtil.isCharacterType(dataType))
				{
					call.append('\'');
				}
				numParams ++;
			}
		}
		call.append(" )");

		String sql = template.replace("%function%", call.toString());
		return sql;
	}

	public static boolean isRefCursor(WbConnection conn, String type)
	{
		List<String> refTypes = conn.getDbSettings().getRefCursorTypeNames();
		return refTypes.contains(type);
	}

	public boolean hasOutParameter(DataStore params)
	{
		for (int i=0; i < params.getRowCount(); i++)
		{
			String resultMode = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
			if (resultMode.endsWith("OUT")) return true;
		}
		return false;
	}

	public static boolean returnsRefCursor(WbConnection conn, DataStore params)
	{
		// A function in Postgres that returns a refcursor
		// must be called using {? = call('procname')} in order
		// to be able to retrieve the result set from the refcursor
		for (int i=0; i < params.getRowCount(); i++)
		{
			String typeName = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
			String resultType = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
			if (isRefCursor(conn, typeName) && "RETURN".equals(resultType)) return true;
		}
		return false;
	}

	public boolean isFunction()
	{
		return resultType == DatabaseMetaData.procedureReturnsResult;
	}

	public boolean isFunction(DataStore params)
	{
		for (int i=0; i < params.getRowCount(); i++)
		{
			String resultMode = params.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
			if ("RETURN".equals(resultMode)) return true;
		}
		return false;
	}

	private static enum OracleType
	{
		packageType,
		objectType;
	}
}
