/*
 * StatementFactoryTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.Types;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.resource.Settings;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementFactoryTest
	extends WbTestCase
{
	public StatementFactoryTest()
	{
		super("StatementFactoryTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
		Settings.getInstance().setDoFormatInserts(false);
		Settings.getInstance().setDoFormatUpdates(false);
	}

	@Test
	public void testValueExpression()
		throws Exception
	{
		// Make sure the datatype defines a valuetemplate
		Settings.getInstance().setProperty("workbench.db.testmodedeb.valuetemplate.inet", "inet '%value%'");
		DbSettings forTest = new DbSettings("testmodedeb", "TestMode");

		ColumnIdentifier inetCol = new ColumnIdentifier("ip_address", java.sql.Types.OTHER);
		inetCol.setDbmsType("inet");
		inetCol.setIsPkColumn(true);

		ColumnIdentifier idCol = new ColumnIdentifier("id", java.sql.Types.INTEGER);
		idCol.setDbmsType("int8");

		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { inetCol, idCol });
		RowData row = new RowData(info);

		// this is not the correct class for an inet column, but for testing purposed this is enough
		row.setValue(0, "127.0.0.1");
		row.setValue(1, Integer.valueOf(42));
		row.resetStatus();

		row.setValue(0, "127.0.0.2");
		row.setValue(1, Integer.valueOf(43));

		TableIdentifier table = new TableIdentifier("inet_test");
		info.setUpdateTable(table);
		boolean oldFormat = Settings.getInstance().getDoFormatUpdates();
		try
		{
			Settings.getInstance().setDoFormatUpdates(false);

			StatementFactory factory = new StatementFactory(info, null);
			factory.setTestSettings(forTest);
			DmlStatement dml = factory.createUpdateStatement(row, false, "\n");
			assertEquals("UPDATE inet_test SET ip_address = inet '127.0.0.2', id = ? WHERE ip_address = inet '127.0.0.1'", dml.getSql());
			SqlLiteralFormatter formatter = new SqlLiteralFormatter();
			String expected = "UPDATE inet_test SET ip_address = inet '127.0.0.2', id = 43 WHERE ip_address = inet '127.0.0.1'";
			String result = dml.getExecutableStatement(formatter).toString();
			assertEquals(expected, result);
		}
		finally
		{
			Settings.getInstance().setDoFormatUpdates(oldFormat);
		}
	}

	@Test
	public void testCreateUpdateStatement()
	{
		String[] cols = new String[] { "key", "section", "firstname", "lastname" };
		int[] types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
		ResultInfo info = new ResultInfo(cols, types, null);
		info.setIsPkColumn(0, true);
		info.setIsPkColumn(1, true);
		TableIdentifier table = new TableIdentifier("person");

		info.setUpdateTable(table);
		StatementFactory factory = new StatementFactory(info, null);
		RowData data = new RowData(info.getColumnCount());
		data.setValue(0, new Integer(42));
		data.setValue(1, "start");
		data.setValue(2, "Zaphod");
		data.setValue(3, "Bla");
		data.resetStatus();

		data.setValue(2, "Beeblebrox");

		DmlStatement stmt = factory.createUpdateStatement(data, false, "\n");
		String sql = stmt.toString();
		assertEquals(true, sql.startsWith("UPDATE"));

		SqlLiteralFormatter formatter = new SqlLiteralFormatter();
		sql = stmt.getExecutableStatement(formatter).toString();
		assertEquals(true, sql.indexOf("key = 42") > -1);
		assertEquals(true, sql.indexOf("section = 'start'") > -1);
	}

	@Test
	public void testCreateInsertStatement()
	{
		String[] cols = new String[] { "key", "firstname", "lastname" };
		int[] types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR };

		ResultInfo info = new ResultInfo(cols, types, null);
		info.setIsPkColumn(0, true);
		TableIdentifier table = new TableIdentifier("person");

		info.setUpdateTable(table);
		StatementFactory factory = new StatementFactory(info, null);
		RowData data = new RowData(3);
		data.setValue(0, new Integer(42));
		data.setValue(1, "Zaphod");
		data.setValue(2, "Beeblebrox");

		DmlStatement stmt = factory.createInsertStatement(data, false, "\n");
		String sql = stmt.toString();
		assertEquals("Not an INSERT statement", true, sql.startsWith("INSERT"));

		SqlLiteralFormatter formatter = new SqlLiteralFormatter();
		sql = stmt.getExecutableStatement(formatter).toString();
		assertEquals("Wrong values inserted", true, sql.indexOf("VALUES (42, 'Zaphod', 'Beeblebrox')") > -1);
	}

	@Test
	public void testCreateInsertIdentityStatement()
	{
		String[] cols = new String[] { "key", "firstname", "lastname" };
		int[] types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR };

		ResultInfo info = new ResultInfo(cols, types, null);
		info.setIsPkColumn(0, true);
		info.getColumn(0).setDbmsType("identity");
		TableIdentifier table = new TableIdentifier("person");

		info.setUpdateTable(table);
		StatementFactory factory = new StatementFactory(info, null);
		RowData data = new RowData(3);
		data.setValue(0, new Integer(42));
		data.setValue(1, "Zaphod");
		data.setValue(2, "Beeblebrox");

		Settings.getInstance().setFormatInsertIgnoreIdentity(true);
		DmlStatement stmt = factory.createInsertStatement(data, false, "\n");
		String sql = stmt.toString();
		assertEquals("Not an INSERT statement", true, sql.startsWith("INSERT"));

		SqlLiteralFormatter formatter = new SqlLiteralFormatter();
		sql = stmt.getExecutableStatement(formatter).toString();
		assertEquals("Wrong values inserted", true, sql.indexOf("VALUES ('Zaphod', 'Beeblebrox')") > -1);

		Settings.getInstance().setFormatInsertIgnoreIdentity(false);
		stmt = factory.createInsertStatement(data, false, "\n");
		sql = stmt.getExecutableStatement(formatter).toString();
		assertEquals("Wrong values inserted", true, sql.indexOf("VALUES (42, 'Zaphod', 'Beeblebrox')") > -1);
	}

	@Test
	public void testCreateDeleteStatement()
	{
		String[] cols = new String[] { "key", "value", "firstname", "lastname" };
		int[] types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };

		ResultInfo info = new ResultInfo(cols, types, null);
		info.setIsPkColumn(0, true);
		info.setIsPkColumn(1, true);
		TableIdentifier table = new TableIdentifier("person");

		info.setUpdateTable(table);
		StatementFactory factory = new StatementFactory(info, null);
		RowData data = new RowData(info.getColumnCount());
		data.setValue(0, new Integer(42));
		data.setValue(1, "otherkey");
		data.setValue(2, "Zaphod");
		data.setValue(3, "Beeblebrox");
		data.resetStatus();

		DmlStatement stmt = factory.createDeleteStatement(data, false);
		String sql = stmt.toString();
		assertEquals("Not a delete statement", true, sql.startsWith("DELETE"));

		SqlLiteralFormatter formatter = new SqlLiteralFormatter();
		sql = stmt.getExecutableStatement(formatter).toString();
		assertEquals("Wrong WHERE clause created", true, sql.indexOf("key = 42") > -1);
		assertEquals("Wrong WHERE clause created", true, sql.indexOf("value = 'otherkey'") > -1);
	}
}
