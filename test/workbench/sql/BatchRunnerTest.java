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
	private File[] scriptFiles;
	private BatchRunner runner;
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
	
	private void createScript()
	throws Exception
	{
		scriptFiles = new File[1];
		scriptFiles[0] = new File(util.getBaseDir(), "preparedata.sql");
		PrintWriter writer = new PrintWriter(new FileWriter(scriptFiles[0]));
		System.out.println("Writing script file=" + scriptFiles[0].getAbsolutePath());
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
	}
	
	private void createRunner()
	throws Exception
	{
		util.emptyBaseDirectory();
		createScript();
		ArgumentParser parser = WbManager.createArgumentParser();
		StringBuffer files = new StringBuffer(scriptFiles.length * 50);
		for (int i = 0; i < scriptFiles.length; i++)
		{
			if (i > 0) files.append(' ');
			files.append("-script='");
			files.append(scriptFiles[i].getAbsolutePath());
			files.append('\'');
		}
		parser.parse("-url=jdbc:hsqldb:" + util.getDbName() + " -user=sa -driver=org.hsqldb.jdbcDriver "  + files.toString() + " -rollbackOnDisconnect=true");
		this.runner = BatchRunner.createBatchRunner(parser);
		assertNotNull(this.runner);
	}
	
	public void testBatchRunner()
	{
		try
		{
			createRunner();
			assertNotNull(this.runner);
			
			this.runner.connect();
			WbConnection con = runner.getConnection();
			assertNotNull(con);
			assertNotNull(con.getProfile());
			
			boolean rollback = con.getProfile().getRollbackBeforeDisconnect();
			assertEquals("Rollback property not read from commandline", true, rollback);
			
			this.runner.execute();
			
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
			fail("Error running scripts");
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	
	
}
