/*
 * CommonDiffParametersTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.wbcommands.CommonDiffParameters.TableMapping;
import workbench.util.ArgumentParser;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class CommonDiffParametersTest
	extends WbTestCase
{
	private WbConnection source;
	private WbConnection target;
	private TestUtil util;

	public CommonDiffParametersTest()
	{
		super("CommonDiffParametersTest");
		util = getTestUtil();
	}

	@Before
	public void setUp()
		throws Exception
	{
		this.source = util.getConnection("diffParameterSource");
		this.target = util.getConnection("diffParameterTarget");
	}

	@After
	public void tearDown()
		throws Exception
	{
		this.source.disconnect();
		this.target.disconnect();
	}

	@Test
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

	@Test
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

	@Test
	public void testExclude()
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

	@Test
	public void testSchemaParameter()
		throws Exception
	{
		String sql = "drop all objects;\n" +
			"create schema difftest;\n" +
			"set schema difftest;\n" +
			"create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" +
			"create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id)); \n" +
			"create table dummy (some_id integer); \n" +
			"alter table person_address add constraint fk_adr foreign key (address_id) references address (address_id);\n" +
			"alter table person_address add constraint fk_per foreign key (person_id) references person (person_id);\n" +
			"commit;\n";

		TestUtil.executeScript(source, sql);
		TestUtil.executeScript(target, sql);

		ArgumentParser cmdLine = new ArgumentParser();

		CommonDiffParameters params = new CommonDiffParameters(cmdLine);
		cmdLine.parse("-referenceSchema=difftest -targetSchema=difftest -referenceTables=person");

		TableMapping result = params.getTables(source, target);
		assertEquals(1, result.referenceTables.size());
		assertEquals(1, result.targetTables.size());

		cmdLine = new ArgumentParser();
		params = new CommonDiffParameters(cmdLine);
		cmdLine.parse("-referenceTables=difftest.person,difftest.address -targetTables=difftest.person,difftest.address");
		result = params.getTables(source, target);
		assertEquals(2, result.referenceTables.size());
		assertEquals(2, result.targetTables.size());
		assertTrue("Wrong tables", result.referenceTables.get(0).equals(result.targetTables.get(0)));
		assertTrue("Wrong tables", result.referenceTables.get(1).equals(result.targetTables.get(1)));
	}

}
