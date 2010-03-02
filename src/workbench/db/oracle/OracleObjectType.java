/*
 *  OracleObjectType.java
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleObjectType
	implements DbObject
{
	private String owner;
	private String objectName;
	private List<ColumnIdentifier> columns;
	private int numMethods;
	private int numAttributes;
	private String source;
	
	public OracleObjectType(String owner, String objectName)
	{
		this.owner = owner;
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
		return owner;
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
		return owner + "." + objectName;
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return owner + "." + objectName;
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
		return owner + "." + objectName;
	}

	@Override
	public String getComment()
	{
		return null;
	}

	@Override
	public void setComment(String cmt)
	{
		// Comments are not supported for object types
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return "DROP TYPE " + owner + "." + objectName + " FORCE";
	}

	public List<ColumnIdentifier> getAttributes()
	{
		return columns;
	}

	public void setAttributes(List<ColumnIdentifier> attr)
	{
		columns = new ArrayList<ColumnIdentifier>(attr);
	}

	public void setNumberOfMethods(int count)
	{
		this.numMethods = count;
	}

	public int getNumberOfMethods()
	{
		return numMethods;
	}

	public void setNumberOfAttributes(int count)
	{
		this.numAttributes = count;
	}

	public int getNumberOfAttributes()
	{
		if (CollectionUtil.isEmpty(columns))
		{
			return numAttributes;
		}
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
