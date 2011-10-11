/*
 * H2Constant.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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

	@Override
	public String getCatalog()
	{
		return catalog;
	}

	@Override
	public String getSchema()
	{
		return schema;
	}

	@Override
	public String getObjectType()
	{
		return objectType;
	}

	@Override
	public String getObjectName()
	{
		return constantName;
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return constantName;
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, constantName);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return constantName;
	}

	@Override
	public String toString()
	{
		return getObjectName();
	}

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		return con.getMetadata().getObjectSource(this);
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return null;
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return getFullyQualifiedName(con);
	}

	@Override
	public String getComment()
	{
		return remarks;
	}

	@Override
	public void setComment(String cmt)
	{
		remarks = cmt;
	}

}
