/*
 * BaseObjectTypeTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.Types;

import workbench.util.CollectionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class BaseObjectTypeTest
{
	public BaseObjectTypeTest()
	{
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void testIsEqualTo()
	{
		BaseObjectType o1 = new BaseObjectType(null, "ref_type");
		BaseObjectType o2 = new BaseObjectType(null, "some_type");

		ColumnIdentifier col1 = new ColumnIdentifier("id");
		col1.setDbmsType("integer");
		col1.setDataType(Types.INTEGER);

		ColumnIdentifier col2 = new ColumnIdentifier("firstname");
		col2.setDbmsType("varchar(100)");
		col2.setDataType(Types.VARCHAR);

		ColumnIdentifier col3 = new ColumnIdentifier("lastname");
		col3.setDbmsType("varchar(100)");
		col3.setDataType(Types.VARCHAR);

		o1.setAttributes(CollectionUtil.arrayList(col1, col2, col3));
		o2.setAttributes(CollectionUtil.arrayList(col1, col2, col3));
		assertTrue(o1.isEqualTo(o2));

		ColumnIdentifier col4 = new ColumnIdentifier("lastname");
		col4.setDbmsType("varchar(10)");
		col4.setDataType(Types.VARCHAR);
		o2.setAttributes(CollectionUtil.arrayList(col1, col2, col4));
		assertFalse(o1.isEqualTo(o2));
	}


}
