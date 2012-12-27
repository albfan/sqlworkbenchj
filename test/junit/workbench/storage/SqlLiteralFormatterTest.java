/*
 * SqlLiteralFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.storage;

import java.sql.Types;
import java.util.Calendar;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlLiteralFormatterTest
	extends WbTestCase
{

	public SqlLiteralFormatterTest()
	{
		super("SqlLiteralFormatterTest");
	}

	@Test
	public void testNVarchar()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		ColumnIdentifier col = new ColumnIdentifier("SOME_STRING", Types.VARCHAR);
		ColumnData data = new ColumnData("hello", col);
		CharSequence literal = f.getDefaultLiteral(data);
		assertNotNull(literal);
		assertEquals("'hello'", literal.toString());

		col = new ColumnIdentifier("SOME_STRING", Types.NVARCHAR);
		data = new ColumnData("hello", col);
		literal = f.getDefaultLiteral(data);
		assertNotNull(literal);
		assertEquals("N'hello'", literal.toString());
	}
	
	@Test
	public void testGetJdbcLiteral()
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

	@Test
	public void testGetAnsiLiteral()
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

	@Test
	public void testGetOracleLiteral()
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
}
