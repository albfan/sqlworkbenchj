/*
 * DefaultDataTypeResolver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;
import workbench.util.SqlUtil;
/**
 * @author Thomas Kellerer
 */
public class DefaultDataTypeResolver
	implements DataTypeResolver
{

	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
	{
		return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

	@Override
	public int fixColumnType(int type)
	{
		// Nothing to do
		return type;
	}
	
}
