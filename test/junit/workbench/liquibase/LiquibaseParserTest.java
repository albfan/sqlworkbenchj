/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.liquibase;


import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.util.MessageBuffer;
import workbench.util.WbFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class LiquibaseParserTest
	extends WbTestCase
{

	public LiquibaseParserTest()
	{
		super("LiquibaseParserTest");
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

@Test
	public void testSqlFileWithDelim()
		throws Exception
	{
		String xml =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
			"  \n" +
			"<databaseChangeLog \n" +
			"  xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog/1.9\" \n" +
			"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
			"  xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog/1.9 \n" +
			"         http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-1.9.xsd\"> \n" +
			"  \n" +
			"    <changeSet id=\"1\" author=\"Arthur\"> \n" +
			"         <comment>Create table</comment>\n " +
			"         <sql> \n" +
			"             create table foo (id integer); \n" +
			"         </sql> \n" +
			"    </changeSet> \n" +
			"\n" +
			"    <changeSet id=\"2\" author=\"Arthur\"> \n" +
			"         <comment>Drop table</comment>\n " +
			"         <sql>drop table bar;</sql>\n" +
			"    </changeSet> \n" +
			"</databaseChangeLog>";

		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();
		WbFile xmlFile = new WbFile(util.getBaseDir(), "changelog.xml");
		TestUtil.writeFile(xmlFile, xml, "UTF-8");

		LiquibaseParser parser = new LiquibaseParser(xmlFile, "UTF-8", new MessageBuffer());
		List<ChangeSetIdentifier> changeSets = parser.getChangeSets();
		assertEquals(2, changeSets.size());

		assertEquals("Arthur", changeSets.get(0).getAuthor());
		assertEquals("1", changeSets.get(0).getId());
		assertEquals("Create table", changeSets.get(0).getComment());

		assertEquals("2", changeSets.get(1).getId());
		assertEquals("Drop table", changeSets.get(1).getComment());
	}

}
