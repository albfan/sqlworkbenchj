/*
 * PGProcNameTest.java
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
package workbench.db.postgres;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PGProcNameTest
{

	@Test
	public void testParse()
	{
		String procname = "my_func(integer, varchar)";
		PGTypeLookup types = new PGTypeLookup(getTypes());
		PGProcName proc = new PGProcName(procname, types);
		assertEquals("my_func", proc.getName());
		assertEquals(2, proc.getArguments().size());
		assertEquals(23, proc.getArguments().get(0).argType.getOid());

		PGProcName proc2 = new PGProcName("func_2", "23;23;1043", "i;i;i", types);
		assertEquals("func_2", proc2.getName());
		assertEquals(3, proc2.getArguments().size());
		assertEquals(23, proc2.getArguments().get(0).argType.getOid());
		assertEquals(23, proc2.getArguments().get(1).argType.getOid());
		assertEquals(1043, proc2.getArguments().get(2).argType.getOid());
	}

	private Map<Long, PGType> getTypes()
	{
		Map<Long, PGType> result = new HashMap<>();
		result.put(16L, new PGType("boolean", 16));
		result.put(18L, new PGType("char", 18));
		result.put(20L, new PGType("bigint", 20));
		result.put(23L, new PGType("integer", 23));
		result.put(21L, new PGType("smalling", 21));
		result.put(1043L, new PGType("character varying", 1043));
		return result;
	}

}
