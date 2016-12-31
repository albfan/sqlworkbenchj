/*
 * ColumnChangerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.sqltemplates;


import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnChangerTest
	extends WbTestCase
{

	public ColumnChangerTest()
	{
		super("ColumnChangerTest");
	}

	@Test
	public void testSqlServer()
	{
		DbSettings settings = new DbSettings("microsoft_sql_server");
		ColumnChanger changer = new ColumnChanger(settings);

		assertTrue(changer.canAddColumn());
		assertTrue(changer.canAlterType());
		assertTrue(changer.canChangeComment());
		assertTrue(changer.canChangeNullable());
		assertTrue(changer.canRenameColumn());

		TableIdentifier table = new TableIdentifier("PERSON");
		ColumnIdentifier oldCol = new ColumnIdentifier("FIRST_NAME", java.sql.Types.VARCHAR, false);
		oldCol.setDbmsType("VARCHAR(20)");
		oldCol.setIsNullable(false);

		ColumnIdentifier newCol = oldCol.createCopy();
		newCol.setDbmsType("VARCHAR(50)");

		List<String> sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(1, sqls.size());
		assertEquals("ALTER TABLE PERSON\n  ALTER COLUMN FIRST_NAME VARCHAR(50) NOT NULL", sqls.get(0));

		newCol = oldCol.createCopy();
		newCol.setIsNullable(true);
		sqls = changer.getAlterStatements(table, oldCol, newCol);
		System.out.println(sqls);
		assertEquals(1, sqls.size());
		assertEquals("ALTER TABLE PERSON\n  ALTER COLUMN FIRST_NAME VARCHAR(20) NULL", sqls.get(0));
	}

	@Test
	public void testPostgres()
	{
		DbSettings settings = new DbSettings("postgresql");
		ColumnChanger changer = new ColumnChanger(settings);

		assertTrue(changer.canAddColumn());
		assertTrue(changer.canAlterType());
		assertTrue(changer.canChangeComment());
		assertTrue(changer.canChangeNullable());
		assertTrue(changer.canRenameColumn());

		TableIdentifier table = new TableIdentifier("PERSON");
		ColumnIdentifier oldCol = new ColumnIdentifier("FIRST_NAME", java.sql.Types.VARCHAR, false);
		oldCol.setDbmsType("VARCHAR(20)");
		oldCol.setIsNullable(false);

		ColumnIdentifier newCol = oldCol.createCopy();
		newCol.setDbmsType("VARCHAR(50)");
		newCol.setColumnName("FIRSTNAME");
		newCol.setDefaultValue("'Arthur'");
		newCol.setIsNullable(true);

		List<String> sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(4, sqls.size());
		assertEquals("ALTER TABLE PERSON ALTER COLUMN FIRST_NAME TYPE VARCHAR(50)", sqls.get(0));
		assertEquals("ALTER TABLE PERSON ALTER COLUMN FIRST_NAME SET DEFAULT 'Arthur'", sqls.get(1));
		assertEquals("ALTER TABLE PERSON ALTER COLUMN FIRST_NAME DROP NOT NULL", sqls.get(2));
		assertEquals("ALTER TABLE PERSON RENAME COLUMN FIRST_NAME TO FIRSTNAME", sqls.get(3));

		oldCol.setDefaultValue("'Arthur'");
		newCol = oldCol.createCopy();
		newCol.setDefaultValue(null);

		sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(1, sqls.size());
		assertEquals("ALTER TABLE PERSON ALTER COLUMN FIRST_NAME DROP DEFAULT", sqls.get(0));

		oldCol = new ColumnIdentifier("FIRST_NAME", java.sql.Types.VARCHAR, false);
		oldCol.setDbmsType("VARCHAR(20)");
		oldCol.setIsNullable(false);
		newCol = oldCol.createCopy();
		newCol.setIsNullable(true);
		sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(1, sqls.size());
		assertEquals("ALTER TABLE PERSON ALTER COLUMN FIRST_NAME DROP NOT NULL", sqls.get(0));

		oldCol = new ColumnIdentifier("FIRST_NAME", java.sql.Types.VARCHAR, false);
		oldCol.setDbmsType("VARCHAR(20)");
		oldCol.setIsNullable(true);
		newCol = oldCol.createCopy();
		newCol.setIsNullable(false);
		sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(1, sqls.size());
		assertEquals("ALTER TABLE PERSON ALTER COLUMN FIRST_NAME SET NOT NULL", sqls.get(0));

		newCol = new ColumnIdentifier("PERSON_HOBBY", java.sql.Types.VARCHAR, false);
		newCol.setDbmsType("VARCHAR(25)");
		newCol.setDefaultValue("'Hitchhiking'");
		newCol.setComment("new comment");
		newCol.setIsNullable(true);
		sqls = changer.getAlterStatements(table, null, newCol);
		assertEquals(2, sqls.size());
		assertEquals("ALTER TABLE PERSON ADD COLUMN PERSON_HOBBY VARCHAR(25) DEFAULT 'Hitchhiking'", sqls.get(0).trim());
		assertEquals("COMMENT ON COLUMN PERSON.PERSON_HOBBY IS 'new comment'", sqls.get(1).trim());
	}

	@Test
	public void testOracle()
	{
		DbSettings settings = new DbSettings("oracle");
		ColumnChanger changer = new ColumnChanger(settings);

		assertTrue(changer.canAddColumn());
		assertTrue(changer.canAlterType());
		assertTrue(changer.canChangeComment());
		assertTrue(changer.canChangeNullable());
		assertTrue(changer.canRenameColumn());

		TableIdentifier table = new TableIdentifier("PERSON");
		ColumnIdentifier oldCol = new ColumnIdentifier("FIRST_NAME", java.sql.Types.VARCHAR, false);
		oldCol.setDbmsType("VARCHAR(20)");
		oldCol.setIsNullable(false);

		ColumnIdentifier newCol = oldCol.createCopy();
		newCol.setDbmsType("VARCHAR(50)");
		newCol.setColumnName("FIRSTNAME");
		newCol.setDefaultValue("'Arthur'");
		newCol.setIsNullable(true);

		List<String> sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(4, sqls.size());
		assertEquals("ALTER TABLE PERSON MODIFY FIRST_NAME VARCHAR(50)", sqls.get(0));
		assertEquals("ALTER TABLE PERSON MODIFY FIRST_NAME DEFAULT 'Arthur'", sqls.get(1));
		assertEquals("ALTER TABLE PERSON MODIFY FIRST_NAME NULL", sqls.get(2));
		assertEquals("ALTER TABLE PERSON RENAME COLUMN FIRST_NAME TO FIRSTNAME", sqls.get(3));

		oldCol.setDefaultValue("'Arthur'");
		newCol = oldCol.createCopy();
		newCol.setDefaultValue(null);

		sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(1, sqls.size());
		assertEquals("ALTER TABLE PERSON MODIFY FIRST_NAME DEFAULT NULL", sqls.get(0));

		oldCol = new ColumnIdentifier("FIRST_NAME", java.sql.Types.VARCHAR, false);
		oldCol.setDbmsType("VARCHAR(20)");
		oldCol.setIsNullable(false);
		newCol = oldCol.createCopy();
		newCol.setIsNullable(true);
		sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(1, sqls.size());
		assertEquals("ALTER TABLE PERSON MODIFY FIRST_NAME NULL", sqls.get(0));

		oldCol = new ColumnIdentifier("FIRST_NAME", java.sql.Types.VARCHAR, false);
		oldCol.setDbmsType("VARCHAR(20)");
		oldCol.setIsNullable(true);
		newCol = oldCol.createCopy();
		newCol.setIsNullable(false);
		sqls = changer.getAlterStatements(table, oldCol, newCol);
		assertEquals(1, sqls.size());
		assertEquals("ALTER TABLE PERSON MODIFY FIRST_NAME NOT NULL", sqls.get(0));

		newCol = new ColumnIdentifier("PERSON_HOBBY", java.sql.Types.VARCHAR, false);
		newCol.setDbmsType("VARCHAR(25)");
		newCol.setDefaultValue("'Hitchhiking'");
		newCol.setComment("new comment");
		newCol.setIsNullable(true);
		sqls = changer.getAlterStatements(table, null, newCol);
		assertEquals(2, sqls.size());
		assertEquals("ALTER TABLE PERSON ADD (PERSON_HOBBY VARCHAR(25) DEFAULT 'Hitchhiking' )", sqls.get(0).trim());
		assertEquals("COMMENT ON COLUMN PERSON.PERSON_HOBBY IS 'new comment'", sqls.get(1).trim());
	}

	@Test
	public void testMySQL()
	{
		DbSettings settings = new DbSettings("mysql");
		ColumnChanger changer = new ColumnChanger(settings);

		assertTrue(changer.canAddColumn());
		assertTrue(changer.canAlterType());
		assertTrue(changer.canChangeComment());
		assertTrue(changer.canChangeNullable());
		assertTrue(changer.canRenameColumn());

		TableIdentifier table = new TableIdentifier("PERSON");
		ColumnIdentifier oldCol = new ColumnIdentifier("FIRST_NAME", java.sql.Types.VARCHAR, false);
		oldCol.setDbmsType("VARCHAR(20)");
		oldCol.setIsNullable(false);

		ColumnIdentifier newCol = oldCol.createCopy();
		newCol.setColumnName("FIRSTNAME");
		newCol.setIsNullable(false);

		String alterScript = changer.getAlterScript(table, oldCol, newCol);
		assertEquals("ALTER TABLE PERSON CHANGE FIRST_NAME FIRSTNAME VARCHAR(20) NOT NULL ;", alterScript.trim());
	}
}
