/*
 * PostgresDataTypeResolverTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.Types;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Thomas Kellerer
 */
public class PostgresDataTypeResolverTest
{

	@Test
	public void testGetSqlTypeDisplay()
	{
		PostgresDataTypeResolver resolver = new PostgresDataTypeResolver();
		
		String display = resolver.getSqlTypeDisplay("NUMERIC", Types.NUMERIC, 65535, 0);
		assertEquals("NUMERIC", display);
		
		display = resolver.getSqlTypeDisplay("NUMERIC", Types.NUMERIC, 131089, 0);
		assertEquals("NUMERIC", display);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 300, 0);
		assertEquals("VARCHAR(300)", display);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, Integer.MAX_VALUE, 0);
		assertEquals("varchar", display);

		display = resolver.getSqlTypeDisplay("text", Types.VARCHAR, 300, 0);
		assertEquals("text", display);

		display = resolver.getSqlTypeDisplay("int8", Types.BIGINT, 0, 0);
		assertEquals("bigint", display);

		display = resolver.getSqlTypeDisplay("int4", Types.INTEGER, 0, 0);
		assertEquals("integer", display);

		display = resolver.getSqlTypeDisplay("int2", Types.SMALLINT, 0, 0);
		assertEquals("smallint", display);
	}
	
}
