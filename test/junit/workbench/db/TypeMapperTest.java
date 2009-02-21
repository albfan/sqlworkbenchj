/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */
package workbench.db;

import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class TypeMapperTest
	extends TestCase
{

	public TypeMapperTest(String testName)
	{
		super(testName);
	}

	public void testGetTypeName()
	{
		TypeMapper mapper = new TypeMapper();
		mapper.parseTypeMap("3:DOUBLE;2:NUMERIC($size, $digits);-1:VARCHAR2($size)");
		String type = mapper.getUserMapping(3, 1, 1);
		assertEquals("DOUBLE", type);
		type = mapper.getUserMapping(2, 11, 3);
		assertEquals("NUMERIC(11, 3)", type);
		type = mapper.getUserMapping(-1, 100, 0);
		assertEquals("VARCHAR2(100)", type);
	}
}
