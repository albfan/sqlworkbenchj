/*
 * OracleSchemaDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.io.File;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbDataDiff;

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
		if (reference == null || target == null)
		{
			return;
		}

		try
		{
			createTables(reference, target);
			WbDataDiff diff = new WbDataDiff();
			String dir = getTestUtil().getBaseDir();
			File outFile = new File(dir, "diff.sql");

			String sql =
				"WbDataDiff -referenceProfile='" + reference.getProfile().getName() + "' "+
				"           -targetProfile='" + target.getProfile().getName() + "' " +
				"           -file='" + outFile.getAbsolutePath() + "' " +
				"           -referenceSchema=" + reference.getCurrentUser()  +
				"           -targetSchema=" + target.getCurrentUser() +
				"           -singleFile=true";

			StatementRunnerResult result = diff.execute(sql);
			String msg = result.getMessageBuffer().toString();
//			System.out.println(msg);
			assertTrue(result.isSuccess());
			assertTrue(msg, outFile.exists());
			assertTrue(outFile.delete());
		}
		finally
		{
			OracleTestUtil.dropAllObjects(reference);
			OracleTestUtil.dropAllObjects(target);
		}
	}


}
