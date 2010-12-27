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

	@Override
	public String getColumnClassName(int type, String dbmsType)
	{
		return null;
	}

	/**
	 * Returns the correct display for the given data type.
	 *
	 * @see workbench.util.SqlUtil#getSqlTypeDisplay(java.lang.String, int, int, int)
	 */
	@Override
	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
	{
		return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

	/**
	 * Default implementation, does not change the datatype
	 * @param type the java.sql.Types as returned from the driver
	 * @param dbmsType the DBMS data type as returned from the driver
	 * @return the passed type
	 */
	@Override
	public int fixColumnType(int type, String dbmsType)
	{
		// Nothing to do
		return type;
	}
	
}
