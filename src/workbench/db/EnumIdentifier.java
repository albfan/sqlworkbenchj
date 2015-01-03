/*
 * EnumIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	implements ComparableDbObject
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
		if (this.values == null) values = new ArrayList<>();
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
		return SqlUtil.fullyQualifiedName(conn, this);
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

	@Override
	public boolean isComparableWith(DbObject other)
	{
		return (other instanceof EnumIdentifier);
	}

	@Override
	public boolean isEqualTo(DbObject other)
	{
		if (other instanceof EnumIdentifier)
		{
			EnumIdentifier id = (EnumIdentifier)other;
			int mySize = values == null ? 0 : values.size();
			int otherSize = id.values == null ? 0 : id.values.size();
			if (mySize != otherSize) return false;
			return values.equals(id.values);
		}
		return false;
	}
}
