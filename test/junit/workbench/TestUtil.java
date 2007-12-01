/*
 * TestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import java.io.StringReader;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.sql.BatchRunner;
import workbench.sql.DefaultStatementRunner;
import workbench.sql.ScriptParser;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;

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
		this(name, true);
	}
	
	public TestUtil(String name, boolean noTemplates)
	{
		try
		{
			testName = name;
			prepareEnvironment(noTemplates);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void prepareEnvironment()
		throws IOException
	{
		prepareEnvironment(true);
	}
	
	public void prepareEnvironment(boolean noTemplates)
		throws IOException
	{
		prepareBaseDir();
		WbManager.prepareForTest(getArgs(noTemplates));
	}
	
	public String[] getArgs(boolean noTemplates)
	{
		
		String args[] = null;
		if (noTemplates)
		{
			args = new String[] { "-notemplates -nosettings -configdir=" + basedir };
		}
		else
		{
			args = new String[] { "-nosettings -configdir=" + basedir };
		}
		return args;
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
		pw.println("workbench.gui.language=en");
		pw.println("workbench.gui.autoconnect=false");
		pw.println("workbench.gui.updatecheck.interval=0");
		pw.println("workbench.db.previewsql=false");
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
			if (files[i].getName().equals("workbench.log")) continue;
			
			if (!files[i].delete())
			{
				System.out.println("Could not delete file: " + files[i].getAbsolutePath());
			}
		}
	}
	
	public WbConnection getHSQLConnection(String dbName)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = WbManager.createArgumentParser();
		parser.parse("-url='jdbc:hsqldb:mem:" + dbName + ";shutdown=true' -user=sa -driver=org.hsqldb.jdbcDriver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		prof.setName(dbName);
		ConnectionMgr.getInstance().addProfile(prof);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, dbName);
		dropAll(con, false);
		return con;
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
		parser.parse("-url='jdbc:h2:mem:" + db + "' -user=sa -driver=org.h2.Driver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		prof.setName(db);
		ConnectionMgr.getInstance().addProfile(prof);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, db);
		dropAll(con, true);
		return con;
	}

	private void dropAll(WbConnection con, boolean isH2)
	{
		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			if (isH2)
			{
				stmt.executeUpdate("DROP ALL OBJECTS");
			}
			else
			{
				stmt.executeUpdate("DROP SCHEMA PUBLIC CASCADE");
			}
			con.commit();
		}
		catch (Exception e)
		{
			System.out.println("Could not drop all objects");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}
	
	public WbConnection getConnection(File db)
		throws SQLException, ClassNotFoundException
	{
		return getConnection(db, "WbUnitTest");
	}
	
	public WbConnection getConnection(File db, String id)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = WbManager.createArgumentParser();
		parser.parse("-url='jdbc:h2:" + db.getAbsolutePath() + "' -user=sa -driver=org.h2.Driver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, id);
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

	public static List<String> readLines(File f)
		throws IOException
	{
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader in = null;
		int lines = 0;
		try
		{
			in = new BufferedReader(new FileReader(f));
			String s = in.readLine();
			while (s != null)
			{
				result.add(s);
				s = in.readLine();
			}
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return result;
	}
	
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
	
	public static String getXPathValue(String xml, String expression)
	{
		try
		{
			XPath xpath = XPathFactory.newInstance().newXPath();
			InputSource inputSource = new InputSource(new StringReader(xml));
			String value = (String) xpath.evaluate(expression, inputSource, XPathConstants.STRING);		
			return value;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}	
	
	public static void writeFile(File f, String content)
		throws IOException
	{
		FileWriter w = new FileWriter(f);
		w.write(content);
		w.close();
	}
	
	public static void executeScript(WbConnection con, String script)
		throws SQLException
	{
		ScriptParser parser = new ScriptParser(script);
		int count = parser.getSize();
		for (int i=0; i < count; i++)
		{
			String sql = parser.getCommand(i);
			Statement stmt = null;
			try
			{
				stmt = con.createStatement();
				stmt.execute(sql);
			}
			catch (SQLException e)
			{
				System.out.println("**** Error executing statement at index= " + i + ", sql=" + sql);
				throw e;
			}
			finally
			{
				SqlUtil.closeStatement(stmt);
			}
		}
	}

}
