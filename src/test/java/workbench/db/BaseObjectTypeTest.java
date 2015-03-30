/*
 * BaseObjectTypeTest.java
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
