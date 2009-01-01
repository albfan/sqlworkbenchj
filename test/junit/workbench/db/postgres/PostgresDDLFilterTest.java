/*
 * PostgresDDLFilterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class PostgresDDLFilterTest
	extends TestCase
{
	public PostgresDDLFilterTest(String testName)
	{
		super(testName);
	}

	public void testAdjustDDL()
	{
		PostgresDDLFilter filter = new PostgresDDLFilter();
		String sql = "SELECT * FROM dummy";
		String filtered = filter.adjustDDL(sql);
		assertEquals(sql, filtered);

		sql = "CREATE INDEX myindex on mytable(mycol)";
		filtered = filter.adjustDDL(sql);
		assertEquals(sql, filtered);

		sql = "CREATE TRIGGER trg on mytable execute procedure test_trg;";
		filtered = filter.adjustDDL(sql);
		assertEquals(sql, filtered);

		sql = "CREATE function myfunc as 'select count(*) from person;' language sql;";
		filtered = filter.adjustDDL(sql);
		assertEquals(sql, filtered);
		
		
		sql = "CREATE OR REPLACE FUNCTION append(i varchar) RETURNS varchar AS\n$$\n" +
					"BEGIN\n" +
					"    RETURN i ||'bla';\n" + 
					"END;\n$$\n"+
					"LANGUAGE plpgsql;";
		filtered = filter.adjustDDL(sql);
		String expected = "CREATE OR REPLACE FUNCTION append(i varchar) RETURNS varchar AS\n '\n" +
					"BEGIN\n" +
					"    RETURN i ||''bla'';\n" + 
					"END;\n' \n"+
					"LANGUAGE plpgsql;";
//		System.out.println(expected);
//		System.out.println("-----");
//		System.out.println(filtered);
		assertEquals(expected, filtered);
	}
}
