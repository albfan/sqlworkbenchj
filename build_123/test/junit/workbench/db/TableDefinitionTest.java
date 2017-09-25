/*
 * TableDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDefinitionTest
{

	public TableDefinitionTest()
	{
	}

	@Test
	public void testToString()
	{
		TableIdentifier tbl = new TableIdentifier("PERSON");
		List<ColumnIdentifier> cols = new ArrayList<>(3);
		cols.add(new ColumnIdentifier("ID", Types.INTEGER));
		ColumnIdentifier firstname = new ColumnIdentifier("FIRSTNAME", Types.VARCHAR);
		firstname.setColumnSize(50);
		cols.add(firstname);

		ColumnIdentifier lastname = new ColumnIdentifier("LASTNAME", Types.VARCHAR);
		lastname.setColumnSize(50);
		cols.add(lastname);

		TableDefinition def = new TableDefinition(tbl, cols);
		assertEquals("PERSON (ID, FIRSTNAME, LASTNAME)", def.toString());
	}
}
