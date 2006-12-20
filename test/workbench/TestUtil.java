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
import java.sql.SQLException;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
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
	private String testName;
	
	public TestUtil(String name)
	{
		try
		{
			testName = name;
			prepareEnvironment();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void prepareEnvironment()
		throws IOException
	{
		prepareBaseDir();
		WbManager.getInstance().prepareForTest(basedir);
	}
	
	public void prepareBaseDir()
		throws IOException
	{
		File tempdir = new File(System.getProperty("java.io.tmpdir"));
		File dir = new File(tempdir, "wbtest");
		dir.mkdir();
		basedir = dir.getAbsolutePath();

		PrintWriter pw = new PrintWriter(new FileWriter(new File(dir, "workbench.settings")));
		pw.println("workbench.log.console=false");
		pw.println("workbench.log.format={type} {timestamp} {source} {message} {error} {stacktrace}");
		pw.println("workbench.log.level=DEBUG");
		pw.println("workbench.log.maxfilesize=150000");
		pw.close();
		emptyBaseDirectory();
	}
	
	public void emptyBaseDirectory()
	{
		// Cleanup old database files
		File dir = new File(basedir);
		deleteFiles(dir);
	}
	
	private void deleteFiles(File dir)
	{
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++)
		{ 
			if (files[i].isDirectory())
			{
				deleteFiles(files[i]);
			}
			if (files[i].getName().equals("workbench.settings")) continue;
			files[i].delete();
		}
	}
	
	public WbConnection getConnection()
		throws SQLException, ClassNotFoundException
	{
		return getConnection(this.testName);
	}
	
	public WbConnection getConnection(String db)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = WbManager.createArgumentParser();
		parser.parse("-url='jdbc:hsqldb:mem:" + db + ";shutdown=true' -user=sa -driver=org.hsqldb.jdbcDriver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, "WbUnitTest");
		return con;
	}

	public WbConnection getConnection(File db)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = WbManager.createArgumentParser();
		parser.parse("-url='jdbc:hsqldb:" + db.getAbsolutePath() + ";shutdown=true' -user=sa -driver=org.hsqldb.jdbcDriver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, "WbUnitTest");
		return con;
	}
	
	public DefaultStatementRunner createConnectedStatementRunner()
		throws Exception
	{
		return createConnectedStatementRunner(getConnection());
	}
	
	public DefaultStatementRunner createConnectedStatementRunner(WbConnection con)
		throws Exception
	{
		DefaultStatementRunner runner = new DefaultStatementRunner();
		runner.setBaseDir(getBaseDir());
		runner.setConnection(con);
		return runner;
	}

	public String getBaseDir() { return this.basedir; }

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
