/*
 * OracleMergeGeneratorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.Types;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleMergeGeneratorTest
	extends WbTestCase
{
	public OracleMergeGeneratorTest()
	{
		super("OracleMergeGeneratorTest");
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

		OracleMergeGenerator generator = new OracleMergeGenerator(null);
		List<String> result = generator.generateMerge(ds, null, 0);
		assertNotNull(result);
		assertEquals(1, result.size());
		String expected =
			"merge into person ut\n" +
			"using\n" +
			"(\n" +
			"  select 42 as id, 'Arthur' as fname, 'Dent' as lname from dual\n" +
			"  union all\n" +
			"  select 24, 'Ford', 'Prefect' from dual\n" +
			") md on (ut.id = md.id)\n" +
			"when matched then update\n" +
			"     set ut.fname = md.fname,\n" +
			"         ut.lname = md.lname\n" +
			"when not matched then\n" +
			"  insert (ut.id, ut.fname, ut.lname)\n" +
			"  values (md.id, md.fname, md.lname);";
		String sql = result.get(0);
//		System.out.println("----- expected: \n" + expected + "\n****** result: \n" + sql + "\n-------");
		assertEquals(expected, sql);

		result = generator.generateMerge(ds, new int[] {0}, 0);
		assertNotNull(result);
		assertEquals(1, result.size());
		expected =
			"merge into person ut\n" +
			"using\n" +
			"(\n" +
			"  select 42 as id, 'Arthur' as fname, 'Dent' as lname from dual\n" +
			") md on (ut.id = md.id)\n" +
			"when matched then update\n" +
			"     set ut.fname = md.fname,\n" +
			"         ut.lname = md.lname\n" +
			"when not matched then\n" +
			"  insert (ut.id, ut.fname, ut.lname)\n" +
			"  values (md.id, md.fname, md.lname);";
		sql = result.get(0);
//		System.out.println("----- expected: \n" + expected + "\n****** result: \n" + sql + "\n-------");
		assertEquals(expected, sql);
	}

}
