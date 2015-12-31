/*
 * ResultInfoTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.storage;

import workbench.db.ColumnIdentifier;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultInfoTest
{

	@Test
	public void testFindColumn()
		throws Exception
	{
		ColumnIdentifier col1 = new ColumnIdentifier("\"KEY\"", java.sql.Types.VARCHAR, true);
		ColumnIdentifier col2 = new ColumnIdentifier("\"Main Cat\"", java.sql.Types.VARCHAR, false);
		ColumnIdentifier col3 = new ColumnIdentifier("firstname", java.sql.Types.VARCHAR, false);
		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { col1, col2, col3 } );
		assertEquals(3, info.getColumnCount());
		assertEquals(true, info.hasPkColumns());

		int index = info.findColumn("key");
		assertEquals(0, index);

		index = info.findColumn("\"KEY\"");
		assertEquals(0, index);

		index = info.findColumn("\"key\"");
		assertEquals(0, index);

		index = info.findColumn("\"Main Cat\"");
		assertEquals(1, index);

		index = info.findColumn("firstname");
		assertEquals(2, index);
	}


}
