/*
 * SqlServerDataConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.Types;
import junit.framework.TestCase;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerDataConverterTest
{

	@Test
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

	@Test
	public void testConvertValue()
	{
		SqlServerDataConverter converter = SqlServerDataConverter.getInstance();
		assertTrue(converter.convertsType(Types.BINARY, "timestamp"));
		assertFalse(converter.convertsType(Types.BLOB, "timestamp"));
		assertFalse(converter.convertsType(Types.BINARY, "image"));
	}
}
