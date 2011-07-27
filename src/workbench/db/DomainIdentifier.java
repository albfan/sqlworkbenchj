/*
 * DomainIdentifier.java
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

import java.sql.SQLException;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DomainIdentifier
	implements DbObject
{
	private String catalog;
	private String schema;
	private String domain;
	private String objectType = "DOMAIN";
	private String remarks;
	private String dataType;
	private boolean nullable;
	private String constraintDefinition;
	private String constraintName;
	private String defaultValue;
	private String source;

	public DomainIdentifier(String dcatalog, String dschema, String name)
	{
		catalog = dcatalog;
		schema = dschema;
		domain = name;
	}

	public void setDefaultValue(String def)
	{
		defaultValue = def;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDataType(String dbmsType)
	{
		dataType = dbmsType;
	}

	public String getDataType()
	{
		return dataType;
	}

	public void setNullable(boolean flag)
	{
		nullable = flag;
	}

	public boolean isNullable()
	{
		return nullable;
	}

	public void setConstraintName(String name)
	{
		this.constraintName = name;
	}

	public String getConstraintName()
	{
		return constraintName;
	}

	public void setCheckConstraint(String check)
	{
		this.constraintDefinition = check;
	}

	public String getCheckConstraint()
	{
		return constraintDefinition;
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

	public void setObjectType(String type)
	{
		objectType = type;
	}

	@Override
	public String getObjectType()
	{
		return objectType;
	}

	@Override
	public String getObjectName()
	{
		return domain;
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return domain;
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.buildExpression(null, catalog, schema, domain);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, this);
	}

	@Override
	public String toString()
	{
		return getSummary();
	}

	public String getSummary()
	{
		StringBuilder result = new StringBuilder(25);
		result.append(this.dataType);
		if (!nullable) result.append(" NOT NULL");
		if (StringUtil.isNonBlank(defaultValue)) result.append(" DEFAULT " + defaultValue);
		if (StringUtil.isNonBlank(constraintDefinition))
		{
			result.append(' ');
			result.append(constraintDefinition);
		}
		result.append(';');
		return result.toString();
	}

	public String getSource()
	{
		return source;
	}

	public void setSource(String sql)
	{
		source = sql;
	}

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (StringUtil.isBlank(source))
		{
			source = con.getMetadata().getObjectSource(this);
		}
		return source;
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
