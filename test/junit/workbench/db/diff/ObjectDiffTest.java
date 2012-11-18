/*
 * ObjectDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.diff;

import java.sql.Types;

import workbench.db.BaseObjectType;
import workbench.db.ColumnIdentifier;

import workbench.util.CollectionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectDiffTest
{
	public ObjectDiffTest()
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
	public void testDiff()
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

		ColumnIdentifier col4 = new ColumnIdentifier("lastname");
		col4.setDbmsType("varchar(10)");
		col4.setDataType(Types.VARCHAR);
		o2.setAttributes(CollectionUtil.arrayList(col1, col2, col4));

		ObjectDiff diff = new ObjectDiff(o1, o2);
		assertTrue(diff.isDifferent(null, null));
		String xml = diff.getMigrateTargetXml(null, null).toString();
		assertTrue(xml.length() > 0);

		o2.setAttributes(CollectionUtil.arrayList(col1, col2, col3));
		assertFalse(diff.isDifferent(null, null));
	}

}
