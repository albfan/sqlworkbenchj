/*
 * TestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.sql.BatchRunner;
import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptParser;
import workbench.sql.StatementRunner;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.ArgumentParser;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class TestUtil
{

	private String basedir;
	private String testName;

	/**
	 * Creates a new TestUtil with the given test name
	 * @param name
	 */
	public TestUtil(String name)
	{
		this(name, true);
		System.setProperty("workbench.log.console", "false");
	}

	/**
	 * Creates a new TestUtil with the given test name, but makes sure no
	 * driver templates are loaded by WbManager
	 *
	 * @param name
	 * @param noTemplates
	 * @see workbench.WbManager#prepareForTest(java.lang.String[])
	 */
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
		String cmdline = "-nosettings -configdir='" + basedir + "' ";

		if (noTemplates)
		{
			cmdline +=  " -notemplates";
		}

		return new String[] { cmdline };
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
		pw.println("workbench.log.format={timestamp} {type} {source} {message} {error}");
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

	/**
	 * Return a connection to an HSQL memory Database with the given name
	 */
	public WbConnection getHSQLConnection(String dbName)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = new AppArguments();
		parser.parse("-url='jdbc:hsqldb:mem:" + dbName + ";shutdown=true' -username=sa -driver=org.hsqldb.jdbcDriver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		prof.setName(dbName);
		ConnectionMgr.getInstance().addProfile(prof);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, dbName);
		dropAll(con, false);
		return con;
	}

	/**
	 * Return a connection to an H2 (in-memory) Database with the name of this TestUtil
	 * @see TestUtil#TestUtil(String)
	 */
	public WbConnection getConnection()
		throws SQLException, ClassNotFoundException
	{
		return getConnection(this.testName);
	}

	/**
	 * Return a connection to an H2 (in-memory) Database with the given name
	 * @see TestUtil#TestUtil(String)
	 */
	public WbConnection getConnection(String db)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = new AppArguments();
		parser.parse("-url='jdbc:h2:mem:" + db + "' -username=sa -driver=org.h2.Driver");
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

	/**
	 * Creates a connection to a file-based H2 database with the given file name.
	 * A connection profile for this connection is created with the ID "WbUnitTest".
	 * The name of the profile is the file name (without the file path)
	 */
	public WbConnection getConnection(File db)
		throws SQLException, ClassNotFoundException
	{
		return getConnection(db, "WbUnitTest", false);
	}

	/**
	 * Creates a connection to a file-based H2 database with the given file name.
	 * A connection profile for this connection is created with the given ID.
	 * The name of the profile is the file name (without the file path)
	 */
	public WbConnection getConnection(File db, String id, boolean mvcc)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = new AppArguments();
		String option = (mvcc ? ";MVCC=true" : "");
		parser.parse("-url='jdbc:h2:" + db.getAbsolutePath() + option + "' -username=sa -driver=org.h2.Driver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		prof.setName(db.getName());
		ConnectionMgr.getInstance().addProfile(prof);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, id);
		return con;
	}

	/**
	 * Creates a statement runner with a connection to an H2 memory database.
	 * The basedir of the StatementRunner is set to this basedir
	 *
	 * @see #getConnection()
	 * @see #getBaseDir()
	 * @see StatementRunner#setBaseDir(java.lang.String)
	 */
	public StatementRunner createConnectedStatementRunner()
		throws Exception
	{
		return createConnectedStatementRunner(getConnection());
	}

	/**
	 * Creates a statement runner for the given connection.
	 * The basedir of the StatementRunner is set to this basedir
	 *
	 * @see #getBaseDir()
	 * @see StatementRunner#setBaseDir(java.lang.String)
	 */
	public StatementRunner createConnectedStatementRunner(WbConnection con)
		throws Exception
	{
		StatementRunner runner = new StatementRunner();
		runner.setBaseDir(getBaseDir());
		runner.setConnection(con);
		return runner;
	}

	public String getBaseDir()
	{
		return this.basedir;
	}

	public void copyResourceFile(Object test, String filename)
		throws IOException
	{
		InputStream in = test.getClass().getResourceAsStream(filename);
		File target = new File(basedir, filename);
		OutputStream out = new FileOutputStream(target);
		FileUtil.copy(in, out);
	}

	public static List<String> getLines(String s)
		throws IOException
	{
		return readLines(new StringReader(s));
	}

	public static List<String> readLines(File f)
		throws IOException
	{
		return readLines(new FileReader(f));
	}

	public static List<String> readLines(Reader source)
		throws IOException
	{
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader in = null;

		try
		{
			in = new BufferedReader(source);
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
		return getXPathValue(xml, expression, null);
	}

	public static String getXPathValue(String xml, String expression, Map<String, String> namespaceMapping)
	{
		try
		{
			DocumentBuilderFactory xmlFact = DocumentBuilderFactory.newInstance();
      xmlFact.setNamespaceAware(true);
      DocumentBuilder builder = xmlFact.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(xml));
      Document doc = builder.parse(inputSource);
			XPath xpath = XPathFactory.newInstance().newXPath();
			if (namespaceMapping != null)
			{
			  xpath.setNamespaceContext(new SimpleNamespaceContext(namespaceMapping));
			}
			String value = (String) xpath.evaluate(expression, doc, XPathConstants.STRING);
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

	public static void writeFile(File f, String content, String encoding)
		throws IOException
	{
		Writer w = EncodingUtil.createWriter(f, encoding, false);
		w.write(content);
		w.close();
	}

	public static void executeScript(WbConnection con, String script)
		throws SQLException
	{
		executeScript(con, script, null);
	}

	public static void executeScript(WbConnection con, String script, DelimiterDefinition alternateDelimiter)
		throws SQLException
	{
		if (con == null) return;

		ScriptParser parser = new ScriptParser(script);
		if (alternateDelimiter != null)
		{
			parser.setAlternateDelimiter(alternateDelimiter);
		}
		
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

	public void prepareSource(WbFile sourceDb)
		throws SQLException, ClassNotFoundException
	{
		Connection con = null;
		Statement stmt = null;

		try
		{
			Class.forName("org.h2.Driver");
			con = DriverManager.getConnection("jdbc:h2:" + sourceDb.getFullPath(), "sa", "");
			stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE person (id integer primary key, firstname varchar(50), lastname varchar(50))");
			stmt.executeUpdate("insert into person (id, firstname, lastname) values (1, 'Harry', 'Handsome')");
			stmt.executeUpdate("insert into person (id, firstname, lastname) values (2, 'Mary', 'Moviestar')");
			stmt.executeUpdate("insert into person (id, firstname, lastname) values (3, 'Major', 'Bug')");
			stmt.executeUpdate("insert into person (id, firstname, lastname) values (4, 'General', 'Failure')");
			con.commit();
			stmt.close();
			con.close();
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
			try { con.close(); } catch (Throwable th) {}
		}
	}

	public void prepareTarget(WbFile targetDb)
		throws SQLException, ClassNotFoundException
	{
		Connection con = DriverManager.getConnection("jdbc:h2:" + targetDb.getFullPath(), "sa", "");
		Statement stmt = null;
		try
		{
			Class.forName("org.h2.Driver");
			stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE person (id integer primary key, firstname varchar(50), lastname varchar(50))");
			con.commit();
			stmt.close();
			con.close();
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
			try { con.close(); } catch (Throwable th) {}
		}
	}

	public void createProfiles(WbFile sourceDb, WbFile targetDb)
		throws FileNotFoundException
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>  \n" +
             "<java version=\"1.5.0_08\" class=\"java.beans.XMLDecoder\">  \n" +
             "	 \n" +
             " <object class=\"java.util.ArrayList\">  \n" +
             "  <void method=\"add\">  \n" +
             "   <object class=\"workbench.db.ConnectionProfile\">  \n" +
             "    <void property=\"driverclass\">  \n" +
             "     <string>org.h2.Driver</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"name\">  \n" +
             "     <string>SourceConnection</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"url\">  \n" +
             "     <string>" + "jdbc:h2:" + StringUtil.replace(sourceDb.getFullPath(), "\\", "/") + "</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"username\">  \n" +
             "     <string>sa</string>  \n" +
             "    </void>  \n" +
             "   </object>  \n" +
             "  </void>  \n" +
             "	 \n" +
             "  <void method=\"add\">  \n" +
             "   <object class=\"workbench.db.ConnectionProfile\">  \n" +
             "    <void property=\"driverclass\">  \n" +
             "     <string>org.h2.Driver</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"name\">  \n" +
             "     <string>TargetConnection</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"url\">  \n" +
             "     <string>" + "jdbc:h2:" + StringUtil.replace(targetDb.getFullPath(), "\\", "/") + "</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"username\">  \n" +
             "     <string>sa</string>  \n" +
             "    </void>  \n" +
             "   </object>  \n" +
             "  </void>  \n" +
             "	 \n" +
             " </object>  \n" +
             "</java> ";
		PrintWriter writer = new PrintWriter(new FileOutputStream(new File(getBaseDir(), "WbProfiles.xml")));
		writer.println(xml);
		writer.close();
		// Make sure the new profiles are read
		ConnectionMgr.getInstance().readProfiles();
	}

	/**
	 * If the given SQL command is a CREATE TABLE command, return
	 * the table that is created, otherwise return null;
	 */
	public static String getCreateTable(CharSequence sql)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken t = lexer.getNextToken(false, false);
			if (t == null || !t.getContents().equals("CREATE")) return null;
			t = lexer.getNextToken(false, false);
			if (t == null || !t.getContents().equals("TABLE")) return null;
			t = lexer.getNextToken(false, false);
			if (t == null) return null;
			return t.getContents();
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
