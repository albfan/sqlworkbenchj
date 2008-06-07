/*
 * SqlLiteralFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.Types;
import java.util.Calendar;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlLiteralFormatterTest
	extends junit.framework.TestCase
{
	public SqlLiteralFormatterTest(String testName)
	{
		super(testName);
	}

	public void testGetJdbcLiteral()
	{
		try
		{
			SqlLiteralFormatter f = new SqlLiteralFormatter();
			f.setDateLiteralType(SqlLiteralFormatter.JDBC_DATE_LITERAL_TYPE);

			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 2002);
			c.set(Calendar.MONTH, 3); // April

			c.set(Calendar.DAY_OF_MONTH, 2);
			c.set(Calendar.HOUR_OF_DAY, 14);
			c.set(Calendar.MINUTE, 15);
			c.set(Calendar.SECOND, 16);

			java.sql.Time tm = new java.sql.Time(c.getTime().getTime());
			ColumnIdentifier timecol = new ColumnIdentifier("TIME_COL", Types.TIME);
			ColumnData data = new ColumnData(tm, timecol);
			CharSequence literal = f.getDefaultLiteral(data);
			assertEquals("JDBC time incorrect", "{t '14:15:16'}", literal);

			java.sql.Date dt = new java.sql.Date(c.getTime().getTime());
			ColumnIdentifier datecol = new ColumnIdentifier("DATE_COL", Types.DATE);
			data = new ColumnData(dt, datecol);
			literal = f.getDefaultLiteral(data);
			assertEquals("JDBC date incorrect", "{d '2002-04-02'}", literal);

			java.sql.Timestamp ts = new java.sql.Timestamp(c.getTime().getTime());
			ColumnIdentifier tscol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
			data = new ColumnData(ts, tscol);
			literal = f.getDefaultLiteral(data);
			assertEquals("JDBC timestamp incorrect", "{ts '2002-04-02 14:15:16.000'}", literal);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetAnsiLiteral()
	{
		try
		{
			SqlLiteralFormatter f = new SqlLiteralFormatter();
			f.setDateLiteralType(SqlLiteralFormatter.ANSI_DATE_LITERAL_TYPE);

			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 2002);
			c.set(Calendar.MONTH, 3); // April

			c.set(Calendar.DAY_OF_MONTH, 2);
			c.set(Calendar.HOUR_OF_DAY, 14);
			c.set(Calendar.MINUTE, 15);
			c.set(Calendar.SECOND, 16);

			java.sql.Time tm = new java.sql.Time(c.getTime().getTime());
			ColumnIdentifier timecol = new ColumnIdentifier("TIME_COL", Types.TIME);
			ColumnData data = new ColumnData(tm, timecol);
			CharSequence literal = f.getDefaultLiteral(data);
			assertEquals("ANSI time incorrect", "TIME '14:15:16'", literal);

			java.sql.Date dt = new java.sql.Date(c.getTime().getTime());
			ColumnIdentifier datecol = new ColumnIdentifier("DATE_COL", Types.DATE);
			data = new ColumnData(dt, datecol);
			literal = f.getDefaultLiteral(data);
			assertEquals("ANSI date incorrect", "DATE '2002-04-02'", literal);

			java.sql.Timestamp ts = new java.sql.Timestamp(c.getTime().getTime());
			ColumnIdentifier tscol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
			data = new ColumnData(ts, tscol);
			literal = f.getDefaultLiteral(data);
			assertEquals("ANSI timestamp incorrect", "TIMESTAMP '2002-04-02 14:15:16'", literal);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetOracleLiteral()
	{
		try
		{
			SqlLiteralFormatter f = new SqlLiteralFormatter();
			f.setDateLiteralType("oracle");

			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 2002);
			c.set(Calendar.MONTH, 3); // April

			c.set(Calendar.DAY_OF_MONTH, 2);
			c.set(Calendar.HOUR_OF_DAY, 14);
			c.set(Calendar.MINUTE, 15);
			c.set(Calendar.SECOND, 16);

			java.sql.Date dt = new java.sql.Date(c.getTime().getTime());
			ColumnIdentifier datecol = new ColumnIdentifier("DATE_COL", Types.DATE);
			ColumnData data = new ColumnData(dt, datecol);
			CharSequence literal = f.getDefaultLiteral(data);
			assertEquals("Oracle date incorrect", "to_date('2002-04-02', 'YYYY-MM-DD')", literal);

			java.sql.Timestamp ts = new java.sql.Timestamp(c.getTime().getTime());
			ColumnIdentifier tscol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
			data = new ColumnData(ts, tscol);
			literal = f.getDefaultLiteral(data);
			assertEquals("Oracle timestamp incorrect", "to_date('2002-04-02 14:15:16', 'YYYY-MM-DD HH24:MI:SS')", literal);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
