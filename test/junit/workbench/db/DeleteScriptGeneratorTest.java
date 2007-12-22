/*
 * DeleteScriptGeneratorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.sql.ScriptParser;
import workbench.storage.ColumnData;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class DeleteScriptGeneratorTest
	extends TestCase
{
	private WbConnection dbConnection;
	public DeleteScriptGeneratorTest(String testName)
	{
		super(testName);
	}

	public void testGenerateScript()
	{
		try
		{
			createSimpleTables();
			DeleteScriptGenerator generator = new DeleteScriptGenerator(dbConnection);
			TableIdentifier table = new TableIdentifier("PERSON");
			generator.setTable(table);
			List<ColumnData> pk = new ArrayList<ColumnData>();
			ColumnData id = new ColumnData(new Integer(1), new ColumnIdentifier("ID"));
			pk.add(id);
			CharSequence sql = generator.getScriptForValues(pk);
//			System.out.println("***");
//			System.out.println(sql);
			ScriptParser parser = new ScriptParser(sql.toString());
			assertEquals(2, parser.getSize());
			String addressDelete = parser.getCommand(0);
			String addressTable = SqlUtil.getDeleteTable(addressDelete);
			Pattern p = Pattern.compile("\\s*person_id\\s*=\\s*1", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(addressDelete);
			assertEquals("ADDRESS", addressTable);
			assertTrue(m.find());
			
			String personTable = SqlUtil.getDeleteTable(parser.getCommand(1));
			assertEquals("PERSON", personTable);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGenerateStatements()
	{
		try
		{
			createMultiColumnPkTables();
			DeleteScriptGenerator generator = new DeleteScriptGenerator(dbConnection);
			TableIdentifier table = new TableIdentifier("BASE");
			generator.setTable(table);
			List<ColumnData> pk = new ArrayList<ColumnData>();
			pk.add(new ColumnData(new Integer(1), new ColumnIdentifier("BASE_ID1")));
			pk.add(new ColumnData(new Integer(1), new ColumnIdentifier("BASE_ID2")));
			
			List<String> statements = generator.getStatementsForValues(pk, true);
//			for (String s : statements)
//			{
//				System.out.println(s + ";\n");
//			}

			assertEquals(4, statements.size());
			
			Statement stmt = dbConnection.createStatement();
			for (String sql : statements)
			{
				stmt.executeUpdate(sql);
			}
			dbConnection.commit();
			
			String[] tables = new String[] { "BASE", "CHILD1", "CHILD2", "CHILD22" };
			
			for (String st : tables)
			{
				ResultSet rs = stmt.executeQuery("select count(*) from " + st);
				int count = -1;
				if (rs.next())
				{
					count = rs.getInt(1);
				}
				assertEquals("Wrong count in table: " + st, 1, count);
			}
			
			stmt.close();
			
			String sql = statements.get(3);
			String t = SqlUtil.getDeleteTable(sql);
			assertEquals("BASE", t);
			
			sql = statements.get(2);
			t = SqlUtil.getDeleteTable(sql);
			assertEquals("CHILD1", t);

			// Test when root table should not be included
			statements = generator.getStatementsForValues(pk, false);
			assertEquals(3, statements.size());
			sql = statements.get(2);
			t = SqlUtil.getDeleteTable(sql);
			assertEquals("CHILD1", t);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	private void createMultiColumnPkTables()
		throws Exception
	{
		String sql = "CREATE TABLE base \n" + 
					 "( \n" + 
					 "   base_id1  integer  NOT NULL, \n" + 
					 "   base_id2  integer  NOT NULL \n" + 
					 "); \n" + 
					 "ALTER TABLE base \n" + 
					 "   ADD CONSTRAINT base_pkey PRIMARY KEY (base_id1, base_id2); \n" + 
					 
					 "CREATE TABLE child1 \n" + 
					 "( \n" + 
					 "   child1_id1  integer  NOT NULL, \n" + 
					 "   child1_id2  integer  NOT NULL, \n" + 
					 "   c1base_id1  integer  NOT NULL, \n" + 
					 "   c1base_id2  integer  NOT NULL \n" + 
					 "); \n" +

					 "ALTER TABLE child1 \n" + 
					 "   ADD CONSTRAINT child1_pkey PRIMARY KEY (child1_id1, child1_id2); \n" + 
					 " \n" + 
					 "ALTER TABLE child1 \n" + 
					 "  ADD CONSTRAINT fk_child1 FOREIGN KEY (c1base_id1, c1base_id2) \n" + 
					 "  REFERENCES base (base_id1, base_id2); \n" +
					 
					 "CREATE TABLE child2 \n" + 
					 "( \n" + 
					 "   child2_id1  integer  NOT NULL, \n" + 
					 "   child2_id2  integer  NOT NULL, \n" + 
					 "   c2c1_id1  integer  NOT NULL, \n" + 
					 "   c2c1_id2  integer  NOT NULL \n" + 
					 "); \n" +

					 "ALTER TABLE child2 \n" + 
					 "   ADD CONSTRAINT child2_pkey PRIMARY KEY (child2_id1, child2_id2); \n" + 
					 " \n" + 
					 "ALTER TABLE child2 \n" + 
					 "  ADD CONSTRAINT fk_child2 FOREIGN KEY (c2c1_id1, c2c1_id2) \n" + 
					 "  REFERENCES child1 (child1_id1, child1_id2); \n" +

					 "CREATE TABLE child22 \n" + 
					 "( \n" + 
					 "   child22_id1  integer  NOT NULL, \n" + 
					 "   child22_id2  integer  NOT NULL, \n" + 
					 "   c22c1_id1  integer  NOT NULL, \n" + 
					 "   c22c1_id2  integer  NOT NULL \n" + 
					 "); \n" +

					 "ALTER TABLE child22 \n" + 
					 "   ADD CONSTRAINT child22_pkey PRIMARY KEY (child22_id1, child22_id2); \n" + 
					 " \n" + 
					 "ALTER TABLE child22 \n" + 
					 "  ADD CONSTRAINT fk_child22 FOREIGN KEY (c22c1_id1, c22c1_id2) \n" + 
					 "  REFERENCES child1 (child1_id1, child1_id2); \n"
					 ;
		
		TestUtil util = new TestUtil("DependencyDeleter");
		this.dbConnection = util.getConnection();
		TestUtil.executeScript(dbConnection, sql);
		Statement stmt = this.dbConnection.createStatement();
		stmt.executeUpdate("insert into base (base_id1, base_id2) values (1,1)");
		stmt.executeUpdate("insert into base (base_id1, base_id2) values (2,2)");
		
		stmt.executeUpdate("insert into child1 (child1_id1, child1_id2, c1base_id1, c1base_id2) values (11,11,1,1)");
		stmt.executeUpdate("insert into child1 (child1_id1, child1_id2, c1base_id1, c1base_id2) values (12,12,2,2)");
		
		stmt.executeUpdate("insert into child2 (child2_id1, child2_id2, c2c1_id1, c2c1_id2) values (101,101,11,11)");
		stmt.executeUpdate("insert into child2 (child2_id1, child2_id2, c2c1_id1, c2c1_id2) values (102,102,12,12)");

		stmt.executeUpdate("insert into child22 (child22_id1, child22_id2, c22c1_id1, c22c1_id2) values (201,201,11,11)");
		stmt.executeUpdate("insert into child22 (child22_id1, child22_id2, c22c1_id1, c22c1_id2) values (202,202,12,12)");
		dbConnection.commit();
		stmt.close();
	}

	
	private void createSimpleTables()
		throws Exception
	{
		String sql = 
					 "CREATE TABLE address \n" +
					 "( \n" + 
					 "   id           integer  NOT NULL, \n" + 
					 "   address_data varchar(100) not null, \n" + 
					 "   person_id    integer \n" + 
					 "); \n" + 
					 
					 "ALTER TABLE address \n" + 
					 "   ADD CONSTRAINT address_pkey PRIMARY KEY (id); \n" +
					 
					 "CREATE TABLE person \n" + 
					 "( \n" + 
					 "   id        integer         NOT NULL, \n" + 
					 "   firstname varchar(50), \n" + 
					 "   lastname  varchar(50) \n" + 
					 "); \n" + 
					 
					 "ALTER TABLE person \n" + 
					 "   ADD CONSTRAINT person_pkey PRIMARY KEY (id); \n" + 
					 " \n" + 
					 
					 "ALTER TABLE address \n" + 
					 "  ADD CONSTRAINT fk_pers FOREIGN KEY (person_id) \n" + 
					 "  REFERENCES person (id); \n";
		
		
		TestUtil util = new TestUtil("DeleteScriptGenerator");
		this.dbConnection = util.getConnection();
		TestUtil.executeScript(dbConnection, sql);
	}
	
}
