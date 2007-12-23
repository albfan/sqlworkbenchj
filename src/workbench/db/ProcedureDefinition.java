/*
 * ProcedureDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class ProcedureDefinition
	implements DbObject
{
	private String schema;
	private String catalog;
	private String procName;
	
	// as returned by the JDBC driver corresponds to 
	// DatabaseMetaData.procedureNoResult
	// DatabaseMetaData.procedureReturnsResult
	private int resultType; 
	
	private boolean isOraclePackage = false;
	private CharSequence source;

	public static ProcedureDefinition createOraclePackage(String schem, String name)
	{
		ProcedureDefinition def = new ProcedureDefinition(name, schem, null, DatabaseMetaData.procedureResultUnknown);
		def.setOraclePackage(true);
		return def;
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
	
	public ProcedureDefinition(String cat, String schem, String name, int type, boolean isOracle)
	{
		schema = schem;
		catalog = cat;
		procName = name;
		resultType = type;
		if (isOracle)
		{
			this.isOraclePackage = !StringUtil.isEmptyString(catalog);
		}
	}

	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (con == null) return null;
		if (this.source == null)
		{
			try
			{
				con.getMetadata().readProcedureSource(this);
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
	
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, procName);
	}
	
	public String getObjectName()
	{
		return getProcedureName();
	}
	
	public void setSource(CharSequence s) 
	{ 
		this.source = s; 
	}
	
	public CharSequence getSource() 
	{ 
		return this.source; 
	}
	
	public void setOraclePackage(boolean flag) { this.isOraclePackage = true; }
	public boolean isOraclePackage() { return this.isOraclePackage; }
	
	public String getCatalog() 
	{
		if (this.isOraclePackage) return null;
		return this.catalog; 
	}
	
	public String getSchema() 
	{ 
		return this.schema; 
	}
	
	public String getProcedureName() 
	{
		if (this.isOraclePackage) return catalog;
		return this.procName; 
	}
	
	public int getResultType() 
	{ 
		return this.resultType; 
	}
	
	public String getObjectType()
	{
		if (this.isOraclePackage)
		{
			return "PACKAGE";
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
		return procName;
	}
	
}
