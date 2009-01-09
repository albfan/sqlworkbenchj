/*
 * BatchRunnerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import junit.framework.TestCase;
import workbench.AppArguments;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.db.WbConnection;
import workbench.gui.profiles.ProfileKey;
import workbench.util.ArgumentParser;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.WbFile;

/**
 * @author support@sql-workbench.net
 */
public class BatchRunnerTest
	extends TestCase
{
	private TestUtil	util;
	
	public BatchRunnerTest(String testName)
	{
		super(testName);
		try
		{
			util = new TestUtil(testName);
			util.prepareEnvironment();
		}
		catch (IOException ex)
		{
			fail(ex.getMessage());
		}
	}

	public void testSingleCommand()
		throws Exception
	{
		try
		{
			util.emptyBaseDirectory();
			util.prepareEnvironment();

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
				" -driver=org.h2.Driver " +
				" -rollbackOnDisconnect=true "  +
				" -command='delete from person; commit;' ");

			BatchRunner runner = BatchRunner.createBatchRunner(parser);

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
			
			PrintWriter writer = new PrintWriter(new FileWriter(scriptFile));
			writer.println("-- test script");
			writer.println("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));");
			writer.println("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');");
			writer.println("commit;");
			writer.println("insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');");
			writer.println("insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox');");
			writer.println("-- import data should fail!");
			writer.println("WbImport -file='" + importFile.getName() + "' -type=text -header=true -table=person -continueOnError=false -transactionControl=false");
			writer.close();
			
			ArgumentParser parser = new AppArguments();
			WbFile dbFile = new WbFile(util.getBaseDir(), "errtest");
			parser.parse("-url='jdbc:h2:'" + dbFile.getFullPath() + 
				" -username=sa -driver=org.h2.Driver "  +
				" -logfile='" + logfile.getFullPath() + "' " + 
				" -script='" + scriptFile.getFullPath() + "' " +
				" -abortOnError=true -cleanupError='" + errorFile.getFullPath() + "' " +
				" -autocommit=false " +
				" -cleanupSuccess='" + successFile.getFullPath() + "' "
			);
			
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
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
			if (con != null) con.disconnect();
		}
	}

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
			PrintWriter writer = new PrintWriter(new FileWriter(scriptFile));
			writer.println("-- test script");
			writer.println("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));");
			writer.println("commit;");
			writer.println("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');");
			writer.println("insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');");
			writer.println("insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox');");
			writer.println("WbImport -file='" + importFile.getName() + "' -type=text -header=true -table=person -continueOnError=false -transactionControl=false;");
			writer.println("insert into person (nr, firstname, lastname) values (8,'Tricia', 'McMillian');");
			writer.close();
			
			ArgumentParser parser = new AppArguments();
			WbFile dbFile = new WbFile(util.getBaseDir(), "successtest");
			parser.parse("-url='jdbc:h2:'" + dbFile.getFullPath() + 
				" -username=sa -driver=org.h2.Driver "  +
				" -script='" + scriptFile.getFullPath() + "' " +
				" -abortOnError=true -cleanupError='" + errorFile.getFullPath() + "' " +
				" -cleanupSuccess='" + successFile.getFullPath() + "' " +
				" -autocommit=false " +
				" -rollbackOnDisconnect=true "
				);
			
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
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
			if (con != null) con.disconnect();
		}
	}
	
	public void testCreateCommandLineProfile()
		throws Exception
	{
		AppArguments cmdline = new AppArguments();
		cmdline.parse("-readOnly=true -removeComments=true -emptyStringIsNull=true -autoCommit=true -separateConnection=true -url=jdbc:postgres://localhost/test -username=test -password=topsecret -configdir=. -driver=org.postgresql.Driver -driverjar=postgresql-8.3-603.jdbc3.jar");
		ConnectionProfile p = BatchRunner.createCmdLineProfile(cmdline);
		assertTrue(p.getAutocommit());
		assertTrue(p.getUseSeparateConnectionPerTab());
		assertTrue(p.getRemoveComments());
		assertTrue(p.getEmptyStringIsNull());
		assertTrue(p.isReadOnly());
		assertEquals("org.postgresql.Driver", p.getDriverclass());
		assertEquals("topsecret", p.getPassword());
		assertEquals("test", p.getUsername());
		assertEquals("jdbc:postgres://localhost/test", p.getUrl());
		DbDriver drv = ConnectionMgr.getInstance().findRegisteredDriver("org.postgresql.Driver");
		assertNotNull(drv);
		String dir = cmdline.getValue("configdir");
		assertEquals(".", dir);
	}
	
	public void testEmptyStatement()
		throws Exception
	{
		String sql = "-- comment only";
		util.emptyBaseDirectory();

		File scriptFile = new File(util.getBaseDir(), "testbatch.sql");
		FileWriter writer = new FileWriter(scriptFile);
		writer.write(sql);
		writer.close();

		ArgumentParser parser = new AppArguments();
		String script = "-script='" + scriptFile.getAbsolutePath() + "'";
		parser.parse("-url='jdbc:h2:mem:testEmptyStmt' -username=sa -driver=org.h2.Driver "  + script  + " -displayresult=true -ignoredroperrors=true -showprogress=true -showtiming=false");
		BatchRunner runner = BatchRunner.createBatchRunner(parser);

		assertNotNull(runner);

		runner.connect();
		WbConnection con = runner.getConnection();
		assertNotNull(con);
		assertNotNull(con.getProfile());

		runner.execute();
		assertEquals(true, runner.isSuccess());
	}
	
	public void testBatchRunner()
		throws Exception
	{
		try
		{
			util.emptyBaseDirectory();
			util.prepareEnvironment(true);
			
			WbFile scriptFile = new WbFile(util.getBaseDir(), "preparedata.sql");
			PrintWriter writer = new PrintWriter(new FileWriter(scriptFile));
			writer.println("-- test script");
			writer.println("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));");
			writer.println("-- first row");
			writer.println("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');");
			writer.println("-- first row");
			writer.println("insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');");
			writer.println("-- first row");
			writer.println("insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox');");
			writer.println("/* make everything permanent\nmore comments */");
			writer.println("commit;");
			writer.close();
			
			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:h2:mem:testBatchRunner' " + 
				" -username=sa " +
				" -driver=org.h2.Driver "  + 
				" -script='" + scriptFile.getFullPath() + "' " +
				" -rollbackOnDisconnect=true " +
				" -showProgress=true " + 
				" -displayResult=true ");
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
	
			assertNotNull(runner);
			
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
		
	public void testAltDelimiter()
	{
		try
		{
			util.emptyBaseDirectory();
			
			ArgumentParser parser = new AppArguments();
			File scriptFile = new File(util.getBaseDir(), "preparedata.sql");
			PrintWriter writer = new PrintWriter(new FileWriter(scriptFile));
			writer.println("-- test script");
			writer.println("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100))");
			writer.println("/");
			writer.println("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent')");
			writer.println("/");
			writer.println("insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect')");
			writer.println("/");
			writer.println("insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox')");
			writer.println("/");
			writer.println("commit");
			writer.println("/");
			writer.close();			
			
			File scriptFile2 = new File(util.getBaseDir(), "insert.sql");
			PrintWriter writer2 = new PrintWriter(new FileWriter(scriptFile2));
			writer2.println("-- test script");
			writer2.println("insert into person (nr, firstname, lastname) values (4,'Tricia', 'McMillian');");
			writer2.println("commit;");
			writer2.close();			
			
			parser.parse("-url='jdbc:h2:mem:testAltDelimiter' -altdelimiter='/;nl' -user=sa -driver=org.h2.Driver -script='" + scriptFile.getAbsolutePath() + "','" + scriptFile2.getAbsolutePath() + "'");
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			
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
			assertEquals("Runner not successful!", true, runner.isSuccess());
			
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
			PrintWriter writer = new PrintWriter(new FileWriter(scriptFile));
			writer.println("select * from person;");
			writer.close();			

			ArgumentParser parser = new AppArguments();
			parser.parse("-displayresult=true -altdelimiter='/;nl' -script=" + scriptFile.getAbsolutePath());
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			runner.setConnection(con);

			File out= new File(util.getBaseDir(), "console.txt");
			console = new PrintStream(out);
			runner.setOptimizeColWidths(false);
			runner.setConsole(console);
			runner.execute();
			console.close();
			
			BufferedReader in = new BufferedReader(new FileReader(out));
			String content = FileUtil.readCharacters(in);
			System.out.println("*************\n" + content + "\n*****************");
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
