/*
 * DomainIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import workbench.db.*;
import java.sql.SQLException;
import workbench.util.SqlUtil;

/**
 * A class representing a CONSTANT in H2 Database
 *
 * @author Thomas Kellerer
 */
public class H2Constant
	implements DbObject
{
	private String catalog;
	private String schema;
	private String constantName;
	private final String objectType = "CONSTANT";
	private String remarks;
	private String value;
	private String dataType;

	public H2Constant(String dcatalog, String dschema, String name)
	{
		catalog = dcatalog;
		schema = dschema;
		constantName = name;
	}

	public void setDataType(String dbmsType)
	{
		dataType = dbmsType;
	}

	public String getDataType()
	{
		return dataType;
	}
	
	public void setValue(String constantValue)
	{
		value = constantValue;
	}

	public String getValue()
	{
		return value;
	}
	
	public String getCatalog()
	{
		return catalog;
	}

	public String getSchema()
	{
		return schema;
	}

	public String getObjectType()
	{
		return objectType;
	}

	public String getObjectName()
	{
		return constantName;
	}

	public String getObjectName(WbConnection conn)
	{
		return constantName;
	}

	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, constantName);
	}

	public String getObjectExpression(WbConnection conn)
	{
		return constantName;
	}

	public String toString()
	{
		return getObjectName();
	}
	
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		return con.getMetadata().getObjectSource(this);
	}

	@Override
	public String getDropStatement(WbConnection con)
	{
		return null;
	}

	public String getObjectNameForDrop(WbConnection con)
	{
		return getFullyQualifiedName(con);
	}

	public String getComment()
	{
		return remarks;
	}

	public void setComment(String cmt)
	{
		remarks = cmt;
	}

}
