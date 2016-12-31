/*
 * BatchRunnerTest.java
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
package workbench.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import workbench.AppArguments;
import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.db.WbConnection;

import workbench.gui.profiles.ProfileKey;

import workbench.util.ArgumentParser;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.WbFile;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class BatchRunnerTest
	extends WbTestCase
{
	private TestUtil	util;

	public BatchRunnerTest()
		throws Exception
	{
		super("BatchRunnerTest");
		util = getTestUtil();
	}

	@Test
	public void testSingleCommand()
		throws Exception
	{
		try
		{
			util.emptyBaseDirectory();

			WbConnection con = util.getConnection("testSingleCommand");
			Statement stmt = con.createStatement();

			// prepare database
			TestUtil.executeScript(con,
				"CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));\n" +
				"insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');\n" +
				"insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');\n" +
				"commit;"
				);

			ResultSet rs = stmt.executeQuery("select count(*) from person");
			assertTrue(rs.next());
			int count = rs.getInt(1);
			assertEquals(2, count);
			rs.close();

			// Initialize the batch runner
			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:h2:mem:testSingleCommand' " +
				" -username=sa " +
				" -password='' " +
				" -driver=org.h2.Driver " +
				" -rollbackOnDisconnect=true "  +
				" -command='delete from person; commit;' ");

			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			initRunner4Test(runner);

			assertNotNull(runner);

			runner.connect();
			con = runner.getConnection();
			assertNotNull(con);

			runner.execute();
			assertTrue(runner.isSuccess());

			rs = stmt.executeQuery("select count(*) from person");
			assertTrue(rs.next());
			count = rs.getInt(1);
			assertEquals(0, count);

			SqlUtil.closeAll(rs, stmt);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testTransactionControlError()
		throws Exception
	{
		WbConnection con = null;
		try
		{
			util.emptyBaseDirectory();
			WbFile errorFile = new WbFile(util.getBaseDir(), "error.sql");
			TestUtil.writeFile(errorFile, "rollback;\n");
			WbFile successFile = new WbFile(util.getBaseDir(), "success.sql");
			TestUtil.writeFile(successFile, "commit;\n");

			WbFile importFile = new WbFile(util.getBaseDir(), "data.txt");
			String data = "nr\tfirstname\tlastname\n" +
				"1\tArthur\tDent\n";
			TestUtil.writeFile(importFile, data);
			WbFile scriptFile = new WbFile(util.getBaseDir(), "myscript.sql");
			WbFile logfile = new WbFile(util.getBaseDir(), "junit_transaction_test.txt");

			TestUtil.writeFile(scriptFile,
			"-- test script\n" +
			"CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));\n" +
			"insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');\n" +
			"commit;\n" +
			"insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');\n" +
			"insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox');\n" +
			"-- import data should fail!\n" +
			"WbImport -file='" + importFile.getName() + "' -type=text -header=true -table=person -continueOnError=false -transactionControl=false\n");

			ArgumentParser parser = new AppArguments();
			WbFile dbFile = new WbFile(util.getBaseDir(), "errtest");
			parser.parse("-url=\"jdbc:h2:" + dbFile.getFullPath() + "\"" +
				" -username=sa -driver=org.h2.Driver "  +
				" -logfile='" + logfile.getFullPath() + "' " +
				" -script='" + scriptFile.getFullPath() + "' " +
				" -password='' " +
				" -abortOnError=true -cleanupError='" + errorFile.getFullPath() + "' " +
				" -autocommit=false " +
				" -cleanupSuccess='" + successFile.getFullPath() + "' "
			);

			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			initRunner4Test(runner);
			assertNotNull(runner);

			runner.connect();
			runner.execute();

			con = util.getConnection(dbFile);
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from person");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Not enough rows!", 1, nr);
			}
			else
			{
				fail("No data");
			}
			SqlUtil.closeAll(rs, stmt);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testTransactionControlSuccess()
		throws Exception
	{
		WbConnection con = null;
		try
		{
			util.emptyBaseDirectory();
			WbFile errorFile = new WbFile(util.getBaseDir(), "error.sql");
			TestUtil.writeFile(errorFile, "rollback;\n");
			WbFile successFile = new WbFile(util.getBaseDir(), "success.sql");
			TestUtil.writeFile(successFile, "commit;\n");

			WbFile importFile = new WbFile(util.getBaseDir(), "data_success.txt");
			String data = "nr\tfirstname\tlastname\n" +
				"5\tMary\tMoviestar\n" +
				"6\tMajor\tBug\n" +
				"7\tGeneral\tFailure\n";
			TestUtil.writeFile(importFile, data);
			WbFile scriptFile = new WbFile(util.getBaseDir(), "myscript.sql");
			TestUtil.writeFile(scriptFile,
			"-- test script\n" +
			"CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));\n" +
			"commit;\n" +
			"insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');\n" +
			"insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');\n" +
			"insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox');\n" +
			"WbImport -file='" + importFile.getName() + "' -type=text -header=true -table=person -continueOnError=false -transactionControl=false;\n" +
			"insert into person (nr, firstname, lastname) values (8,'Tricia', 'McMillian');");

			ArgumentParser parser = new AppArguments();
			WbFile dbFile = new WbFile(util.getBaseDir(), "successtest");
			parser.parse("-url=jdbc:h2:" + dbFile.getFullPath() +
				" -username=sa -driver=org.h2.Driver "  +
				" -password='' " +
				" -script='" + scriptFile.getFullPath() + "' " +
				" -abortOnError=true -cleanupError='" + errorFile.getFullPath() + "' " +
				" -cleanupSuccess='" + successFile.getFullPath() + "' " +
				" -autocommit=false " +
				" -rollbackOnDisconnect=true "
				);

			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			initRunner4Test(runner);
			assertNotNull(runner);

			runner.connect();
			assertTrue(runner.isConnected());
			runner.execute();
			assertTrue(runner.isSuccess());

			con = util.getConnection(dbFile);
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from person");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Not enough rows!", 7, nr);
			}
			else
			{
				fail("No data");
			}
			SqlUtil.closeAll(rs, stmt);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testCreateCommandLineProfile()
		throws Exception
	{
		AppArguments cmdline = new AppArguments();
		cmdline.parse("-readOnly=true -removeComments=true -connectionProperties='myprop=42' -emptyStringIsNull=true -autoCommit=true -separateConnection=true -url=jdbc:postgres://localhost/test -username=test -password=topsecret -configdir=. -driver=org.postgresql.Driver -driverjar=postgresql-8.3-603.jdbc3.jar");
		ConnectionProfile p = BatchRunner.createCmdLineProfile(cmdline);
		assertTrue(p.getAutocommit());
		assertTrue(p.getUseSeparateConnectionPerTab());
		assertTrue(p.getRemoveComments());
		assertTrue(p.getEmptyStringIsNull());
		assertTrue(p.isReadOnly());
		Properties props = p.getConnectionProperties();
		assertNotNull(props);
		assertEquals(1, props.size());
		assertEquals("42", props.get("myprop"));
		assertEquals("org.postgresql.Driver", p.getDriverclass());
		assertEquals("topsecret", p.getPassword());
		assertEquals("test", p.getUsername());
		assertEquals("jdbc:postgres://localhost/test", p.getUrl());
		DbDriver drv = ConnectionMgr.getInstance().findRegisteredDriver("org.postgresql.Driver");
		assertNotNull(drv);
		String dir = cmdline.getValue("configdir");
		assertEquals(".", dir);
	}

	@Test
	public void testEmptyStatement()
		throws Exception
	{
		String sql = "-- comment only";
		util.emptyBaseDirectory();

		File scriptFile = new File(util.getBaseDir(), "testbatch.sql");
		TestUtil.writeFile(scriptFile, sql);

		ArgumentParser parser = new AppArguments();
		String script = "-script='" + scriptFile.getAbsolutePath() + "'";
		parser.parse("-url='jdbc:h2:mem:testEmptyStmt' -username=sa -password='' -driver=org.h2.Driver "  + script  + " -displayresult=true -ignoredroperrors=true -showprogress=true -showtiming=false");
		BatchRunner runner = BatchRunner.createBatchRunner(parser);
		initRunner4Test(runner);

		assertNotNull(runner);

		runner.connect();
		WbConnection con = runner.getConnection();
		assertNotNull(con);
		assertNotNull(con.getProfile());

		runner.execute();
		assertEquals(true, runner.isSuccess());
	}

	private void initRunner4Test(BatchRunner runner)
	{
		if (runner == null) return;
		// this is a workaround for a bug in NetBeans 6.7
		// which gets confused when running JUnit tests that
		// output a '\r' character to standard out.
		runner.setVerboseLogging(false);
		runner.setShowProgress(false);
	}

	@Test
	public void testBatchRunner()
		throws Exception
	{
		try
		{
			util.emptyBaseDirectory();
			util.prepareEnvironment(true);

			WbFile scriptFile = new WbFile(util.getBaseDir(), "preparedata.sql");
			TestUtil.writeFile(scriptFile,
			"-- test script\n" +
			"CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));\n" +
			"-- first row\n" +
			"insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');\n" +
			"-- first row\n" +
			"insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');\n" +
			"-- first row\n" +
			"insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox');\n" +
			"/* make everything permanent\nmore comments */\n" +
			"commit;\n" );


			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:h2:mem:testBatchRunner' " +
				" -username=sa " +
				" -password='' " +
				" -driver=org.h2.Driver "  +
				" -script='" + scriptFile.getFullPath() + "' " +
				" -rollbackOnDisconnect=true " +
				" -showProgress=true " +
				" -displayResult=true ");
			BatchRunner runner = BatchRunner.createBatchRunner(parser);

			assertNotNull(runner);
			initRunner4Test(runner);

			runner.connect();
			WbConnection con = runner.getConnection();
			assertNotNull(con);
			assertNotNull(con.getProfile());

			boolean rollback = con.getProfile().getRollbackBeforeDisconnect();
			assertEquals("Rollback property not read from commandline", true, rollback);

			runner.execute();

			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from person");

			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Not enough records inserted", 3, count);
			}
			SqlUtil.closeAll(rs, stmt);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testNoConnection()
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			ConnectionMgr.getInstance().disconnectAll();
			ConnectionMgr.getInstance().clearProfiles();
			WbFile targetDb  = new WbFile(util.getBaseDir(), "brTargetdb");
			WbFile sourceDb  = new WbFile(util.getBaseDir(), "brSourcedb");
			util.createProfiles(sourceDb, targetDb);
			util.prepareSource(sourceDb);
			util.prepareTarget(targetDb);
			WbFile script = new WbFile(util.getBaseDir(), "copydata.sql");
			String command = "WbCopy -sourceProfile='SourceConnection' -targetProfile='TargetConnection' -sourceTable=person -targetTable=person;";
			TestUtil.writeFile(script, command);
			ArgumentParser parser = new AppArguments();

			parser.parse("-script='" + script.getFullPath() + "'");
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			assertNotNull(runner);
			initRunner4Test(runner);
			runner.connect();
			runner.execute();
			assertTrue(runner.isSuccess());
			WbConnection target = ConnectionMgr.getInstance().getConnection(new ProfileKey("TargetConnection"), "CopyCheck");
			stmt = target.createStatement();
			rs = stmt.executeQuery("select count(*) from person");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals(4, count);
			}
			else
			{
				fail("No rows copied");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testAltDelimiter()
	{
		try
		{
			util.emptyBaseDirectory();

			ArgumentParser parser = new AppArguments();
			File scriptFile = new File(util.getBaseDir(), "preparedata.sql");
			TestUtil.writeFile(scriptFile,
			"-- test script\n" +
			"CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100))\n" +
			"/\n" +
			"insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent')\n" +
			"/\n" +
			"insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect')\n" +
			"/\n" +
			"insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox')\n" +
			"/\n" +
			"commit\n" +
			"/");

			File scriptFile2 = new File(util.getBaseDir(), "insert.sql");
			TestUtil.writeFile(scriptFile2,
			"-- test script\n" +
			"insert into person (nr, firstname, lastname) values (4,'Tricia', 'McMillian');\n" +
			"commit;");

			parser.parse("-url='jdbc:h2:mem:testAltDelimiter' -altdelimiter='/;nl' -username=sa -password='' -driver=org.h2.Driver -script='" + scriptFile.getAbsolutePath() + "','" + scriptFile2.getAbsolutePath() + "'");
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			initRunner4Test(runner);

			assertNotNull(runner);

			runner.connect();
			WbConnection con = runner.getConnection();
			assertNotNull(con);
			assertNotNull(con.getProfile());

			DelimiterDefinition def = con.getProfile().getAlternateDelimiter();
			assertNotNull("No alternate delimiter defined", def);
			assertEquals("Wrong alternate delimiter parsed", "/", def.getDelimiter());
			assertEquals("Wrong singleLine Property parsed", true, def.isSingleLine());

			runner.execute();
			assertEquals(true, runner.isSuccess());

			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from person");

			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Not enough records inserted", 4, count);
			}
			SqlUtil.closeAll(rs, stmt);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testConsoleOutput()
	{
		WbConnection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		PrintStream console = null;
		try
		{
			util.prepareEnvironment();
			con = util.getConnection();
			stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE person (nr integer, firstname varchar(20), lastname varchar(20))");
			stmt.executeUpdate("INSERT INTO person (nr, firstname, lastname) values (1, 'Arthur', 'Dent')");
			stmt.executeUpdate("INSERT INTO person (nr, firstname, lastname) values (2, 'Ford', 'Prefect')");
			con.commit();

			File scriptFile = new File(util.getBaseDir(), "runselect.sql");
			TestUtil.writeFile(scriptFile, "select * from person;");

			ArgumentParser parser = new AppArguments();
			parser.parse("-displayresult=true -altdelimiter='/;nl' -script=" + scriptFile.getAbsolutePath());
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			initRunner4Test(runner);
			runner.setConnection(con);

			File out= new File(util.getBaseDir(), "console.txt");
			console = new PrintStream(out);
			runner.setOptimizeColWidths(false);
			runner.setConsole(console);
			runner.execute();
			console.close();

			BufferedReader in = new BufferedReader(new FileReader(out));
			String content = FileUtil.readCharacters(in);
			//System.out.println("*************\n" + content + "\n*****************");
			int pos = content.indexOf("NR | FIRSTNAME | LASTNAME");
			assertEquals("Header not found", (pos > -1), true);

			pos = content.indexOf("1 | Arthur | Dent");
			assertEquals("Record not found", (pos > -1), true);

			pos = content.indexOf("2 | Ford | Prefect");
			assertEquals("Record not found", (pos > -1), true);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
