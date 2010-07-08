/*
 *  DB2ObjectType.java
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class DB2ObjectType
	implements DbObject
{
	private String objectSchema;
	private String objectName;
	private List<ColumnIdentifier> columns;
	private String source;
	private String remarks;
	
	public DB2ObjectType(String schema, String objectName)
	{
		this.objectSchema = schema;
		this.objectName = objectName;
	}

	@Override
	public String getCatalog()
	{
		return null;
	}

	@Override
	public String getSchema()
	{
		return objectSchema;
	}

	@Override
	public String getObjectType()
	{
		return "TYPE";
	}

	@Override
	public String getObjectName()
	{
		return objectName;
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return objectName;
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return objectSchema + "." + objectName;
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return objectSchema + "." + objectName;
	}

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return objectSchema + "." + objectName;
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
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return "DROP TYPE " + objectSchema + "." + objectName;
	}

	public List<ColumnIdentifier> getAttributes()
	{
		return columns;
	}

	public void setAttributes(List<ColumnIdentifier> attr)
	{
		columns = new ArrayList<ColumnIdentifier>(attr);
	}

	public int getNumberOfAttributes()
	{
		if (columns == null) return 0;
		return columns.size();
	}

	public void setSource(String sql)
	{
		source = sql;
	}

	public String getSource()
	{
		return source;
	}
}
