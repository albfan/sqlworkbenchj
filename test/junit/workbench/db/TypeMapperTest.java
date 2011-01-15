/*
 * TypeMapperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class TypeMapperTest
	extends WbTestCase
{

	public TypeMapperTest()
	{
		super("TypeMapperTest");
	}

	@Test
	public void testGetTypeName()
	{

		TypeMapper mapper = new TypeMapper();
		mapper.parseUserTypeMap("3:DOUBLE;2:NUMERIC($size, $digits);-1:VARCHAR2($size);93:datetime year to second");
		String type = mapper.getUserMapping(3, 1, 1);
		assertEquals("DOUBLE", type);
		type = mapper.getUserMapping(2, 11, 3);
		assertEquals("NUMERIC(11, 3)", type);
		type = mapper.getUserMapping(-1, 100, 0);
		assertEquals("VARCHAR2(100)", type);
		type = mapper.getUserMapping(93, 0, 0);
		assertEquals("datetime year to second", type);
	}
}
