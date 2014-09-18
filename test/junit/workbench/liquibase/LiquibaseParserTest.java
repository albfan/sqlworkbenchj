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

import workbench.sql.ParserType;

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

		LiquibaseParser parser = new LiquibaseParser(xmlFile, "UTF-8", new MessageBuffer(), ParserType.Standard);
		List<ChangeSetIdentifier> changeSets = parser.getChangeSets();
		assertEquals(2, changeSets.size());

		assertEquals("Arthur", changeSets.get(0).getAuthor());
		assertEquals("1", changeSets.get(0).getId());
		assertEquals("Create table", changeSets.get(0).getComment());

		assertEquals("2", changeSets.get(1).getId());
		assertEquals("Drop table", changeSets.get(1).getComment());
	}

	@Test
	public void testGetCustomSQL()
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
			"         <sql splitStatements=\"false\"> \n" +
			"            SELECT 42 FROM DUAL; \n" +
			"            COMMIT;" +
			"        </sql>\n" +
			"    </changeSet> \n" +
			"\n" +
			"    <changeSet id=\"2\" author=\"Tricia\"> \n" +
			"       <createTable tableName=\"person\"> \n" +
			"         <column name=\"id\" type=\"integer\"> \n" +
			"           <constraint primaryKey=\"true\" nullable=\"false\"/> \n" +
			"         </column> \n" +
			"       </createTable> \n" +
			"       <sql splitStatements=\"true\"> \n" +
			"            INSERT INTO person (id) VALUES (1); \n" +
			"            INSERT INTO person (id) VALUES (2); \n" +
			"            COMMIT;" +
			"        </sql>\n" +
			"    </changeSet> \n" +
			"\n" +
			"    <changeSet id=\"5\" author=\"Zaphod\"> \n" +
			"         <createProcedure> \n" +
			"            SELECT 'zaphod-5' FROM DUAL; \n" +
			"        </createProcedure>\n" +
			"    </changeSet> \n" +
			"\n" +
			"    <changeSet id=\"5\" author=\"Arthur\"> \n" +
			"         <createProcedure> \n" +
			"            SELECT 'arthur-5' FROM DUAL; \n" +
			"        </createProcedure>\n" +
			"    </changeSet> \n" +
			"\n" +
			"    <changeSet id=\"3\" author=\"Zaphod\"> \n" +
			"         <createProcedure> \n" +
			"            SELECT 3 FROM DUAL; \n" +
			"        </createProcedure>\n" +
			"    </changeSet> \n" +
			"  \n" +
			"    <changeSet id=\"4\" author=\"Ford\"> \n" +
			"       <sql splitStatements=\"true\" endDelimiter=\"GO\"> \n" +
			"            INSERT INTO person (id) VALUES (1)\n" +
			"            GO\n" +
			"\n" +
			"            INSERT INTO person (id) VALUES (2)\n" +
			"            GO\n" +
			"\n" +
			"            COMMIT\n" +
			"            GO\n" +
			"\n" +
			"        </sql>\n" +
			"    </changeSet> \n" +
			"\n" +
			"</databaseChangeLog>";

		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();
		WbFile xmlFile = new WbFile(util.getBaseDir(), "changelog.xml");

		TestUtil.writeFile(xmlFile, xml, "UTF-8");
		LiquibaseParser lb = new LiquibaseParser(xmlFile);
		List<String> sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("1"));
		assertNotNull(sql);
		assertEquals(1, sql.size());

		sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("2"));
		assertEquals(3, sql.size());

		sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("3"));
		assertNotNull(sql);
		assertEquals(1, sql.size());

		sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("1"), new ChangeSetIdentifier("2"));
		assertNotNull(sql);
		assertEquals(4, sql.size());

		sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("4"));
		assertNotNull(sql);
		assertEquals(3, sql.size());

		sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("5"));
		assertEquals(2, sql.size());

		sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("Arthur::5"));
		assertEquals(1, sql.size());
		assertEquals("SELECT 'arthur-5' FROM DUAL;", sql.get(0).trim());

		sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("3"), new ChangeSetIdentifier("Arthur::5"));
		assertEquals(2, sql.size());
		assertEquals("SELECT 'arthur-5' FROM DUAL;", sql.get(0).trim());
		assertEquals("SELECT 3 FROM DUAL;", sql.get(1).trim());

		sql = lb.getContentFromChangeSet(new ChangeSetIdentifier("Arthur", "*"));
		assertEquals(2, sql.size());
	}

	@Test
	public void testSqlFile()
		throws Exception
	{
		String sqlFoo = "create table foo (id integer not null primary key);";
		String sqlBar = "create table bar (id integer not null primary key);";

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
			"         <sql>drop table foo;</sql>\n" +
			"         <sqlFile path=\"create_table_foo.sql\" relativeToChangelogFile=\"true\" splitStatements=\"false\"/> \n" +
			"    </changeSet> \n" +
			"\n" +
			"    <changeSet id=\"2\" author=\"Arthur\"> \n" +
			"         <sql>drop table bar;</sql>\n" +
			"         <sqlFile path=\"create_table_bar.sql\" relativeToChangelogFile=\"true\" splitStatements=\"false\"/> \n" +
			"    </changeSet> \n" +
			"</databaseChangeLog>";

		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();
		WbFile xmlFile = new WbFile(util.getBaseDir(), "changelog.xml");

		TestUtil.writeFile(xmlFile, xml, "UTF-8");
		WbFile sqlFile1 = new WbFile(util.getBaseDir(), "create_table_foo.sql");
		TestUtil.writeFile(sqlFile1, sqlFoo, "UTF-8");

		WbFile sqlFile2 = new WbFile(util.getBaseDir(), "create_table_bar.sql");
		TestUtil.writeFile(sqlFile2, sqlBar, "UTF-8");

		LiquibaseParser lb = new LiquibaseParser(xmlFile);
		List<String> statements = lb.getContentFromChangeSet(new ChangeSetIdentifier("Arthur", "1"));
		assertNotNull(statements);
		assertEquals(2, statements.size());
		assertEquals("drop table foo", statements.get(0));
		assertEquals(sqlFoo, statements.get(1));
	}

	@Test
	public void testSqlFileWithOraDelim()
		throws Exception
	{
		String sql =
			"create table foo (id integer)\n" +
			"/\n" +
			"create index idx_foo on foo (id)\n" +
			"/\n";

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
			"         <sqlFile path=\"scripts/create_table.sql\" endDelimiter=\"/\" relativeToChangelogFile=\"true\" splitStatements=\"true\"/> \n" +
			"    </changeSet> \n" +
			"\n" +
			"</databaseChangeLog>";

		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();
		WbFile xmlFile = new WbFile(util.getBaseDir(), "changelog.xml");
		WbFile scriptDir = new WbFile(util.getBaseDir(), "scripts");
		scriptDir.mkdirs();

		TestUtil.writeFile(xmlFile, xml, "UTF-8");
		WbFile sqlFile = new WbFile(scriptDir, "create_table.sql");
		TestUtil.writeFile(sqlFile, sql, "UTF-8");

		LiquibaseParser lb = new LiquibaseParser(xmlFile);
		List<String> statements = lb.getContentFromChangeSet(new ChangeSetIdentifier("1"));
		assertNotNull(statements);
		assertEquals(2, statements.size());
		assertEquals("create table foo (id integer)", statements.get(0));
		assertEquals("create index idx_foo on foo (id)", statements.get(1));
	}

}
