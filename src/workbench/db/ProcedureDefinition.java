/*
 * ProcedureDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.DatabaseMetaData;

/**
 *
 * @author support@sql-workbench.net
 */
public class ProcedureDefinition
{
	private String schema;
	private String catalog;
	private String procName;
	
	// as returned by the JDBC driver corresponds to 
	// DatabaseMetaData.procedureNoResult
	// DatabaseMetaData.procedureReturnsResult
	private int resultType; 
	
	private boolean isOraclePackage = false;
	private String source;

	public static ProcedureDefinition createOraclePackage(String schem, String name)
	{
		ProcedureDefinition def = new ProcedureDefinition(null, schem, name, DatabaseMetaData.procedureResultUnknown);
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
	
	public void setSource(String s) { this.source = s; }
	public String getSource() { return this.source; }
	
	public void setOraclePackage(boolean flag) { this.isOraclePackage = true; }
	public boolean isOraclePackage() { return this.isOraclePackage; }
	
	public String getCatalog() { return this.catalog; }
	public String getSchema() { return this.schema; }
	public String getProcedureName() { return this.procName; }
	public int getResultType() { return this.resultType; }
	
	public String getResultTypeDisplay()
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
