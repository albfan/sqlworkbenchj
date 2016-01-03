/*
 * CommonDiffParametersTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql.wbcommands;


import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.wbcommands.CommonDiffParameters.TableMapping;

import workbench.util.ArgumentParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	public void testMultipleSchemas()
		throws Exception
	{
		String script =
			"drop all objects;\n" +
			"create schema one;\n "  +
			"create schema two;\n " +
			"create table one.person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" +
			"create table one.address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
			"create table two.person_address (person_id integer, address_id integer, primary key (person_id, address_id)); \n" +
			"create table one.foobar (id integer primary key);\n" +
			"alter table two.person_address add constraint fk_adr foreign key (address_id) references one.address (address_id);\n" +
			"alter table two.person_address add constraint fk_per foreign key (person_id) references one.person (person_id);\n" +
			"create table two.foobar (id integer primary key);\n" +
			"commit;";

		try
		{
			util.prepareEnvironment();
			TestUtil.executeScript(source, script);
			TestUtil.executeScript(target, script);

			ArgumentParser cmdLine = new ArgumentParser();

			CommonDiffParameters params = new CommonDiffParameters(cmdLine);
			cmdLine.parse("-referenceTables=one.*,two.* -excludeTables=two.foobar");
			TableMapping mapping = params.getTables(source, target);

			assertEquals(4, mapping.referenceTables.size());
			assertEquals(mapping.referenceTables.size(), mapping.targetTables.size());
			for (int i=0; i < mapping.referenceTables.size(); i++)
			{
				TableIdentifier t1 = mapping.referenceTables.get(i);
				TableIdentifier t2 = mapping.targetTables.get(i);
				assertEquals(t1.getTableName(), t2.getTableName());
				assertEquals(t1.getSchema(), t2.getSchema());
			}
			assertNull(TableIdentifier.findTableByNameAndSchema(mapping.referenceTables, new TableIdentifier("TWO.FOOBAR")));

			cmdLine.parse("-referenceTables=one.* -targetTables=one.* -excludeTables=one.foobar");
			mapping = params.getTables(source, target);
			assertEquals(2, mapping.referenceTables.size());
			assertEquals(mapping.referenceTables.size(), mapping.targetTables.size());
			for (int i=0; i < mapping.referenceTables.size(); i++)
			{
				TableIdentifier t1 = mapping.referenceTables.get(i);
				TableIdentifier t2 = mapping.targetTables.get(i);
				assertEquals(t1.getTableName(), t2.getTableName());
				assertEquals(t1.getSchema(), t2.getSchema());
			}
			assertNull(TableIdentifier.findTableByName(mapping.referenceTables, new TableIdentifier("PERSON_ADDRESS")));
			assertNull(TableIdentifier.findTableByName(mapping.targetTables, new TableIdentifier("PERSON_ADDRESS")));

			cmdLine.parse("-referenceTables=one.*,two.* -targetTables=one.*,two.* -excludeTables=two.person_address");
			mapping = params.getTables(source, target);

			assertEquals(4, mapping.referenceTables.size());
			assertEquals(mapping.referenceTables.size(), mapping.targetTables.size());
			for (int i=0; i < mapping.referenceTables.size(); i++)
			{
				TableIdentifier t1 = mapping.referenceTables.get(i);
				TableIdentifier t2 = mapping.targetTables.get(i);
				assertEquals(t1.getTableName(), t2.getTableName());
				assertEquals(t1.getSchema(), t2.getSchema());
			}
			assertNull(TableIdentifier.findTableByNameAndSchema(mapping.referenceTables, new TableIdentifier("TWO", "PERSON_ADDRESS")));
			assertNull(TableIdentifier.findTableByNameAndSchema(mapping.targetTables, new TableIdentifier("TWO", "PERSON_ADDRESS")));
		}
		finally
		{
			source.disconnect();
			target.disconnect();
		}
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
	public void testSingleTable()
		throws Exception
	{
		String sql =
			"create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" +
			"create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id));\n" +
			"commit;\n";

		TestUtil.executeScript(source, sql);
		TestUtil.executeScript(target, sql);

		ArgumentParser cmdLine = new ArgumentParser();

		CommonDiffParameters params = new CommonDiffParameters(cmdLine);
		cmdLine.parse("-referenceSchema=public -targetSchema=public -referenceTables=person");

		TableMapping result = params.getTables(source, target);
		assertEquals(1, result.referenceTables.size());
		assertEquals(1, result.targetTables.size());
		assertEquals("person", result.referenceTables.get(0).getTableName().toLowerCase());
		assertEquals("person", result.targetTables.get(0).getTableName().toLowerCase());
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
		assertEquals(result.referenceTables.get(0).getTableName(), result.targetTables.get(0).getTableName());
		assertEquals(result.referenceTables.get(1).getTableName(), result.targetTables.get(1).getTableName());
	}

	@Test
	public void testNoParameters()
		throws Exception
	{
		String sql =
			"drop all objects;\n" +
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
		cmdLine.parse("");

		TableMapping result = params.getTables(source, target);
		assertEquals(4, result.referenceTables.size());
		assertEquals(4, result.targetTables.size());
	}

}
