/*
 * SQLiteDataTypeResolverTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.sqlite;

import java.sql.Types;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SQLiteDataTypeResolverTest
{

	public SQLiteDataTypeResolverTest()
	{
	}

	@Test
	public void testFixColumnType()
	{
		SQLiteDataTypeResolver resolver = new SQLiteDataTypeResolver();
		assertEquals(Types.DOUBLE, resolver.fixColumnType(Types.VARCHAR, "REAL"));
		assertEquals(Types.DECIMAL, resolver.fixColumnType(Types.VARCHAR, "DECIMAL(10,3)"));
		assertEquals(Types.BLOB, resolver.fixColumnType(Types.VARCHAR, "BLOB"));
	}
}
