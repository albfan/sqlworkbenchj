/*
 * ObjectDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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

import workbench.TestUtil;

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
		col2.setColumnSize(100);
		col2.setDataType(Types.VARCHAR);

		ColumnIdentifier col3 = new ColumnIdentifier("lastname");
		col3.setDbmsType("varchar(100)");
		col3.setColumnSize(100);
		col3.setDataType(Types.VARCHAR);
		o1.setAttributes(CollectionUtil.arrayList(col1, col2, col3));

		ColumnIdentifier col4 = new ColumnIdentifier("lastname");
		col4.setDbmsType("varchar(10)");
		col4.setColumnSize(10);
		col4.setDataType(Types.VARCHAR);
		o2.setAttributes(CollectionUtil.arrayList(col1, col2, col4));

		ObjectDiff diff = new ObjectDiff(o1, o2, "DIFFTEST");
		assertTrue(diff.isDifferent(null, null));
		String xml = diff.getMigrateTargetXml(null, null).toString();
		assertTrue(xml.length() > 0);
//		System.out.println(xml);
		String value = TestUtil.getXPathValue(xml, "count(/modify-type[@name='some_type']/modify-column[@name='lastname'])");
		assertEquals("1", value);

		value = TestUtil.getXPathValue(xml, "/modify-type[@name='some_type']/modify-column[@name='lastname']/new-column-attributes/dbms-data-type");
		assertEquals("varchar(100)", value);

		o2.setAttributes(CollectionUtil.arrayList(col1, col2, col3));
		assertFalse(diff.isDifferent(null, null));
	}

}
