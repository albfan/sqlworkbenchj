/*
 * RowDataComparerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */package workbench.db.compare;

import junit.framework.TestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.storage.DmlStatement;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;
import workbench.storage.StatementFactory;
import workbench.util.SqlUtil;

/**
 * @author support@sql-workbench.net
 */
public class RowDataComparerTest
	extends TestCase
{

	public RowDataComparerTest(String testName)
	{
		super(testName);
	}

	public void testGetMigrationSql()
	{
		System.out.println("getMigrationSql");
		ColumnIdentifier[] cols = new ColumnIdentifier[3];
		cols[0] = new ColumnIdentifier("ID");
		cols[0].setIsPkColumn(true);
		cols[0].setIsNullable(false);

		cols[1] = new ColumnIdentifier("FIRSTNAME");
		cols[1].setIsPkColumn(false);
		cols[1].setIsNullable(false);

		cols[2] = new ColumnIdentifier("LASTNAME");
		cols[2].setIsPkColumn(false);
		cols[2].setIsNullable(false);

		ResultInfo info = new ResultInfo(cols);
		info.setUpdateTable(new TableIdentifier("PERSON"));

		StatementFactory factory = new StatementFactory(info, null);
		factory.setEmptyStringIsNull(true);
		factory.setIncludeNullInInsert(true);

		RowData reference = new RowData(info);
		reference.setValue(0, new Integer(42));
		reference.setValue(1, "Zaphod");
		reference.setValue(2, "Beeblebrox");
		reference.resetStatus();

		RowData target = new RowData(info);
		target.setValue(0, new Integer(42));
		target.setValue(1, "Arthur");
		target.setValue(2, "Beeblebrox");
		target.resetStatus();

		RowDataComparer instance = new RowDataComparer(reference, target);

		SqlLiteralFormatter formatter = new SqlLiteralFormatter();

		DmlStatement result = instance.getMigrationSql(factory);
		String sql = result.getExecutableStatement(formatter).toString();
		String verb = SqlUtil.getSqlVerb(sql);
		assertEquals("UPDATE", verb);
		assertTrue(sql.indexOf("SET FIRSTNAME = 'Zaphod'") > -1);
		
		instance = new RowDataComparer(reference, null);
		result = instance.getMigrationSql(factory);
		sql = result.getExecutableStatement(formatter).toString();
		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("INSERT", verb);
		assertTrue(sql.indexOf("(42, 'Zaphod', 'Beeblebrox')") > -1);
		
		reference = new RowData(info);
		reference.setValue(0, new Integer(42));
		reference.setValue(1, "Zaphod");
		reference.setValue(2, null);
		reference.resetStatus();

		target = new RowData(info);
		target.setValue(0, new Integer(42));
		target.setValue(1, "Zaphod");
		target.setValue(2, null);
		target.resetStatus();
		
		instance = new RowDataComparer(reference, target);
		result = instance.getMigrationSql(factory);
		assertNull(result);
		
	}
}
