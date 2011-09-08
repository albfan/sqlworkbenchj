/*
 * ColumnChangerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.List;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

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
	public void testPostgres()
	{
		DbSettings settings = new DbSettings("postgresql", "PostgreSQL");
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
		DbSettings settings = new DbSettings("oracle", "Oracle");
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
		DbSettings settings = new DbSettings("mysql", "MySQL");
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
		assertEquals("ALTER TABLE PERSON CHANGE FIRST_NAME FIRSTNAME VARCHAR(20) NOT NULL  COMMENT '';", alterScript.trim());
	}
}
