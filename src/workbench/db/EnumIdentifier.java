/*
 * EnumIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.util.SqlUtil;

/**
 * Representation of an enum type definition.
 *
 * Currently only used for PostgreSQL
 * 
 * @author Thomas Kellerer
 */
public class EnumIdentifier
	implements DbObject
{
	private String catalog;
	private String schema;
	private String enumName;
	private String remarks;
	private List<String> values;

	public EnumIdentifier(String dcatalog, String dschema, String name)
	{
		catalog = dcatalog;
		schema = dschema;
		enumName = name;
	}

	public void addEnumValue(String value)
	{
		if (this.values == null) values = new ArrayList<String>();
		values.add(value);
	}

	public List<String> getValues()
	{
		return values;
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
		return "ENUM";
	}

	@Override
	public String getObjectName()
	{
		return enumName;
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return enumName;
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.buildExpression(null, catalog, schema, enumName);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return enumName;
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
