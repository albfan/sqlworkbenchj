/*
 * BatchRunnerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.IOException;
import java.io.PrintWriter;
import junit.framework.*;
import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class BatchRunnerTest
	extends TestCase
{
	private String basedir;
	private String dbName;
	private TestUtil util;
	
	public BatchRunnerTest(String testName)
	{
		super(testName);
		try
		{
			util = new TestUtil();
			util.prepareEnvironment();
		}
		catch (IOException ex)
		{
			fail(ex.getMessage());
		}
	}
	
	public void testBatchRunner()
	{
		try
		{
			util.emptyBaseDirectory();
			
			File scriptFile = new File(util.getBaseDir(), "preparedata.sql");
			PrintWriter writer = new PrintWriter(new FileWriter(scriptFile));
			writer.println("-- test script");
			writer.println("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));");
			writer.println("-- first row");
			writer.println("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');");
			writer.println("-- first row");
			writer.println("insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');");
			writer.println("-- first row");
			writer.println("insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox');");
			writer.println("-- make everything permanent");
			writer.println("commit;");
			writer.close();

			ArgumentParser parser = WbManager.createArgumentParser();
			String script = "-script='" + scriptFile.getAbsolutePath() + "'";
			parser.parse("-url='jdbc:hsqldb:" + util.getDbName() + ";shutdown=true' -user=sa -driver=org.hsqldb.jdbcDriver "  + script + " -rollbackOnDisconnect=true");
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
			parser.parse("-url='jdbc:hsqldb:" + util.getDbName() + ";shutdown=true' -altdelimiter='/;nl' -user=sa -driver=org.hsqldb.jdbcDriver -script='" + scriptFile.getAbsolutePath() + "'");
			BatchRunner runner = BatchRunner.createBatchRunner(parser);
			
			assertNotNull(runner);

			runner.connect();
			WbConnection con = runner.getConnection();
			assertNotNull(con);
			assertNotNull(con.getProfile());
			
			DelimiterDefinition def = con.getProfile().getAlternateDelimiter();
			assertNotNull("No alternate delimiter defined", def);
			assertEquals("Wrong delimiter parsed", "/", def.getDelimiter());
			assertEquals("Wrong singleLine Property parsed", true, def.isSingleLine());
			
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
}
