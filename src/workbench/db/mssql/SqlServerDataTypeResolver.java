/*
 * SqlServerDataTypeResolver
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import java.sql.Types;
import workbench.db.DefaultDataTypeResolver;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerDataTypeResolver
	extends DefaultDataTypeResolver
{

	@Override
	public String getColumnClassName(int type, String dbmsType)
	{
		if (Settings.getInstance().getFixSqlServerTimestampDisplay() && type == Types.BINARY && "timestamp".equals(dbmsType))
		{
			// RowData#readRow() will convert the byte[] into a hex String getFixSqlServerTimestampDisplay() is true
			// so we need to make sure, the class name is correct
			return "java.lang.String";
		}
		return null;
	}

}
