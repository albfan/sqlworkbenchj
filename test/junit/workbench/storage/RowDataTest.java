/*
 * RowDataTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.ResultSet;
import java.sql.Statement;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class RowDataTest extends TestCase
{

	public RowDataTest(String testName)
	{
		super(testName);
	}

	public void testTrimCharData()
		throws Exception
	{
		TestUtil util = new TestUtil(getName());
		WbConnection  con = util.getHSQLConnection("charTest");

		// HSQLDB does not pad a CHAR column to the defined length as defined
		// by the ANSI standard. But it does not remove trailing spaces either
		// so by storing trailing spaces, the trimCharData feature can be tested
		TestUtil.executeScript(con, 
			"CREATE TABLE char_test (char_data char(5), vchar varchar(10));\n" +
			"INSERT INTO char_test VALUES ('1    ', '1    ');\n" +
			"INSERT INTO char_test VALUES ('12   ', '12   ');\n" +
			"INSERT INTO char_test VALUES ('123  ', '123  ');\n" +
			"COMMIT;\n" +
			"");
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery("select char_data, vchar from char_test");
			ResultInfo info = new ResultInfo(rs.getMetaData(), con);
			RowData row = new RowData(info);
			row.setTrimCharData(true);
			rs.next();
			row.read(rs, info);
			String v = (String)row.getValue(0);
			assertEquals("1", v);
			v = (String)row.getValue(1);
			assertEquals("1    ", v);

			row.setTrimCharData(false);
			rs.next();
			row.read(rs, info);
			v = (String)row.getValue(0);
			assertEquals("12   ", v);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
			con.disconnect();
		}
		util.emptyBaseDirectory();
	}

	public void testBlobs()
	{
		RowData row = new RowData(2);
		row.setValue(0, new Integer(1));
		row.setValue(1, new byte[] {1,2,3});
		row.resetStatus();
		
		row.setValue(1, new byte[] {1,2,3});
		assertFalse(row.isColumnModified(1));
		assertFalse(row.isModified());
	}
	
	public void testResetStatus()
	{
		RowData row = new RowData(2);
		row.setValue(0, new Integer(42));
		row.setValue(1, "Test");
		row.resetStatus();

		row.setValue(0, new Integer(43));
		row.setValue(1, "Test2");
		assertTrue(row.isModified());
		
		row.resetStatusForColumn(1);
		assertTrue(row.isModified());
		assertTrue(row.isColumnModified(0));
		assertFalse(row.isColumnModified(1));
		
		row.resetStatusForColumn(0);
		assertFalse(row.isColumnModified(0));
		assertFalse(row.isColumnModified(1));
		assertFalse(row.isModified());
	}
	
	public void testChangeValues()
	{
		RowData row = new RowData(2);
		assertTrue(row.isNew());
		
		row.setValue(0, "123");
		row.setValue(1, new Integer(42));
		assertTrue(row.isNew());
		assertTrue(row.isModified());
		assertEquals("123", row.getValue(0));
		assertEquals(new Integer(42), row.getValue(1));

		assertEquals("123", row.getOriginalValue(0));
		assertEquals(new Integer(42), row.getOriginalValue(1));
		
		row.resetStatus();
		assertFalse(row.isModified());
		
		Object value = row.getValue(0);
		assertEquals(value, "123");
		value = row.getValue(1);
		assertEquals(value, new Integer(42));
		
		row.setValue(0, null);
		value = row.getValue(0);
		assertNull(value);
		assertEquals("123", row.getOriginalValue(0));
		assertTrue(row.isModified());
		
		row.resetStatus();
		row.setValue(0, "456");
		value = row.getValue(0);
		assertEquals(value, "456");
		assertNull(row.getOriginalValue(0));
		assertTrue(row.isColumnModified(0));
		
		row.setValue(0, "123");
		row.setValue(1, null);
		row.resetStatus();
		row.setValue(1, null);
		assertFalse(row.isModified());
	}
}
