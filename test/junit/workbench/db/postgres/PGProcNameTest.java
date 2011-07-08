/*
 * PGProcNameTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
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
		assertEquals(23, proc.getArguments().get(0).oid);

		PGProcName proc2 = new PGProcName("func_2", "23;23;1043", types);
		assertEquals("func_2", proc2.getName());
		assertEquals(3, proc2.getArguments().size());
		assertEquals(23, proc2.getArguments().get(0).oid);
		assertEquals(23, proc2.getArguments().get(1).oid);
		assertEquals(1043, proc2.getArguments().get(2).oid);
	}

	private Map<Long, PGType> getTypes()
	{
		Map<Long, PGType> result = new HashMap<Long, PGType>();
		result.put(16L, new PGType("bool", "boolean", 16));
		result.put(18L, new PGType("char", "char", 18));
		result.put(20L, new PGType("bigint", "bigint", 20));
		result.put(23L, new PGType("integer", "integer", 23));
		result.put(21L, new PGType("smallint", "smalling", 21));
		result.put(1043L, new PGType("varchar", "character varying", 1043));
		return result;
	}

}
