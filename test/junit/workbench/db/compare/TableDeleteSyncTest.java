/*
 * TableDeleteSyncTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.compare;

import org.junit.Test;
import org.junit.Before;
import workbench.WbTestCase;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.ScriptParser;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDeleteSyncTest
	extends WbTestCase
{
	private WbConnection source;
	private WbConnection target;
	private int rowCount = 127;
	private int toDelete = 53;
	private TestUtil util;
	
	public TableDeleteSyncTest()
	{
		super("syncDelete");
		util = getTestUtil();
	}

	@Before
	public void setUp()
		throws Exception
	{
		Statement sourceStmt = null;
		Statement targetStmt = null;
		
		try
		{
			source = util.getConnection("sync_delete_reference");
			target = util.getConnection("sync_delete_target");
			sourceStmt= source.getSqlConnection().createStatement();
			String create = "CREATE table person (" +
				"id1 integer not null, " +
				"id2 integer not null, " +
				"firstname varchar(20), " +
				"lastname varchar(20), primary key(id1, id2))";
			sourceStmt.executeUpdate(create);

			targetStmt = target.getSqlConnection().createStatement();
			targetStmt.executeUpdate(create.replace(" person ", " person_t "));

			for (int i=0; i < rowCount; i++)
			{
				sourceStmt.executeUpdate("INSERT INTO person (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}

			for (int i=0; i < rowCount; i++)
			{
				targetStmt.executeUpdate("INSERT INTO person_t (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}
			
			for (int i=10000; i < 10000 + toDelete; i++)
			{
				targetStmt.executeUpdate("INSERT INTO person_t (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}
			source.commit();
			target.commit();
		}
		finally
		{
			SqlUtil.closeStatement(sourceStmt);
			SqlUtil.closeStatement(targetStmt);
		}
	}

	@Test
	public void testDeleteTarget()
		throws Exception
	{
		Statement check = null;
		ResultSet rs = null;
		try
		{
			TableDeleteSync sync = new TableDeleteSync(target, source);
			sync.setTableName(new TableIdentifier("person"), new TableIdentifier("person_t"));
			sync.doSync();
			long deleted = sync.getDeletedRows();
			assertEquals(toDelete, deleted);
			check = target.getSqlConnection().createStatement();
			rs = check.executeQuery("select count(*) from person_t");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Wrong row count in target table", rowCount, count);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

	@Test
	public void testCreateScript()
		throws Exception
	{
		try
		{
			TableDeleteSync sync = new TableDeleteSync(target, source);
			StringWriter writer = new StringWriter();
			sync.setOutputWriter(writer, "\n", "UTF-8");
			sync.setTableName(new TableIdentifier("person"), new TableIdentifier("person_t"));
			sync.doSync();
			
			String sql = writer.toString();
			ScriptParser parser = new ScriptParser(sql);
			int count = parser.getSize();
			assertEquals("Wrong DELETE count", toDelete, count);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
}
