/*
 * InputBufferTest.java
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
package workbench.console;


import workbench.WbTestCase;

import workbench.db.DBID;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class InputBufferTest
	extends WbTestCase
{
	public InputBufferTest()
	{
		super("InputBufferTest");
	}

	@Test
	public void testAddLine()
	{
		InputBuffer buffer = new InputBuffer();
		buffer.setDbId(DBID.Oracle.getId());

		boolean result = buffer.addLine("select * ");
		assertFalse(result);
		result = buffer.addLine("from mytable");
		assertFalse(result);
		result = buffer.addLine(";");
		assertTrue(result);

		buffer.clear();

		result = buffer.addLine("create or replace procedure proc");
		assertFalse(result);
		result = buffer.addLine("as ");
		assertFalse(result);
		result = buffer.addLine("begin ");
		assertFalse(result);
		result = buffer.addLine("  delete from test; ");
		assertFalse(result);
		result = buffer.addLine("end;");
		assertFalse(result);
		result = buffer.addLine("/");
		assertTrue(result);
	}

}
