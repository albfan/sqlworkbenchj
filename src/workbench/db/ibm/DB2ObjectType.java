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

import workbench.db.BaseObjectType;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class DB2ObjectType
	extends BaseObjectType
{
	public DB2ObjectType(String schema, String objectName)
	{
		super(schema, objectName);
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return "DROP TYPE " + getSchema() + "." + getObjectName();
	}

}
