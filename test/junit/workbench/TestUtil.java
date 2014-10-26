/*
 * TestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.ErrorInformationReader;
import workbench.db.ReaderFactory;
import workbench.db.WbConnection;

import workbench.sql.BatchRunner;
import workbench.sql.DelimiterDefinition;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;
import workbench.sql.StatementRunner;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.ArgumentParser;
import workbench.util.DdlObjectInfo;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 *
 * @author Thomas Kellerer
 */
public class TestUtil
{
	private boolean oldDel;
	private boolean oldIns;
	private boolean oldUpd;

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

	public final void prepareEnvironment(boolean noTemplates)
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

		try (PrintWriter pw = new PrintWriter(new FileWriter(new File(dir, "workbench.settings"))))
		{
			pw.println("workbench.log.console=false");
			pw.println("workbench.log.format={timestamp} {type} {source} {message} {error}");
			pw.println("workbench.log.level=DEBUG");
			pw.println("workbench.log.maxfilesize=150000");
			pw.println("workbench.gui.language=en");
			pw.println("workbench.gui.autoconnect=false");
			pw.println("workbench.gui.updatecheck.interval=0");
			pw.println("workbench.db.previewsql=false");
		}
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
		for (File file : files)
		{
			if (file.isDirectory())
			{
				deleteFiles(file);
			}
			if (file.getName().equals("workbench.settings")) continue;
			if (file.getName().equals("workbench.log"))	continue;

			if (!file.delete())
			{
				System.out.println("Could not delete file: " + file.getAbsolutePath());
			}
		}
	}

	/**
	 * Return a connection to an HSQL memory Database with the given name
	 */
	public WbConnection getHSQLConnection(String dbName)
		throws SQLException, ClassNotFoundException
	{
		return getHSQLConnection(dbName, "");
	}

	public WbConnection getHSQLConnection(String dbName, String urlParameters)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = new AppArguments();
		parser.parse("-url='jdbc:hsqldb:mem:" + dbName + urlParameters + ";shutdown=true' -username=sa -driver=org.hsqldb.jdbcDriver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		prof.setName(dbName);
		ConnectionMgr.getInstance().addProfile(prof);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, dbName);
		dropAll(con);
		return con;
	}

	public WbConnection getHSQLConnection(File db, String dbName)
		throws SQLException, ClassNotFoundException
	{
		ArgumentParser parser = new AppArguments();
		parser.parse("-url='jdbc:hsqldb:" + db.getAbsolutePath() + ";shutdown=true' -username=sa -driver=org.hsqldb.jdbcDriver");
		ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
		prof.setName(dbName);
		ConnectionMgr.getInstance().addProfile(prof);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, dbName);
		dropAll(con);
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
		prof.setStorePassword(true);
		ConnectionMgr.getInstance().addProfile(prof);
		WbConnection con = ConnectionMgr.getInstance().getConnection(prof, db);
		dropAll(con);
		return con;
	}

	public void dropAll(WbConnection con)
	{
		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			if (con.getMetadata().isH2())
			{
				stmt.executeUpdate("DROP ALL OBJECTS");
			}
			else if (con.getMetadata().isHsql())
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

	public void restoreSqlFormatting()
	{
		Settings.getInstance().setDoFormatDeletes(oldDel);
		Settings.getInstance().setDoFormatInserts(oldIns);
		Settings.getInstance().setDoFormatUpdates(oldUpd);
	}

	public void disableSqlFormatting()
	{
		oldDel = Settings.getInstance().getDoFormatDeletes();
		oldIns = Settings.getInstance().getDoFormatInserts();
		oldUpd = Settings.getInstance().getDoFormatUpdates();

		Settings.getInstance().setDoFormatDeletes(false);
		Settings.getInstance().setDoFormatInserts(false);
		Settings.getInstance().setDoFormatUpdates(false);

	}

	public WbFile getFile(String filename)
	{
		return new WbFile(basedir, filename);
	}

	public String getBaseDir()
	{
		return this.basedir;
	}

	public File copyResourceFile(Object test, String filename)
		throws IOException
	{
		return copyResourceFile(test.getClass(), filename);
	}

	public File copyResourceFile(Class clz, String filename)
		throws IOException
	{
		InputStream in = clz.getResourceAsStream(filename);
		File target = new File(basedir, filename);
		OutputStream out = new FileOutputStream(target);
		FileUtil.copy(in, out);
		return target;
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
			// The build.xml will include the Xerces runtime because of the Simple ODF library
			// for some reason using the Xerces DocumentBuilderFactory does not work here
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
		try (FileWriter w = new FileWriter(f))
		{
			w.write(content);
		}
	}

	public static void writeFile(File f, String content, String encoding)
		throws IOException
	{
		try (Writer w = EncodingUtil.createWriter(f, encoding, false))
		{
			w.write(content);
		}
	}

	public static void executeScript(WbConnection con, InputStream in)
		throws SQLException, IOException
	{
		Reader r = new InputStreamReader(in);
		String script = FileUtil.readCharacters(r);
		executeScript(con, script, null, false);
	}

	public static void executeScript(WbConnection con, String script)
		throws SQLException
	{
		executeScript(con, script, null, false);
	}

	public static void executeScript(WbConnection con, String script, boolean printError)
		throws SQLException
	{
		executeScript(con, script, null, printError);
	}

	public static void executeScript(WbConnection con, String script, DelimiterDefinition alternateDelimiter)
		throws SQLException
	{
		executeScript(con, script, alternateDelimiter, true);
	}

	public static void executeScript(WbConnection con, String script, DelimiterDefinition alternateDelimiter, boolean printError)
		throws SQLException
	{
		if (con == null) return;

		ScriptParser parser = new ScriptParser(script, ParserType.getTypeFromConnection(con));
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
				DdlObjectInfo info = SqlUtil.getDDLObjectInfo(sql);
				if (printError && info != null)
				{
					ErrorInformationReader reader = ReaderFactory.getErrorInformationReader(con);
					String msg = null;
					if (reader != null)
					{
						reader.getErrorInfo(null, info.getObjectName(), info.getObjectType(), true);
					}
					if (StringUtil.isNonBlank(msg))
					{
						System.out.println("**** Error executing statement:\n" + msg + "\n------------------");
					}
				}
			}
			catch (SQLException e)
			{
				if (printError)
				{
					System.out.println("**** Error executing statement at index= " + i + ", sql=" + sql + ", error: " + e.getMessage());
					e.printStackTrace();
				}
				throw e;
			}
			finally
			{
				SqlUtil.closeStatement(stmt);
			}
		}
	}

	public static int getNumberValue(WbConnection conn, String query)
	{
		Number result = (Number)getSingleQueryValue(conn, query);
		if (result == null) return Integer.MIN_VALUE;
		return result.intValue();
	}

	public static Object getSingleQueryValue(WbConnection conn, String query)
	{
		Statement stmt = null;
		ResultSet rs = null;
		Object result = null;
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next())
			{
				result = rs.getObject(1);
			}
		}
		catch (Exception e)
		{
			result = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
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
		try (PrintWriter writer = new PrintWriter(new FileOutputStream(new File(getBaseDir(), "WbProfiles.xml"))))
		{
			writer.println(xml);
		}
		// Make sure the new profiles are read
		ConnectionMgr.getInstance().reloadProfiles();
	}

	/**
	 * If the given SQL command is a CREATE TABLE command, return
	 * the table that is created, otherwise return null;
	 */
	public static String getCreateTable(CharSequence sql)
	{
		try
		{
			SQLLexer lexer = SQLLexerFactory.createLexer(sql);
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

	public static Map<String, String> getNameSpaces(String xml, String rootTag)
	{
		Map<String, String> result = new HashMap<>();
		// find first tag
		int start = xml.indexOf("<" + rootTag);
		int end = xml.indexOf('>', start);
		int firstSpace = xml.indexOf(' ', start);
		String nstext = xml.substring(firstSpace, end);

		String[] elements = nstext.split(" ");
		for (String element : elements)
		{
			if (element == null ||element.isEmpty()) continue;
			if (!element.startsWith("xmlns:")) continue;

			int colon = element.indexOf(':');
			int equal = element.indexOf('=');
			String name = element.substring(colon + 1, equal);
			String value = element.substring(equal + 1);
			result.put(name, StringUtil.trimQuotes(value));
		}
		return result;
	}

	public static String cleanupSql(CharSequence sql)
	{
		if (StringUtil.isBlank(sql)) return "";
			SQLLexer lexer = SQLLexerFactory.createLexer(sql);
		StringBuilder result = new StringBuilder(sql.length());
		SQLToken last = null;
		SQLToken t = lexer.getNextToken(false, true);
		while (t != null)
		{
			if (t.isWhiteSpace())
			{
				if (last != null && !last.isWhiteSpace())
				{
					result.append(' ');
				}
			}
			else
			{
				result.append(t.getText());
			}
			last = t;
			t = lexer.getNextToken(false, true);
		}
		return result.toString().trim();
	}

	public static void dump(String value)
	{
		int size = value.length();
		for (int i = 0; i < size; i++)
		{
			int c = value.charAt(i);
			String s = Integer.toHexString(c);
			if (s.length() == 1) System.out.print("0");
			System.out.print(s);
			System.out.print(" ");
		}
		System.out.println("");
	}
}
