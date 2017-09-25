/*
 * TestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Ignore;
import workbench.console.DataStorePrinter;
import workbench.resource.Settings;
import workbench.ssh.SshException;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.ErrorInformationReader;
import workbench.db.ReaderFactory;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.BatchRunner;
import workbench.sql.DelimiterDefinition;
import workbench.sql.StatementRunner;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;
import workbench.sql.wbcommands.InvalidConnectionDescriptor;

import workbench.util.ArgumentParser;
import workbench.util.CollectionUtil;
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
@Ignore
public class TestUtil
{
  private boolean oldDel;
  private boolean oldIns;
  private boolean oldUpd;

  private String basedir;
  private String testName;

  /**
   * Creates a new TestUtil with the given test name
   *
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
   *
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
    List<String> cmdline = CollectionUtil.arrayList("-nosettings", "-configdir='" + basedir + "'", "-Dworkbench.log.console=false");

    if (noTemplates)
    {
      cmdline.add("-notemplates");
    }

    return cmdline.toArray(new String[]
    {
    });
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
      if (file.getName().equals("workbench.log")) continue;
      if (file.getName().equals("derby.log")) continue;

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
    throws SQLException, ClassNotFoundException, SshException, InvalidConnectionDescriptor
  {
    return getHSQLConnection(dbName, "");
  }

  public WbConnection getHSQLConnection(String dbName, String urlParameters)
    throws SQLException, ClassNotFoundException, SshException, InvalidConnectionDescriptor
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
    throws SQLException, ClassNotFoundException, SshException, InvalidConnectionDescriptor
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
   *
   * @see TestUtil#TestUtil(String)
   */
  public WbConnection getConnection()
    throws SQLException, ClassNotFoundException, SshException, InvalidConnectionDescriptor
  {
    return getConnection(this.testName);
  }

  /**
   * Return a connection to an H2 (in-memory) Database with the given name
   *
   * @see TestUtil#TestUtil(String)
   */
  public WbConnection getConnection(String db)
    throws SQLException, ClassNotFoundException, SshException, InvalidConnectionDescriptor
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
    throws SQLException, ClassNotFoundException, SshException, InvalidConnectionDescriptor
  {
    return getConnection(db, "WbUnitTest", false);
  }

  /**
   * Creates a connection to a file-based H2 database with the given file name.
   * A connection profile for this connection is created with the given ID.
   * The name of the profile is the file name (without the file path)
   */
  public WbConnection getConnection(File db, String id, boolean mvcc)
    throws SQLException, ClassNotFoundException, SshException, InvalidConnectionDescriptor
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
        lines++;
        s = in.readLine();
      }
    }
    finally
    {
      try
      {
        in.close();
      }
      catch (Throwable th)
      {
      }
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
      String value = (String)xpath.evaluate(expression, doc, XPathConstants.STRING);
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
    for (int i = 0; i < count; i++)
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
            reader.getErrorInfo(null, null, info.getObjectName(), info.getObjectType(), true);
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

  public static DataStore getQueryResult(WbConnection conn, String query)
  {
    Statement stmt = null;
    ResultSet rs = null;
    DataStore result = null;
    try
    {
      stmt = conn.createStatement();
      rs = stmt.executeQuery(query);
      result = new DataStore(rs, conn, true);
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
      if (element == null || element.isEmpty()) continue;
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

  public static List<String> readLines(File input, String encoding)
    throws IOException
  {
    return FileUtil.getLines(EncodingUtil.createBufferedReader(input, encoding));
  }

  public static void dumpTableContent(WbConnection conn, String tableName)
  {
    dumpQuery(conn, "select * from " + tableName);
  }

  public static void dumpQuery(WbConnection conn, String query)
  {
    try (Statement stmt = conn.createStatementForQuery();
         ResultSet rs = stmt.executeQuery(query))
    {
      DataStore ds = new DataStore(rs, true);
      DataStorePrinter printer = new DataStorePrinter(ds);
      printer.printTo(System.out);
    }
    catch (Exception ex)
    {
      // ignore
    }
  }
}
