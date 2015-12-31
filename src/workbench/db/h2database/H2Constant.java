/*
 * H2Constant.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.h2database;

import java.sql.SQLException;

import workbench.db.DbObject;
import workbench.db.WbConnection;

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

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
