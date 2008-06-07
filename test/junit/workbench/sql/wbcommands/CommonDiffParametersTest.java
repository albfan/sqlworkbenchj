/*
 * CommonDiffParametersTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */

package workbench.sql.wbcommands;

import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.wbcommands.CommonDiffParameters.TableMapping;
import workbench.util.ArgumentParser;

/**
 *
 * @author support@sql-workbench.net
 */
public class CommonDiffParametersTest 
	extends TestCase 
{
	private WbConnection source;
	private WbConnection target;
	private TestUtil util;
    
	public CommonDiffParametersTest(String testName) 
	{
		super(testName);
		util = new TestUtil("diffParameterTest");
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		this.source = util.getConnection("diffParameterSource");
		this.target = util.getConnection("diffParameterTarget");
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		this.source.disconnect();
		this.target.disconnect();
		super.tearDown();
	}
	
	public void testGetMatchingTables()
		throws Exception
	{
		String sql = "create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" + 
			"create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" + 
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n";
		
		TestUtil.executeScript(source, sql);
		TestUtil.executeScript(target, sql);
		
		ArgumentParser cmdLine = new ArgumentParser();
		
		CommonDiffParameters params = new CommonDiffParameters(cmdLine);
		cmdLine.parse("-referenceTables=person, address, person_address");
		
		TableMapping result = params.getTables(source, target);
		assertEquals(3, result.referenceTables.size());
		assertEquals(3, result.targetTables.size());

		cmdLine.parse("-referenceTables=P*");
		result = params.getTables(source, target);
		assertEquals(2, result.referenceTables.size());
		assertEquals(2, result.targetTables.size());
		assertNotNull(TableIdentifier.findTableByName(result.referenceTables, "PERSON"));
		assertNotNull(TableIdentifier.findTableByName(result.referenceTables, "PERSON_ADDRESS"));
		
		assertNotNull(TableIdentifier.findTableByName(result.targetTables, "PERSON"));
		assertNotNull(TableIdentifier.findTableByName(result.targetTables, "PERSON_ADDRESS"));
	}

	public void testTableMapping()
		throws Exception
	{
		String sql1 = "create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" + 
			"create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" + 
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n";

		String sql2 = "create table t_person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" + 
			"create table t_address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" + 
			"create table l_person2address (person_id integer, address_id integer, primary key (person_id, address_id));\n";
		
		TestUtil.executeScript(source, sql1);
		TestUtil.executeScript(target, sql2);
		
		ArgumentParser cmdLine = new ArgumentParser();
		
		CommonDiffParameters params = new CommonDiffParameters(cmdLine);
		cmdLine.parse("-referenceTables=person, address, person_address -targetTables=t_person,t_address,l_person2address");
		
		TableMapping result = params.getTables(source, target);
		assertEquals(3, result.referenceTables.size());
		assertEquals(3, result.targetTables.size());
		assertEquals("person", result.referenceTables.get(0).getTableName().toLowerCase());
		assertEquals("t_person", result.targetTables.get(0).getTableName().toLowerCase());
	}

	public void testSchemaTables()
		throws Exception
	{
		String sql = "create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" + 
			"create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" + 
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n";

		TestUtil.executeScript(source, sql);
		TestUtil.executeScript(target, sql);
		
		ArgumentParser cmdLine = new ArgumentParser();
		
		CommonDiffParameters params = new CommonDiffParameters(cmdLine);
		cmdLine.parse("-excludeTables=person_address");
		
		TableMapping result = params.getTables(source, target);
		assertEquals(2, result.referenceTables.size());
		assertEquals(2, result.targetTables.size());
		assertEquals(result.targetTables.get(0).getTableName(), result.referenceTables.get(0).getTableName());
		assertEquals(result.targetTables.get(1).getTableName(), result.referenceTables.get(1).getTableName());
	}
	
}
