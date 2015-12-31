/*
 * TableStatementsTest.java
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
package workbench.db.importer;

import org.junit.Test;
import workbench.db.TableIdentifier;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableStatementsTest
{

	@Test
	public void testGetTableStatement()
	{
		TableIdentifier tbl = new TableIdentifier("tsch", "address");

		TableStatements stmt = new TableStatements("delete from ${table.name}", null);
		String sql = stmt.getPreStatement(tbl);
		assertEquals("delete from address", sql);
		assertNull(stmt.getPostStatement(tbl));

		stmt = new TableStatements("set identity insert ${table.expression} on", "set identity insert ${table.expression} off");
		assertEquals("set identity insert tsch.address on", stmt.getPreStatement(tbl));
		assertEquals("set identity insert tsch.address off", stmt.getPostStatement(tbl));

	}
}
