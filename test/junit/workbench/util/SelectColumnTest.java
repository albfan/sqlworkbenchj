/*
 * SelectColumnTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectColumnTest
{

	@Test
	public void testGetColumnTable()
	{
		SelectColumn col = new SelectColumn("first_name");
		assertNull(col.getColumnTable());

		col = new SelectColumn("p.first_name as fname");
		assertEquals("p", col.getColumnTable());
		assertEquals("first_name", col.getObjectName());

		col = new SelectColumn("p.first_name");
		assertEquals("p", col.getColumnTable());

		col = new SelectColumn("myschema.mytable.first_name");
		assertEquals("myschema.mytable", col.getColumnTable());

		col = new SelectColumn("\"MySchema\".\"MyTable\".first_name");
		assertEquals("\"MySchema\".\"MyTable\"", col.getColumnTable());

	}
}
