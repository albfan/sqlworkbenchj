/*
 * SqlRowDataConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import org.junit.Test;
import workbench.WbTestCase;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.ScriptParser;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlRowDataConverterTest
	extends WbTestCase
{

	public SqlRowDataConverterTest()
	{
		super("SqlRowDataConverterTest");
	}

	@Test
	public void testSqlGeneration()
		throws Exception
	{
		WbConnection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			TestUtil util = new TestUtil("testDateLiterals");
			util.prepareEnvironment();

			con = util.getConnection("sqlConverterTest");
			String script =
				"CREATE TABLE person (id integer primary key, name varchar(20));\n" +
				"CREATE VIEW v_Person AS SELECT * from person;";
			TestUtil.executeScript(con, script);

			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM v_person");
			ResultInfo info = new ResultInfo(rs.getMetaData(), con);

			SqlRowDataConverter converter = new SqlRowDataConverter(null);
			converter.setOriginalConnection(con);
			converter.setResultInfo(info);
			converter.setCreateTable(true);
			converter.setAlternateUpdateTable(new TableIdentifier("MYTABLE"));
			StrBuffer start = converter.getStart();
			assertNotNull(start);
			String sql = start.toString();
			SqlUtil.DdlObjectInfo ddl = SqlUtil.getDDLObjectInfo(sql);
			assertEquals(ddl.objectName, "MYTABLE");
			assertEquals(ddl.objectType, "TABLE");
		}
		finally
		{
			con.disconnect();
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Test
	public void testConvert()
	{
		try
		{
			TestUtil util = new TestUtil("testDateLiterals");
			util.prepareEnvironment();

			String[] cols = new String[]
			{
				"char_col", "int_col", "date_col", "ts_col"
			};
			int[] types = new int[]
			{
				Types.VARCHAR, Types.INTEGER, Types.DATE, Types.TIMESTAMP
			};
			int[] sizes = new int[]
			{
				10, 10, 10, 10
			};

			ResultInfo info = new ResultInfo(cols, types, sizes);
			TableIdentifier tbl = new TableIdentifier("MYTABLE");
			info.setUpdateTable(tbl);

			SqlRowDataConverter converter = new SqlRowDataConverter(null);
			converter.setResultInfo(info);

			info.getColumn(0).setIsPkColumn(true);

			RowData data = new RowData(info);
			data.setValue(0, "data1");
			data.setValue(1, new Integer(42));
			Calendar c = Calendar.getInstance();
			c.set(2006, 9, 26, 17, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			java.util.Date d = c.getTime();
			data.setValue(2, c.getTime());
			java.sql.Timestamp ts = new java.sql.Timestamp(d.getTime());
			data.setValue(3, ts);
			data.resetStatus();

			converter.setDateLiteralType(SqlLiteralFormatter.JDBC_DATE_LITERAL_TYPE);
			converter.setCreateInsert();
			String line = converter.convertRowData(data, 0).toString().trim();
			String verb = SqlUtil.getSqlVerb(line);
			assertEquals("No insert generated", "INSERT", verb);

			assertEquals("JDBC date literal not found", true, line.indexOf("{d '2006-10-26'}") > -1);
			assertEquals("JDBC timestamp literal not found", true, line.indexOf("{ts '2006-10-26 ") > -1);

			converter.setDateLiteralType(SqlLiteralFormatter.ANSI_DATE_LITERAL_TYPE);
			line = converter.convertRowData(data, 0).toString().trim();
			assertEquals("ANSI date literal not found", true, line.indexOf("DATE '2006-10-26'") > -1);
			assertEquals("ANSI timestamp literal not found", true, line.indexOf("TIMESTAMP '2006-10-26") > -1);

			converter.setCreateUpdate();
			line = converter.convertRowData(data, 0).toString().trim();

			verb = SqlUtil.getSqlVerb(line);
			assertEquals("No UPDATE generated", "UPDATE", verb);
			assertEquals("Wrong WHERE statement", true, line.endsWith("WHERE char_col = 'data1';"));

			List columns = new ArrayList();
			columns.add(info.getColumn(0));
			columns.add(info.getColumn(1));
			converter.setColumnsToExport(columns);
			line = converter.convertRowData(data, 0).toString().trim();
			assertEquals("date_col included", -1, line.indexOf("date_col ="));
			assertEquals("ts_col included", -1, line.indexOf("ts_col ="));
			assertEquals("int_col not updated", true, line.indexOf("SET int_col = 42") > -1);

			converter.setCreateInsertDelete();
			line = converter.convertRowData(data, 0).toString().trim();
			ScriptParser p = new ScriptParser(line);
			int count = p.getSize();
			assertEquals("Not enough statements generated", 2, count);
			String sql = p.getCommand(0);
			verb = SqlUtil.getSqlVerb(sql);
			assertEquals("DELETE not first statement", "DELETE", verb);

			sql = p.getCommand(1);
			verb = SqlUtil.getSqlVerb(sql);
			assertEquals("INSERT not second statement", "INSERT", verb);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
