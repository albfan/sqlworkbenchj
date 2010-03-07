/*
 *  PostgresStructType.java
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.SQLException;
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 * Postgres TYPE objects
 * 
 * @author Thomas Kellerer
 */
public class PostgresObjectType
	implements DbObject
{
	private String catalog;
	private String schema;
	private String typeName;
	private final String objectType = "TYPE";
	private String remarks;
	private String source;

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
		return typeName;
	}

	public String getObjectName(WbConnection conn)
	{
		return typeName;
	}

	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, typeName);
	}

	public String getObjectExpression(WbConnection conn)
	{
		return typeName;
	}

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

	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (this.source == null)
		{
			return con.getMetadata().getObjectSource(this);
		}
		return source;
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
