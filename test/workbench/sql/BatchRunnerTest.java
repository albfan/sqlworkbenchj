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

import java.io.PrintWriter;
import junit.framework.*;
import java.io.File;
import java.io.FileWriter;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.util.ArgumentParser;

/**
 *
 * @author support@sql-workbench.net
 */
public class BatchRunnerTest extends TestCase
{
	private File[] scriptFiles;
	private BatchRunner runner;
	private String basedir;
	private String dbName;
	
	public BatchRunnerTest(String testName)
	{
		super(testName);
		try
		{
			File tempdir = new File(System.getProperty("java.io.tmpdir"));
			File dir = new File(tempdir, "wbtest");
			dir.mkdir();
			basedir = dir.getAbsolutePath();
			File db = new File(basedir, "batchrunnertest");
			dbName = db.getAbsolutePath();
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	private void emptyBaseDirectory()
	{
		// Cleanup old database files
		File dir = new File(basedir);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			files[i].delete();
		}
	}

	private void prepareDatabase()
	{
		
	}
	
	private void createScript()
		throws Exception
	{
		scriptFiles = new File[1];
		scriptFiles[0] = new File(basedir, "preparedata.sql");
		PrintWriter writer = new PrintWriter(new FileWriter(scriptFiles[0]));
		System.out.println("Writing script file=" + scriptFiles[0].getAbsolutePath());
		writer.println("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100));");
		writer.println("insert into person (nr, firstname, lastname) values (1,'Arthur', 'Dent');");
		writer.println("insert into person (nr, firstname, lastname) values (2,'Ford', 'Prefect');");
		writer.println("insert into person (nr, firstname, lastname) values (3,'Zaphod', 'Beeblebrox');");
		writer.println("commit;");
		writer.close();
	}
	
	protected void setUp() throws Exception
	{
		emptyBaseDirectory();
		WbManager.prepareForTest(basedir);
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
		parser.parse("-url=jdbc:hsqldb:" + dbName + " -user=sa -driver=org.hsqldb.jdbcDriver "  + files.toString());
		this.runner = BatchRunner.createBatchRunner(parser);
		assertNotNull(this.runner);
	}

	public void testBatchRunner()
	{
		assertNotNull(this.runner);
		try
		{
			this.runner.connect();
			this.runner.execute();
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
