/*
 * AnsiSQLMergeGeneratorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.Types;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.WbTestCase;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowDataContainer;

/**
 *
 * @author Thomas Kellerer
 */
public class AnsiSQLMergeGeneratorTest
	extends WbTestCase
{
	public AnsiSQLMergeGeneratorTest()
	{
		super("AnsiSQLMergeGeneratorTest");
	}

	@Test
	public void testGenerateMerge()
	{
		ColumnIdentifier id = new ColumnIdentifier("id", Types.INTEGER);
		id.setIsPkColumn(true);
		ColumnIdentifier fname = new ColumnIdentifier("fname", Types.VARCHAR);
		ColumnIdentifier lname = new ColumnIdentifier("lname", Types.VARCHAR);
		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, fname, lname });

		TableIdentifier tbl = new TableIdentifier("person");
		info.setUpdateTable(tbl);
		DataStore ds = new DataStore(info);
		ds.forceUpdateTable(tbl);
		int row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(42));
		ds.setValue(row, 1, "Arthur");
		ds.setValue(row, 2, "Dent");

		row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(24));
		ds.setValue(row, 1, "Ford");
		ds.setValue(row, 2, "Prefect");

		AnsiSQLMergeGenerator generator = new AnsiSQLMergeGenerator();
		String sql = generator.generateMerge(ds);
		assertNotNull(sql);
		String expected =
			"MERGE INTO person ut\n" +
			"USING (\n" +
			"  VALUES\n" +
			"    (42, 'Arthur', 'Dent'),\n" +
			"    (24, 'Ford', 'Prefect')\n" +
			") AS md (id, fname, lname) ON (ut.id = md.id)\n" +
			"WHEN MATCHED THEN UPDATE\n" +
			"     SET fname = md.fname,\n" +
			"         lname = md.lname\n" +
			"WHEN NOT MATCHED THEN\n" +
			"  INSERT (id, fname, lname)\n" +
			"  VALUES (md.id, md.fname, md.lname);";
//		System.out.println("----- expected: \n" + expected + "\n****** result: \n" + sql + "\n-------");
		assertEquals(expected, sql.trim());

		RowDataContainer selected = RowDataContainer.Factory.createContainer(ds, new int[] {0});
		sql = generator.generateMerge(selected);
		assertNotNull(sql);

		expected =
			"MERGE INTO person ut\n" +
			"USING (\n" +
			"  VALUES\n" +
			"    (42, 'Arthur', 'Dent')\n" +
			") AS md (id, fname, lname) ON (ut.id = md.id)\n" +
			"WHEN MATCHED THEN UPDATE\n" +
			"     SET fname = md.fname,\n" +
			"         lname = md.lname\n" +
			"WHEN NOT MATCHED THEN\n" +
			"  INSERT (id, fname, lname)\n" +
			"  VALUES (md.id, md.fname, md.lname);";
//		System.out.println("----- expected: \n" + expected + "\n****** result: \n" + sql + "\n-------");
		assertEquals(expected, sql.trim());
	}

	@Test
	public void testIncremental()
	{
		ColumnIdentifier id = new ColumnIdentifier("id", Types.INTEGER);
		id.setIsPkColumn(true);
		ColumnIdentifier fname = new ColumnIdentifier("fname", Types.VARCHAR);
		ColumnIdentifier lname = new ColumnIdentifier("lname", Types.VARCHAR);
		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, fname, lname });

		TableIdentifier tbl = new TableIdentifier("person");
		info.setUpdateTable(tbl);
		DataStore ds = new DataStore(info);
		ds.forceUpdateTable(tbl);
		int row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(42));
		ds.setValue(row, 1, "Arthur");
		ds.setValue(row, 2, "Dent");

		row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(24));
		ds.setValue(row, 1, "Ford");
		ds.setValue(row, 2, "Prefect");

		AnsiSQLMergeGenerator generator = new AnsiSQLMergeGenerator();
		StringBuilder result = new StringBuilder(100);
		String part = generator.generateMergeStart(ds);
		result.append(part);
		part = generator.addRow(info, ds.getRow(0), 0);
		result.append(part);

		part = generator.addRow(info, ds.getRow(1), 1);
		result.append(part);

		result.append(generator.generateMergeEnd(ds));

		String expected =
			"MERGE INTO person ut\n" +
			"USING (\n" +
			"  VALUES\n" +
			"    (42, 'Arthur', 'Dent'),\n" +
			"    (24, 'Ford', 'Prefect')\n" +
			") AS md (id, fname, lname) ON (ut.id = md.id)\n" +
			"WHEN MATCHED THEN UPDATE\n" +
			"     SET fname = md.fname,\n" +
			"         lname = md.lname\n" +
			"WHEN NOT MATCHED THEN\n" +
			"  INSERT (id, fname, lname)\n" +
			"  VALUES (md.id, md.fname, md.lname);";
//		System.out.println("----- expected: \n" + expected + "\n****** result: \n" + result.toString() + "\n-------");
		assertEquals(expected, result.toString().trim());
	}
}
