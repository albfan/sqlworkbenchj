/*
 *  OracleObjectType.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import workbench.db.BaseObjectType;
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleObjectType
	extends BaseObjectType
{
	private int numMethods;
	private int numAttributes;

	public OracleObjectType(String owner, String objectName)
	{
		super(owner, objectName);
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return "DROP TYPE " + getFullyQualifiedName(con) + " FORCE";
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

	@Override
	public int getNumberOfAttributes()
	{
		if (CollectionUtil.isEmpty(getAttributes()))
		{
			return numAttributes;
		}
		return getAttributes().size();
	}

	@Override
	public boolean isEqualTo(DbObject other)
	{
		boolean isEqual = super.isEqualTo(other);
		if (isEqual && (other instanceof OracleObjectType))
		{
			OracleObjectType otherType = (OracleObjectType)other;
			isEqual = this.getNumberOfMethods() == otherType.getNumberOfMethods();
		}
		return isEqual;
	}

}
