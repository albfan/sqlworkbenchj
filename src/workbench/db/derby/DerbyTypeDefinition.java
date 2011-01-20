/*
 * DerbyTypeDefinition
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.derby;

import java.sql.SQLException;
import workbench.db.DbObject;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class DerbyTypeDefinition
	implements DbObject
{

	private String typeName;
	private String typeSchema;
	private String javaClassname;
	private String aliasInfo;

	public DerbyTypeDefinition(String schema, String name, String className, String info)
	{
		typeSchema = schema;
		typeName = name;
		javaClassname = className;
		aliasInfo = info;
	}

	@Override
	public String getCatalog()
	{
		return null;
	}

	@Override
	public String getSchema()
	{
		return typeSchema;
	}

	@Override
	public String getObjectType()
	{
		return "TYPE";
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
	public String getObjectExpression(WbConnection conn)
	{
		return typeSchema + "." + typeName;
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return typeSchema + "." + typeName;
	}

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(100);
		sql.append("CREATE TYPE ");
		sql.append(typeSchema + "." + typeName);
		sql.append("\n  EXTERNAL NAME '");
		sql.append(javaClassname);
		sql.append("' \n  ");
		sql.append(aliasInfo);
		sql.append(";\n");
		return sql;
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return getFullyQualifiedName(con);
	}

	@Override
	public String getComment()
	{
		return null;
	}

	@Override
	public void setComment(String cmt)
	{
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return null;
	}
}
