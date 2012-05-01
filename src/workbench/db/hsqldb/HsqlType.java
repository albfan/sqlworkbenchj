/*
 * HsqlType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.hsqldb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.BaseObjectType;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlType
	extends BaseObjectType
{
	private String dataType;

	public HsqlType(String schema, String typeName)
	{
		super(schema, typeName);
	}

	public void setDataTypeName(String typeName)
	{
		dataType = typeName;
	}

	public String getDataTypeName()
	{
		return dataType;
	}

	@Override
	public List<ColumnIdentifier> getAttributes()
	{
		return Collections.emptyList();
	}

	@Override
	public void setAttributes(List<ColumnIdentifier> attr)
	{
	}
}
