/*
 * PostgresMergeGeneratorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.postgres;

import java.sql.Types;
import java.util.GregorianCalendar;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowDataContainer;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresMergeGeneratorTest
	extends WbTestCase
{
	public PostgresMergeGeneratorTest()
	{
		super("PostgresMergeGeneratorTest");
	}

	@Test
	public void testGenerateMerge()
	{
		ColumnIdentifier id = new ColumnIdentifier("id", Types.INTEGER);
		id.setIsPkColumn(true);
		ColumnIdentifier fname = new ColumnIdentifier("fname", Types.VARCHAR);
		ColumnIdentifier lname = new ColumnIdentifier("lname", Types.VARCHAR);
		ColumnIdentifier dob = new ColumnIdentifier("dob", Types.DATE);
		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, fname, lname, dob });

		TableIdentifier tbl = new TableIdentifier("person");
		info.setUpdateTable(tbl);
		DataStore ds = new DataStore(info);
		ds.forceUpdateTable(tbl);
		int row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(42));
		ds.setValue(row, 1, "Arthur");
		ds.setValue(row, 2, "Dent");

		ds.setValue(row, 3, new GregorianCalendar(2012, 0, 1).getTime());

		row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(24));
		ds.setValue(row, 1, "Ford");
		ds.setValue(row, 2, "Prefect");
		ds.setValue(row, 3, new GregorianCalendar(2012, 0, 2).getTime());

		PostgresMergeGenerator generator = new PostgresMergeGenerator();
		String sql = generator.generateMerge(ds);
		assertNotNull(sql);
		String expected =
			"with merge_data (id, fname, lname, dob) as \n" +
			"(\n" +
			"  values\n" +
			"    (42,'Arthur','Dent',DATE '2012-01-01'),\n" +
			"    (24,'Ford','Prefect',DATE '2012-01-02')\n" +
			"),\n" +
			"upsert as\n" +
			"(\n" +
			"  update person m\n" +
			"     set m.fname = md.fname,\n" +
			"         m.lname = md.lname,\n" +
			"         m.dob = md.dob\n" +
			"  from merge_data md\n" +
			"  where m.id = md.id\n" +
			"  returning m.*\n" +
			")\n" +
			"insert into person (id, fname, lname, dob)\n" +
			"select id, fname, lname, dob\n" +
			"from merge_data\n" +
			"where not exists (select 1\n" +
			"                  from upsert up\n" +
			"                  where up.id = md.id);";
//		System.out.println("----- expected: \n" + expected + "\n****** result: \n" + sql + "\n-------");
		assertEquals(expected, sql.trim());

		RowDataContainer selected = RowDataContainer.Factory.createContainer(ds, new int[] { 1 });
		sql = generator.generateMerge(selected);
		expected =
			"with merge_data (id, fname, lname, dob) as \n" +
			"(\n" +
			"  values\n" +
			"    (24,'Ford','Prefect',DATE '2012-01-02')\n" +
			"),\n" +
			"upsert as\n" +
			"(\n" +
			"  update person m\n" +
			"     set m.fname = md.fname,\n" +
			"         m.lname = md.lname,\n" +
			"         m.dob = md.dob\n" +
			"  from merge_data md\n" +
			"  where m.id = md.id\n" +
			"  returning m.*\n" +
			")\n" +
			"insert into person (id, fname, lname, dob)\n" +
			"select id, fname, lname, dob\n" +
			"from merge_data\n" +
			"where not exists (select 1\n" +
			"                  from upsert up\n" +
			"                  where up.id = md.id);";
		assertNotNull(sql);
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

		PostgresMergeGenerator generator = new PostgresMergeGenerator();
		StringBuilder result = new StringBuilder(100);
		String part = generator.generateMergeStart(ds);

		result.append(part);
		part = generator.addRow(info, ds.getRow(0), 0);
		result.append(part);

		part = generator.addRow(info, ds.getRow(1), 1);
		result.append(part);

		part = generator.generateMergeEnd(ds);
		result.append(part);

		String expected =
			"with merge_data (id, fname, lname) as \n" +
			"(\n" +
			"  values\n" +
			"    (42,'Arthur','Dent'),\n" +
			"    (24,'Ford','Prefect')\n" +
			"),\n" +
			"upsert as\n" +
			"(\n" +
			"  update person m\n" +
			"     set m.fname = md.fname,\n" +
			"         m.lname = md.lname\n" +
			"  from merge_data md\n" +
			"  where m.id = md.id\n" +
			"  returning m.*\n" +
			")\n" +
			"insert into person (id, fname, lname)\n" +
			"select id, fname, lname\n" +
			"from merge_data\n" +
			"where not exists (select 1\n" +
			"                  from upsert up\n" +
			"                  where up.id = md.id);";
//		System.out.println("----- expected: \n" + expected + "\n****** result: \n" + sql + "\n-------");
		assertEquals(expected, result.toString().trim());
	}
}
