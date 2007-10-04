/*
 * DependencyDeleterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.sql.ScriptParser;
import workbench.storage.ColumnData;
import workbench.storage.DmlStatement;
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
			Pattern p = Pattern.compile("\\(\\s*person_id\\s*=\\s*1\\)", Pattern.CASE_INSENSITIVE);
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

//	public void testGenerateStatements()
//	{
//		try
//		{
//			createTables();
//			DeleteScriptGenerator generator = new DeleteScriptGenerator(dbConnection);
//			TableIdentifier table = new TableIdentifier("PERSON");
//			generator.setTable(table);
//			List<ColumnData> pk = new ArrayList<ColumnData>();
//			pk.add(new ColumnData(new Integer(1), new ColumnIdentifier("ID")));
//			//pk.add(new ColumnData(new Integer(1), new ColumnIdentifier("ADDRESS_ID")));
//			
//			List<DmlStatement> statements = generator.getStatementsForValues(pk);
//
//			//assertEquals(8, statements.size());
//			for (DmlStatement dml : statements)
//			{
//				System.out.println(dml);
//			}
//			//String addressDelete = statements.get(0).toString();
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}
	
	private void createTables()
		throws Exception
	{
		String sql = 
					 "CREATE TABLE address \n" +
					 "( \n" + 
					 "   id        integer  NOT NULL, \n" + 
					 "   address_data varchar(100) not null, \n" + 
					 "   person_id integer, \n" + 
					 "   country_code varchar(3) \n" + 
					 "); \n" + 
					 
					 "ALTER TABLE address \n" + 
					 "   ADD CONSTRAINT address_pkey PRIMARY KEY (id); \n" +

					 "CREATE TABLE home_address \n" +
					 "( \n" + 
					 "   address_id integer NOT NULL, \n" + 
					 "   person_id  integer not null \n" + 
					 "); \n" + 

					 "ALTER TABLE home_address \n" + 
					 "   ADD CONSTRAINT home_adr_pkey PRIMARY KEY (address_id, person_id); \n" +
					 
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
					 "  REFERENCES person (id); \n" + 
		
					 "ALTER TABLE home_address \n" + 
					 "  ADD CONSTRAINT fk_had_pers FOREIGN KEY (person_id) \n" + 
					 "  REFERENCES person (id); \n" + 

					 "ALTER TABLE home_address \n" + 
					 "  ADD CONSTRAINT fk_had_addr FOREIGN KEY (address_id) \n" + 
					 "  REFERENCES address (id); \n";
		
		TestUtil util = new TestUtil("DeleteScriptGenerator");
		this.dbConnection = util.getConnection();
		TestUtil.executeScript(dbConnection, sql);
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