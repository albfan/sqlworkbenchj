/*
 * BaseObjectType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.util.Collections;
import java.util.List;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A generic TYPE object
 * (used for Postgres, Oracle, DB2 and HSQLDB)
 *
 * @author Thomas Kellerer
 */
public class BaseObjectType
	implements ComparableDbObject
{
	private String catalog;
	private final String schema;
	private final String typeName;
	private final String objectType = "TYPE";
	private String remarks;
	private String source;
	private List<ColumnIdentifier> columns;

	public BaseObjectType(String schema, String typeName)
	{
		this.schema = schema;
		this.typeName = typeName;
	}

	public void setCatalog(String catalogName)
	{
		this.catalog = catalogName;
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
		return typeName;
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return typeName;
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.fullyQualifiedName(conn, this);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return typeName;
	}

	@Override
	public String toString()
	{
		return getObjectName();
	}

	public void setSource(String sql)
	{
		this.source = sql;
	}

	public String getSource()
	{
		return source;
	}

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (this.source == null && con != null)
		{
			return con.getMetadata().getObjectSource(this);
		}
		return source;
	}

	public List<ColumnIdentifier> getAttributes()
	{
		if (columns==null) return Collections.emptyList();
		return columns;
	}

	public void setAttributes(List<ColumnIdentifier> attr)
	{
		columns = new ArrayList<>(attr);
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

	public int getNumberOfAttributes()
	{
		if (CollectionUtil.isEmpty(columns)) return 0;
		return columns.size();
	}

	@Override
	public boolean isComparableWith(DbObject other)
	{
		return this.getClass().equals(other.getClass());
	}

	@Override
	public boolean isEqualTo(DbObject other)
	{
		if (other instanceof BaseObjectType)
		{
			BaseObjectType otherType = (BaseObjectType)other;
			if (this.getNumberOfAttributes() != otherType.getNumberOfAttributes()) return false;

			List<ColumnIdentifier> otherCols = otherType.getAttributes();
			for (ColumnIdentifier col : getAttributes())
			{
				ColumnIdentifier otherCol = ColumnIdentifier.findColumnInList(otherCols, col.getColumnName());
				if (otherCol == null) return false;
				if (!col.isEqualTo(otherCol)) return false;
			}
			return StringUtil.equalStringOrEmpty(remarks, otherType.remarks, false);
		}
		return false;
	}

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
