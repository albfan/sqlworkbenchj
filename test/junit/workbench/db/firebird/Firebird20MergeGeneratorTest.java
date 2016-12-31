/*
 * Firebird20MergeGeneratorTest.java
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
package workbench.db.firebird;

import java.sql.Types;

import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Firebird20MergeGeneratorTest
	extends WbTestCase
{
	public Firebird20MergeGeneratorTest()
	{
		super("FirebirdMergeGeneratorTest");
	}

	/**
	 * Test of generateMerge method, of class FirebirdMerge20Generator.
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
			Firebird20MergeGenerator generator = new Firebird20MergeGenerator();
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
