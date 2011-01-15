/*
 * EnumIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
		return "ENUM";
	}

	public String getObjectName()
	{
		return enumName;
	}

	public String getObjectName(WbConnection conn)
	{
		return enumName;
	}

	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, enumName);
	}

	public String getObjectExpression(WbConnection conn)
	{
		return enumName;
	}

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
