/*
 * BatchRunnerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import junit.framework.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.util.ArgumentParser;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class BatchRunnerTest
	extends TestCase
{
	private TestUtil util;
	
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

	public void testEmptyStatement()
	{
		try
		{
			String sql = "-- comment only";
			util.emptyBaseDirectory();
			
			File scriptFile = new File(util.getBaseDir(), "testbatch.sql");
			FileWriter writer = new FileWriter(scriptFile);
			writer.write(sql);
			writer.close();

			ArgumentParser parser = WbManager.createArgumentParser();
			String script = "-script='" + scriptFile.getAbsolutePath() + "'";
			parser.parse("-url='jdbc:hsqldb:mem:testEmptyStmt;shutdown=true' -user=sa -driver=org.hsqldb.jdbcDriver "  + script  + " -displayresult=true -ignoredroperrors=true -showprogress=true -showtiming=false");
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
	
			assertNotNull(runner);
			
			runner.connect();
			WbConnection con = runner.getConnection();
			assertNotNull(con);
			assertNotNull(con.getProfile());
			
			runner.execute();
			assertEquals(true, runner.isSuccess());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testBatchRunner()
	{
		try
		{
			util.emptyBaseDirectory();
			
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

			ArgumentParser parser = WbManager.createArgumentParser();
			String script = "-script='" + scriptFile.getFullPath() + "'";
			parser.parse("-url='jdbc:hsqldb:mem:testBatchRunner;shutdown=true' -user=sa -driver=org.hsqldb.jdbcDriver "  + script + " -rollbackOnDisconnect=true");
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
		catch (Throwable e)
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
			
			ArgumentParser parser = WbManager.createArgumentParser();
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
			
			parser.parse("-url='jdbc:hsqldb:mem:testAltDelimiter;shutdown=true' -altdelimiter='/;nl' -user=sa -driver=org.hsqldb.jdbcDriver -script='" + scriptFile.getAbsolutePath() + "','" + scriptFile2.getAbsolutePath() + "'");
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
			stmt.executeUpdate("CREATE TABLE person (nr integer, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("INSERT INTO person (nr, firstname, lastname) values (1, 'Arthur', 'Dent')");
			stmt.executeUpdate("INSERT INTO person (nr, firstname, lastname) values (2, 'Ford', 'Prefect')");
			con.commit();
			
			File scriptFile = new File(util.getBaseDir(), "runselect.sql");
			PrintWriter writer = new PrintWriter(new FileWriter(scriptFile));
			writer.println("select * from person;");
			writer.close();			

			ArgumentParser parser = WbManager.createArgumentParser();
			parser.parse("-displayresult=true -altdelimiter='/;nl' -script=" + scriptFile.getAbsolutePath());
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			runner.setConnection(con);

			File out= new File(util.getBaseDir(), "console.txt");
			console = new PrintStream(out);
			runner.setConsole(console);
			runner.execute();
			console.close();
			
			BufferedReader in = new BufferedReader(new FileReader(out));
			String content = FileUtil.readCharacters(in);
			in.close();

			int pos = content.indexOf("NR\tFIRSTNAME\tLASTNAME");
			assertEquals("Header not found", (pos > -1), true);
			
			pos = content.indexOf("1\tArthur\tDent");
			assertEquals("Record not found", (pos > -1), true);
			
			pos = content.indexOf("2\tFord\tPrefect");
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
