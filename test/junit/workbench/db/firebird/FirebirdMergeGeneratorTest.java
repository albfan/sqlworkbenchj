/*
 * FirebirdMergeGeneratorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.firebird;

import java.sql.Types;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.storage.DataStore;
import static org.junit.Assert.*;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.RowDataContainer;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdMergeGeneratorTest
	extends WbTestCase
{
	public FirebirdMergeGeneratorTest()
	{
		super("FirebirdMergeGeneratorTest");
	}

	/**
	 * Test of generateMerge method, of class FirebirdMergeGenerator.
	 */
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

		boolean doFormat = Settings.getInstance().getDoFormatInserts();
		Settings.getInstance().setDoFormatInserts(false);
		try
		{
			FirebirdMergeGenerator generator = new FirebirdMergeGenerator();
			String sql = generator.generateMerge(ds);
			String expected =
				"UPDATE OR INSERT INTO person (id,fname,lname) VALUES (42,'Arthur','Dent')\n" +
				"MATCHING (id);\n" +
				"UPDATE OR INSERT INTO person (id,fname,lname) VALUES (24,'Ford','Prefect')\n" +
				"MATCHING (id);";
//			System.out.println("----- expected: \n" + expected + "\n****** result: \n" + sql + "\n-------");
			assertEquals(expected, sql.trim());
		}
		finally
		{
			Settings.getInstance().setDoFormatInserts(doFormat);
		}
	}


}
