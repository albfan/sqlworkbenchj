/*
 * ProcedureDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
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
	 * as returned by the JDBC driver corresponds to
	 * DatabaseMetaData.procedureNoResult
	 * DatabaseMetaData.procedureReturnsResult
	 */
	private int resultType;

	private OracleType oracleType;
	private String oracleOverloadIndex;

	private CharSequence source;
	private List<String> parameterTypes;
	private String dbmsProcType;
	
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
	
	public String getComment()
	{
		return comment;
	}

	public void setComment(String cmt)
	{
		comment = cmt;
	}

	public synchronized List<String> getParameterTypes(WbConnection con)
	{
		if (parameterTypes == null)
		{
			ProcedureReader reader = con.getMetadata().getProcedureReader();
			DbMetadata meta = con.getMetadata();
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
		if (isOraclePackage())
		{
			return "DROP PACKAGE "  + con.getMetadata().quoteObjectname(schema) + "." + con.getMetadata().quoteObjectname(catalog);
		}
		if (isOracleObjectType())
		{
			String drop = "DROP TYPE " + con.getMetadata().quoteObjectname(schema) + "." + con.getMetadata().quoteObjectname(catalog);
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
		if (!needParameters) return getObjectName();

		if (this.procName.indexOf('(') > -1) return procName;

		List<String> params = getParameterTypes(con);
		if (params.isEmpty()) return procName + "()";
		StringBuilder result = new StringBuilder(procName.length() + params.size() * 5 + 5);
		result.append(procName);
		result.append('(');
		for (int i=0; i < params.size(); i++)
		{
			if (i > 0) result.append(',');
			result.append(params.get(i));
		}
		result.append(')');
		return result.toString();
	}

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

	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.procName);
	}

	public String getFullyQualifiedName(WbConnection conn)
	{
		return getObjectExpression(conn);
	}

	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, procName);
	}

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

	public String getCatalog()
	{
		//		if (oracleType != null) return null;
		return this.catalog;
	}

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

	public String toString()
	{
		String name = oracleType != null ? catalog + "." + procName : procName;
		if (CollectionUtil.isNonEmpty(parameterTypes))
		{
			return name + "( " + StringUtil.listToString(parameterTypes, ", ", false) + " )";
		}
		return name;
	}

	public String toWbCallStatement(WbConnection con)
	{
		StringBuilder call = new StringBuilder(150);
		CommandTester c = new CommandTester();
		StringBuilder paramNames = new StringBuilder(50);
		paramNames.append("-- Parameters: ");
		call.append(c.formatVerb(WbCall.VERB));
		call.append(' ');
		call.append(oracleType != null ? catalog + "." + procName : procName);
		call.append("(");

		int rows = 0;
		int numParams = 0;
		try
		{
			DataStore dataStore = con.getMetadata().getProcedureReader().getProcedureColumns(this);
			rows = dataStore.getRowCount();

			for (int i = 0; i < rows; i++)
			{
				String inOut = dataStore.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				String param = dataStore.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);

				if (numParams > 0)
				{
					call.append(',');
					paramNames.append(", ");
				}

				// only append a ? for OUT or INOUT parameters, not for RETURN parameters
				if (inOut.equals("IN") || inOut.endsWith("OUT"))
				{
					paramNames.append(param);
					call.append('?');
					numParams ++;
				}
			}
			call.append(");");
		}
		catch (Exception ex)
		{
			LogMgr.logError("ProcedureListPanel.valueChanged() thread", "Could not read procedure definition", ex);
			return null;
		}
		paramNames.append('\n');
		call.insert(0, paramNames);
		return call.toString();
	}

	private static enum OracleType
	{
		packageType,
		objectType;
	}
}
