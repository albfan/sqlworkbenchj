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
package workbench.db.mssql;

import java.sql.Types;
import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlServerDataConverterTest
	extends TestCase
{

	public SqlServerDataConverterTest(String testName)
	{
		super(testName);
	}

	public void testConvertsType()
	{
		SqlServerDataConverter converter = SqlServerDataConverter.getInstance();
		byte[] data = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0};
		Object newValue = converter.convertValue(Types.BINARY, "timestamp", data);
		assertEquals("0x0000000000000000", newValue.toString());

		data = new byte[] { 0, 0, 0, 0, 0, (byte)1, (byte)-36, (byte)-111 };
		newValue = converter.convertValue(Types.BINARY, "timestamp", data);
		assertEquals("0x000000000001dc91", newValue.toString());

		data = new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 };
		newValue = converter.convertValue(Types.BINARY, "timestamp", data);
		assertEquals("0x0101010101010101", newValue.toString());
	}

	public void testConvertValue()
	{
		SqlServerDataConverter converter = SqlServerDataConverter.getInstance();
		assertTrue(converter.convertsType(Types.BINARY, "timestamp"));
		assertFalse(converter.convertsType(Types.BLOB, "timestamp"));
		assertFalse(converter.convertsType(Types.BINARY, "image"));
	}
}
