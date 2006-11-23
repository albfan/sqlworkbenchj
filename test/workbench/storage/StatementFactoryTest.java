/*
 * StatementFactoryTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.Types;
import junit.framework.*;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class StatementFactoryTest extends TestCase
{
	public StatementFactoryTest(String testName)
	{
		super(testName);
		try
		{
			TestUtil util = new TestUtil();
			util.prepareEnvironment();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void setUp()
		throws Exception
	{
		Settings.getInstance().setDoFormatInserts(false);
		Settings.getInstance().setDoFormatUpdates(false);
	}
	
	public void testCreateUpdateStatement()
	{
		try
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
			sql = stmt.getExecutableStatement(formatter);
			assertEquals(true, sql.indexOf("key = 42") > -1);
			assertEquals(true, sql.indexOf("section = 'start'") > -1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testCreateInsertStatement()
	{
		try
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
			sql = stmt.getExecutableStatement(formatter);
			assertEquals("Wrong values inserted", true, sql.indexOf("VALUES (42, 'Zaphod', 'Beeblebrox')") > -1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testCreateInsertIdentityStatement()
	{
		try
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
			sql = stmt.getExecutableStatement(formatter);
			assertEquals("Wrong values inserted", true, sql.indexOf("VALUES ('Zaphod', 'Beeblebrox')") > -1);
			
			Settings.getInstance().setFormatInsertIgnoreIdentity(false);
			stmt = factory.createInsertStatement(data, false, "\n");
			sql = stmt.getExecutableStatement(formatter);
			assertEquals("Wrong values inserted", true, sql.indexOf("VALUES (42, 'Zaphod', 'Beeblebrox')") > -1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testCreateDeleteStatement()
	{
		try
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
			sql = stmt.getExecutableStatement(formatter);
			assertEquals("Wrong WHERE clause created", true, sql.indexOf("key = 42") > -1);
			assertEquals("Wrong WHERE clause created", true, sql.indexOf("value = 'otherkey'") > -1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}	
}
