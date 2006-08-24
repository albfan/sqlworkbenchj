/*
 * TestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.interfaces.StatementRunner;
import workbench.sql.BatchRunner;
import workbench.sql.DefaultStatementRunner;
import workbench.util.ArgumentParser;

/**
 *
 * @author support@sql-workbench.net
 */
public class TestUtil
{
	
	private String basedir;
	private String dbName;
	
	public TestUtil()
	{
	}

	public void prepareEnvironment()
		throws IOException
	{
		prepareEnvironment("wbtestdb");
	}
	
	public void prepareEnvironment(String dbBaseName)
		throws IOException
	{
		File tempdir = new File(System.getProperty("java.io.tmpdir"));
		File dir = new File(tempdir, "wbtest");
		dir.mkdir();
		basedir = dir.getAbsolutePath();
		File db = new File(basedir, dbBaseName);
		dbName = db.getAbsolutePath();

		PrintWriter pw = new PrintWriter(new FileWriter(new File(dir, "workbench.settings")));
		pw.println("workbench.log.console=false");
		pw.println("workbench.log.format={type} {timestamp} {source} {message} {error}");
		pw.println("workbench.log.level=DEBUG");
		pw.println("workbench.log.maxfilesize=150000");
		pw.close();
		WbManager.getInstance().prepareForTest(basedir);
	}

	public void emptyBaseDirectory()
	{
		// Cleanup old database files
		File dir = new File(basedir);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++)
		{ 
			files[i].delete();
		}
	}

	public DefaultStatementRunner createConnectedStatementRunner()
		throws Exception
	{
		emptyBaseDirectory();
		ArgumentParser parser = WbManager.createArgumentParser();
		parser.parse("-url=jdbc:hsqldb:" + getDbName() + " -user=sa -driver=org.hsqldb.jdbcDriver");
		DefaultStatementRunner runner = new DefaultStatementRunner();
		runner.setBaseDir(getBaseDir());
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, "WbUnitTest");
		runner.setConnection(con);
		return runner;
	}

	public String getBaseDir() { return this.basedir; }
	public String getDbName() { return this.dbName; }

	public static int countLines(File f)
		throws IOException
	{
		BufferedReader in = null;
		int lines = 0;
		try
		{
			in = new BufferedReader(new FileReader(f));
			String s = in.readLine();
			while (s != null)
			{
				lines ++;
				s = in.readLine();
			}
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return lines;
	}
	
	
}
