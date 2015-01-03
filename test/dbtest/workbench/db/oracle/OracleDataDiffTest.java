/*
 * OracleSchemaDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.oracle;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbDataDiff;

import workbench.util.FileUtil;
import workbench.util.WbFile;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class OracleDataDiffTest
	extends WbTestCase
{

	public OracleDataDiffTest()
	{
		super("OracleSchemaDiffTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		OracleTestUtil.initTestCase(OracleTestUtil.SCHEMA2_NAME);
	}

	private void createTables(WbConnection con1, WbConnection con2)
		throws Exception
	{
		String sql =
			"create table person (\n" +
			"   id integer not null primary key, " +
			"   firstname varchar(100), " +
			"   lastname varchar(100) not null\n" +
			"); \n" +
			"create table spaceship (\n" +
			"   id integer not null primary key, \n" +
			"   name varchar(100) not null\n " +
			");\n" +
			"create table owns_ship (\n" +
			"   person_id integer not null references person, \n" +
			"   ship_id integer not null references spaceship, \n" +
			"   primary key (person_id, ship_id)" +
			");";


		TestUtil.executeScript(con1, sql);
		TestUtil.executeScript(con2, sql);

		String insert1 =
			"insert into person (id, firstname, lastname) \n" +
			"values\n" +
			"(1, 'Arthur', 'Dent');" +
			"insert into person (id, firstname, lastname) \n" +
			"values\n" +
			"(2, 'Ford', 'Prefect');" +
			"insert into person (id, firstname, lastname) \n" +
			"values\n" +
			"(3, 'Tricia', 'McMillan');\n" +
			"insert into person (id, firstname, lastname) \n" +
			"values\n" +
			"(4, 'Zaphod', 'Beeblebrox');\n" +
			"insert into spaceship (id, name)  \n" +
			"values\n" +
			"(1, 'Heart of Gold');\n" +
			"insert into spaceship (id, name)  \n" +
			"values\n" +
			"(2, 'Starship Titanic');\n" +
			"insert into owns_ship (person_id, ship_id)\n" +
			"values \n" +
			"(4, 1);\n" +
			"commit;";
		TestUtil.executeScript(con1, insert1);
		String insert2 =
			"insert into person (id, firstname, lastname) \n" +
			"values\n" +
			"(1, 'Arthur', 'Dent');" +

			"insert into person (id, firstname, lastname) \n" +
			"values\n" +
			"(4, 'Zaphod', 'Beeblebrox');\n" +

			"insert into spaceship (id, name)  \n" +
			"values\n" +
			"(1, 'Heart of Gold');\n" +

			"insert into spaceship (id, name)  \n" +
			"values\n" +
			"(2, 'Starship Titanic');\n" +
			"commit;";
		TestUtil.executeScript(con2, insert2);
	}

	@Test
	public void testDiff()
		throws Exception
	{
		WbConnection reference = OracleTestUtil.getOracleConnection();
		WbConnection target = OracleTestUtil.getOracleConnection2();
		assertNotNull(reference);
		assertNotNull(target);

		try
		{
			createTables(reference, target);
			WbDataDiff diff = new WbDataDiff();
			TestUtil util = getTestUtil();
			WbFile outFile1 = util.getFile("diff1.sql");

			String sql =
				"WbDataDiff -referenceProfile='" + reference.getProfile().getName() + "' "+
				"           -targetProfile='" + target.getProfile().getName() + "' " +
				"           -file='" + outFile1.getAbsolutePath() + "' " +
				"           -singleFile=true";

			StatementRunnerResult result = diff.execute(sql);
			String msg = result.getMessageBuffer().toString();
//			System.out.println(msg);
			assertTrue(msg, result.isSuccess());
			assertTrue(msg, outFile1.exists());

			WbFile outFile2 = util.getFile("diff2.sql");
			sql =
				"WbDataDiff -referenceProfile='" + reference.getProfile().getName() + "' "+
				"           -targetProfile='" + target.getProfile().getName() + "' " +
				"           -file='" + outFile2.getAbsolutePath() + "' " +
				"           -referenceSchema=" + reference.getCurrentUser()  +
				"           -targetSchema=" + target.getCurrentUser() +
				"           -singleFile=true";

			result = diff.execute(sql);
			msg = result.getMessageBuffer().toString();
//			System.out.println(msg);
			assertTrue(msg, result.isSuccess());
			assertTrue(msg, outFile2.exists());

			String sql1 = FileUtil.readFile(outFile1, null);

			// Remove the timestamp from the script to allow comparison
			Pattern p = Pattern.compile("^-- Generated by SQL Workbench/J.*$", Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
			Matcher m = p.matcher(sql1);
			sql1 = m.replaceFirst("");

			String sql2 = FileUtil.readFile(outFile2, null);
			m = p.matcher(sql2);
			sql2 = m.replaceFirst("");

			assertEquals(sql1.trim(), sql2.trim());
			assertTrue("Could not delete first file", outFile1.delete());
			assertTrue("Could not delete second file", outFile2.delete());
		}
		finally
		{
			OracleTestUtil.dropAllObjects(reference);
			OracleTestUtil.dropAllObjects(target);
		}
	}


}
