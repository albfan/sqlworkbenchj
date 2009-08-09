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
package workbench.db;

import java.sql.SQLException;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
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
	
	public String getCatalog()
	{
		return catalog;
	}

	public String getSchema()
	{
		return schema;
	}

	public void setObjectType(String type)
	{
		objectType = type;
	}
	
	public String getObjectType()
	{
		return objectType;
	}

	public String getObjectName()
	{
		return domain;
	}

	public String getObjectName(WbConnection conn)
	{
		return domain;
	}

	public String getObjectExpression(WbConnection conn)
	{
		return domain;
	}

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

	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		return con.getMetadata().getObjectSource(this);
	}

	public String getObjectNameForDrop(WbConnection con)
	{
		return domain;
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
