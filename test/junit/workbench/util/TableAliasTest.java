/*
 * TableAliasTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import static org.junit.Assert.*;
import org.junit.Test;

public class TableAliasTest
{

	@Test
	public void testAlias()
	{
		String value = "table1";
		TableAlias ta = new TableAlias(value);

		assertEquals("Wrong table name", "table1", ta.getTable().getTableName());
		assertEquals("Wrong value name", "table1", ta.getNameToUse());
		assertNull("value is not null", ta.getAlias());

		value = "table2 t1";
		ta = new TableAlias(value);

		assertEquals("Wrong table name", "table2", ta.getTable().getTableName());
		assertEquals("Wrong value name", "t1", ta.getNameToUse());

		value = "table1 as t1";
		ta = new TableAlias(value);

		assertEquals("Wrong table name", "table1", ta.getTable().getTableName());
		assertEquals("Wrong value name", "t1", ta.getNameToUse());
	}

	@Test
	public void testCompare()
	{
		String value = "table1";
		TableAlias ta = new TableAlias(value);
		assertEquals("Not recognized as the same", true, ta.isTableOrAlias("table1"));

		value = "table1 t1";
		ta = new TableAlias(value);
		assertEquals("Not recognized as the same", true, ta.isTableOrAlias("table1"));
		assertEquals("Not recognized as the same", true, ta.isTableOrAlias("t1"));

	}
}
