/*
 * WbExportTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.sql.BatchRunner;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.DdlObjectInfo;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.LobFileParameter;
import workbench.util.LobFileStatement;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.ZipUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbExportTest
	extends WbTestCase
{
	private String basedir;
	private final int rowcount = 10;
	private WbExport exportCmd;
	private WbConnection connection;
	private TestUtil util;

	public WbExportTest()
	{
		super("WbExportTest");
		util = getTestUtil();
		basedir = util.getBaseDir();
		exportCmd = new WbExport();
	}

	@Before
	public void setUp()
		throws Exception
	{
		util.prepareEnvironment();
		connection = prepareDatabase();
		exportCmd.setConnection(this.connection);
	}

	@After
	public void tearDown()
		throws Exception
	{
		connection.disconnect();
		util.emptyBaseDirectory();
	}

	@Test
	public void testIsTypeValid()
	{
		WbExport exp = new WbExport();
		assertTrue(exp.isTypeValid("text"));
		assertTrue(exp.isTypeValid("TEXT"));
		assertTrue(exp.isTypeValid("xml"));
		assertTrue(exp.isTypeValid("sql"));
		assertTrue(exp.isTypeValid("HTML"));
		assertTrue(exp.isTypeValid("sqlUpdate"));
		assertTrue(exp.isTypeValid("sqlInsert"));
		assertTrue(exp.isTypeValid("SQLDeleteInsert"));
		assertTrue(exp.isTypeValid("xlsx"));
		assertTrue(exp.isTypeValid("ods"));
		assertFalse(exp.isTypeValid("calc"));
		assertFalse(exp.isTypeValid("excel"));
		assertFalse(exp.isTypeValid("odt"));
	}

	@Test
	public void testQuoteHeader()
		throws Exception
	{
		WbConnection con = util.getHSQLConnection("quoteheader");
		try
		{
			TestUtil.executeScript(con,
				"create table test (id integer, some_value varchar(20)); \n" +
				"insert into test values (1, 'foo'), (2, 'bar'); \n" +
				"commit;");

			WbFile out = util.getFile("quoted.txt");

			exportCmd.setConnection(con);
			StatementRunnerResult result = exportCmd.execute(
				"wbexport -sourceTable=test -delimiter='|' -header=true -quoteAlways=true -quoteChar='\"' -quoteHeader=true -type=text -file='" + out.getFullPath() + "'");

			String msg = result.getMessageBuffer().toString();
			assertTrue(msg, result.isSuccess());
			assertTrue(out.exists());
      List<String> lines = TestUtil.readLines(out, null);
			assertNotNull(lines);
      assertEquals(3, lines.size());
			assertEquals("\"ID\"|\"SOME_VALUE\"", lines.get(0));

			result = exportCmd.execute(
				"wbexport -sourceTable=test -delimiter='_' -header=true -quoteAlways=false -quoteChar='\"' -quoteHeader=true -type=text -file='" + out.getFullPath() + "'");

			msg = result.getMessageBuffer().toString();
			assertTrue(msg, result.isSuccess());
      lines = TestUtil.readLines(out, null);
			assertNotNull(lines);
      assertEquals(3, lines.size());
			assertEquals("ID_\"SOME_VALUE\"", lines.get(0).trim());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testTrimData()
		throws Exception
	{
		WbConnection con = util.getHSQLConnection("trimtest");
		try
		{
			TestUtil.executeScript(con,
				"create table test (some_data char(20), id integer); \n" +
				"insert into test values ('42', 1); \n" +
				"commit;");

			WbFile out = util.getFile("trim.txt");

			exportCmd.setConnection(con);
			StatementRunnerResult result = exportCmd.execute(
				"wbexport -sourceTable=test -delimiter='|' -header=false -type=text -file='" + out.getFullPath() + "'");


			String msg = result.getMessageBuffer().toString();
			assertTrue(msg, result.isSuccess());
			assertTrue(out.exists());
			String content = FileUtil.readFile(out, null);
			assertNotNull(content);
			assertEquals("42                  |1", content.trim());

			result = exportCmd.execute(
				"wbexport -sourceTable=test -delimiter='|' -header=false -trimCharData=true -type=text -file='" + out.getFullPath() + "'");

			msg = result.getMessageBuffer().toString();
			assertTrue(msg, result.isSuccess());
			assertTrue(out.exists());
			content = FileUtil.readFile(out, null);
			assertNotNull(content);
			assertEquals("42|1", content.trim());

			con.getProfile().setTrimCharData(true);
			result = exportCmd.execute(
				"wbexport -sourceTable=test -delimiter='|' -header=false -type=text -file='" + out.getFullPath() + "'");
			msg = result.getMessageBuffer().toString();
			assertTrue(msg, result.isSuccess());
			assertTrue(out.exists());
			content = FileUtil.readFile(out, null);
			assertNotNull(content);
			assertEquals("42|1", content.trim());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testDecimal()
		throws Exception
	{
		TestUtil.executeScript(connection,
			"create table products (id integer, price decimal(10,2));\n" +
			"insert into products values (1, 3.14);\n" +
			"commit;");

		WbFile out = util.getFile("products.txt");

		StatementRunnerResult result = exportCmd.execute(
			"wbexport -sourceTable=products -delimiter='|' -header=false -type=text -decimal=',' -file='" + out.getFullPath() + "'");
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());
		assertTrue(out.exists());
		String content = FileUtil.readFile(out, null);
		assertNotNull(content);
		assertEquals("1|3,14", content.trim());

		result = exportCmd.execute(
			"wbexport -sourceTable=products -delimiter='|' -header=false -type=text -decimal=',' -fixedDigits=4 -file='" + out.getFullPath() + "'");
		msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());
		assertTrue(out.exists());
		content = FileUtil.readFile(out, null);
		assertNotNull(content);
		assertEquals("1|3,1400", content.trim());

    String sql =
      "create table readings (id integer, some_value decimal(34,14)); \n" +
      " \n" +
      "insert into readings  \n" +
      "values \n" +
      "(1, 1.12345678901234), \n" +
      "(2, 12345678.1234567), \n" +
      "(3, 12345678901234.1)";

    TestUtil.executeScript(connection, sql);

		result = exportCmd.execute("wbexport -sourceTable=readings -maxDigits=0 -delimiter='|' -header=false -type=text -decimal=',' -file='" + out.getFullPath() + "'");
		msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());
		assertTrue(out.exists());
    List<String> lines = TestUtil.readLines(out, "ISO-8859-1");
		assertNotNull(lines);
    assertEquals(3, lines.size());
    for (String line : lines)
    {
      String[] elements = line.split("|");
      switch (elements[0])
      {
        case "1":
          assertEquals("1,12345678901234", elements[1]);
          break;
        case "2":
          assertEquals("12345678,1234567", elements[1]);
          break;
        case "3":
          assertEquals("12345678901234,1", elements[1]);
          break;
      }
    }
	}

	@Test
	public void testPrefix()
		throws Exception
	{
		StatementRunnerResult result = exportCmd.execute(
			"wbexport -sourceTable=person,junit_test " +
			"-sourceTablePrefix=public. " +
			"-outputDir='" +  util.getBaseDir() + "' " +
			"-type=text");
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());
		File person = new File(util.getBaseDir(), "person.txt");
		assertTrue(person.exists());
		File test = new File(util.getBaseDir(), "junit_test.txt");
		assertTrue(test.exists());
	}

	@Test
	public void testAppendSQL()
		throws Exception
	{
		Statement stmt = this.connection.createStatement();
		stmt.executeUpdate("DELETE FROM JUNIT_TEST");
		stmt.executeUpdate("DELETE FROM PERSON");
		connection.commit();
		SqlUtil.closeStatement(stmt);

		WbFile exportFile = new WbFile(util.getBaseDir(), "create_tbl.sql");
		StatementRunnerResult result = exportCmd.execute("wbexport -createTable=true -file='" + exportFile.getFullPath() + "' -type=sqlinsert -useSchema=false -sourceTable=junit_test");
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());
		assertTrue(exportFile.exists());

		long fsize = exportFile.length();

		FileReader in = new FileReader(exportFile);
		String script = FileUtil.readCharacters(in);
		ScriptParser p = new ScriptParser(script);
		assertEquals(2, p.getSize()); // 3 Statements: CREATE TABLE, ALTER TABLE

		result = exportCmd.execute("wbexport -createTable=true -file='" + exportFile.getFullPath() + "' -type=sqlinsert -sourceTable=person -useSchema=false -append=true");
		msg = result.getMessageBuffer().toString();
		assertTrue(result.isSuccess());
		assertTrue(exportFile.exists());
		long fsize2 = exportFile.length();
		assertTrue(fsize2 > fsize);

		result = exportCmd.execute("wbexport -createTable=true -file='" + exportFile.getFullPath() + "' -type=sqlinsert -sourceTable=person -useSchema=false -append=true -writeEmptyResults=false");
		msg = result.getMessageBuffer().toString();
		assertTrue(result.isSuccess());
		assertTrue(exportFile.exists());
		long fsize3 = exportFile.length();
		assertTrue(fsize2 == fsize3);

		in = new FileReader(exportFile);
		script = FileUtil.readCharacters(in);
		p = new ScriptParser(script);
		assertEquals(4, p.getSize());

		String create1 = p.getCommand(0);
		assertEquals("CREATE", SqlUtil.getSqlVerb(create1));
		assertEquals("TABLE", SqlUtil.getCreateType(create1));
		assertEquals("JUNIT_TEST", TestUtil.getCreateTable(create1));

		String create2 = p.getCommand(2);
		assertEquals("CREATE", SqlUtil.getSqlVerb(create2));
		assertEquals("TABLE", SqlUtil.getCreateType(create2));
		assertEquals("PERSON", TestUtil.getCreateTable(create2));
	}

	@Test
	public void testExportRownum()
		throws Exception
	{
		Statement stmt = this.connection.createStatement();
		stmt.executeUpdate("CREATE MEMORY TABLE rownumtest (firstname varchar(100), lastname varchar(100))");
		PreparedStatement pstmt = connection.getSqlConnection().prepareStatement("insert into rownumtest (firstname, lastname) values (?,?)");
		int rows = 20;
		for (int i=0; i < rows; i ++)
		{
			pstmt.setString(1, "FirstName" + i);
			pstmt.setString(2, "LastName" + i);
			pstmt.executeUpdate();
		}
		connection.commit();

		WbFile exportFile = new WbFile(util.getBaseDir(), "rownum_test.txt");
		StatementRunnerResult result = exportCmd.execute("wbexport -header=true -file='" + exportFile.getFullPath() + "' -type=text -sourceTable=rownumtest -rowNumberColumn=rownum");
//		String msg = result.getMessageBuffer().toString();
		assertTrue(result.isSuccess());
		assertTrue(exportFile.exists());
		BufferedReader in = new BufferedReader(new FileReader(exportFile));
		List<String> content = FileUtil.getLines(in);
		assertTrue(content.size() == (rows + 1));
		List<String> elements = StringUtil.stringToList(content.get(0), "\t", true, true, false);
		assertTrue(elements.size() == 3);
		assertEquals("rownum", elements.get(0));
		for (int i=1; i < content.size(); i++)
		{
			List<String> values = StringUtil.stringToList(content.get(i), "\t", false, true, false);
			assertTrue(values.size() == 3);
			assertEquals(values.get(0), Integer.toString(i));
		}
	}

	@Test
	public void testCreateDir()
		throws Exception
	{
		String outputDir = util.getBaseDir() + "/nonexisting";
		File exportDir = new WbFile(outputDir);
		exportDir.delete();
		StatementRunnerResult result = exportCmd.execute("wbexport -outputDir='" + exportDir.getAbsolutePath() + "' -type=text -sourceTable=*");
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg.indexOf("not found!") > -1);
		assertFalse("Export did not fail", result.isSuccess());
		assertFalse("Export directory created", exportDir.exists());

		result = exportCmd.execute("wbexport -outputDir='" + exportDir.getAbsolutePath() + "' -type=text -createDir=true -sourceTable=*");
		assertTrue("Export failed", result.isSuccess());
		assertTrue("Export directory not created", exportDir.exists());
		WbFile f = new WbFile(exportDir, "junit_test.txt");
		assertTrue("Export file for table JUNIT_TEST not created", f.exists());
		f = new WbFile(exportDir, "person.txt");
		assertTrue("Export file for table PERSON not created", f.exists());
		f = new WbFile(exportDir, "blob_test.txt");
		assertTrue("Export file for table BLOB_TEST not created", f.exists());
	}

	@Test
	public void testQuoteEscaping()
	{
		try
		{
			File exportFile = new File(this.basedir, "quote_escaping_test.txt");

			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE quote_test (nr integer, testvalue varchar(100))");
			stmt.executeUpdate("insert into quote_test (nr, testvalue) values (1, 'first')");
			stmt.executeUpdate("insert into quote_test (nr, testvalue) values (2, 'with\"quote')");
			stmt.executeUpdate("insert into quote_test (nr, testvalue) values (3, 'with\ttab')");
			connection.commit();

			// Test escaping
			StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -sourcetable=quote_test -quoteCharEscaping=escape -quoteChar='\"' -header=false");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertEquals("Export file not created", true, exportFile.exists());

			List<String> lines = StringUtil.readLines(exportFile);
			assertEquals("Not enough lines exported", 3, lines.size());
			assertEquals("Wrong second line", "2\twith\\\"quote", lines.get(1));
			assertEquals("Wrong third line", "3\t\"with\ttab\"", lines.get(2));

			// Test escaping
			result = exportCmd.execute("wbexport -file=\"" + exportFile.getAbsolutePath() + "\" -type=text -sourcetable=quote_test -quoteCharEscaping=duplicate -quoteChar='\"' -header=false");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertEquals("Export file not created", true, exportFile.exists());

			lines = StringUtil.readLines(exportFile);
			assertEquals("Not enough lines exported", 3, lines.size());
			assertEquals("Wrong second line", "2\twith\"\"quote", lines.get(1));

			// Test without quote character
			result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=\"text\" -sourcetable=quote_test -quoteCharEscaping=duplicate -quoteChar=\"'\" -header=false");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertEquals("Export file not created", true, exportFile.exists());

			lines = StringUtil.readLines(exportFile);
			assertEquals("Not enough lines exported", 3, lines.size());
			assertEquals("Wrong second line", "2\twith\"quote", lines.get(1));
			assertEquals("Wrong third line", "3\t'with\ttab'", lines.get(2));

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

	@Test
	public void testCommit()
	{
		try
		{
			File exportFile = new File(this.basedir, "commit_test.sql");

			// Test default behaviour
			StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sqlinsert -sourcetable=junit_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertEquals("Export file not created", true, exportFile.exists());

			ScriptParser p = new ScriptParser();
			p.setFile(exportFile);

			assertEquals("Wrong number of statements", rowcount + 1, p.getSize());

			// Test no commit at all
			exportFile.delete();
			result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sqlinsert -sourcetable=junit_test -commitEvery=none");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertEquals("Export file not created", true, exportFile.exists());

			p = new ScriptParser();
			p.setFile(exportFile);
			assertEquals("Wrong number of statements", rowcount, p.getSize());

			// Test commit each statement
			exportFile.delete();
			result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sqlinsert -sourcetable=junit_test -commitEvery=1");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertEquals("Export file not created", true, exportFile.exists());

			p = new ScriptParser();
			p.setFile(exportFile);
			assertEquals("Wrong number of statements", rowcount * 2, p.getSize());

			String verb = SqlUtil.getSqlVerb(p.getCommand(0));
			assertEquals("Wrong first statement", "INSERT", verb);

			verb = SqlUtil.getSqlVerb(p.getCommand(1));
			assertEquals("No commit as second statement", "COMMIT", verb);

			verb = SqlUtil.getSqlVerb(p.getCommand(3));
			assertEquals("No commit", "COMMIT", verb);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDateLiterals()
		throws Exception
	{
		File exportFile = new File(this.basedir, "date_literal_test.sql");
		exportFile.delete();

		Statement stmt = this.connection.createStatement();
		stmt.executeUpdate("CREATE TABLE literal_test (nr integer, date_col DATE, ts_col TIMESTAMP)");
		stmt.executeUpdate("insert into literal_test (nr, date_col, ts_col) values (1, '2006-01-01', '2007-02-02 14:15:16')");
		this.connection.commit();

		// Test JDBC literals
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sql -sqlDateLiterals=jdbc -sourcetable=literal_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
		assertEquals("Export file not created", true, exportFile.exists());

		FileReader in = new FileReader(exportFile);
		String script = FileUtil.readCharacters(in);
		ScriptParser p = new ScriptParser(script);

		// WbExport creates 2 statements: the INSERT and the COMMIT
		assertEquals("Wrong number of statements", 2, p.getSize());

		String sql = p.getCommand(0);
		String verb = SqlUtil.getSqlVerb(sql);
		assertEquals("Not an insert statement", "INSERT", verb);
		assertEquals("JDBC Date literal not found", true, sql.indexOf("{d '2006-01-01'}") > -1);
		assertEquals("JDBC Timestamp literal not found", true, sql.indexOf("{ts '2007-02-02 14:15:16") > -1);

		// Test ANSI literals
		exportFile.delete();
		result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sql -sqlDateLiterals=ansi -sourcetable=literal_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
		assertEquals("Export file not created", true, exportFile.exists());

		in = new FileReader(exportFile);
		script = FileUtil.readCharacters(in);
		p = new ScriptParser(script);
		sql = p.getCommand(0);
		verb = SqlUtil.getSqlVerb(script);
		assertEquals("Not an insert statement", "INSERT", verb);
		assertEquals("ANSI Date literal not found", true, sql.indexOf("DATE '2006-01-01'") > -1);
		assertEquals("ANSI Timestamp literal not found", true, sql.indexOf("TIMESTAMP '2007-02-02 14:15:16") > -1);

		// Test Standard literals
		exportFile.delete();
		result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sql -sqlDateLiterals=standard -sourcetable=literal_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
		assertEquals("Export file not created", true, exportFile.exists());

		in = new FileReader(exportFile);
		script = FileUtil.readCharacters(in);
		p = new ScriptParser(script);
		sql = p.getCommand(0);
		verb = SqlUtil.getSqlVerb(script);
		assertEquals("Not an insert statement", "INSERT", verb);
		assertEquals("STANDARD Date literal not found", true, sql.indexOf("'2006-01-01'") > -1);
		assertEquals("STANDARD Timestamp literal not found", true, sql.indexOf("'2007-02-02 14:15:16") > -1);

		// Test Oracle literals
		exportFile.delete();
		result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sql -sqlDateLiterals=Oracle -sourcetable=literal_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
		assertEquals("Export file not created", true, exportFile.exists());

		in = new FileReader(exportFile);
		script = FileUtil.readCharacters(in);
		p = new ScriptParser(script);
		sql = p.getCommand(0);
		verb = SqlUtil.getSqlVerb(script);
//			System.out.println("Statement=" + sql);
		assertEquals("Not an insert statement", "INSERT", verb);
		assertEquals("Oracle Date literal not found", true, sql.indexOf("to_date('2006-01-01'") > -1);
		assertEquals("Oracle Timestamp literal not found", true, sql.indexOf("to_timestamp('2007-02-02 14:15:16.000") > -1);
	}

	@Test
	public void testAppend()
		throws Exception
	{
		File exportFile = new File(this.basedir, "export_append.txt");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -sourcetable=junit_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());
		// WbExport creates an empty line at the end plus the header line
		assertEquals("Wrong number of lines", rowcount + 1, TestUtil.countLines(exportFile));

		result = exportCmd.execute("wbexport -append=true -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -sourcetable=junit_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Wrong number of lines", (rowcount * 2) + 1, TestUtil.countLines(exportFile));
	}

	@Test
	public void testInvalidFile()
		throws Exception
	{
		File exportFile = new File("/this/is/expected/to/fail/no_export.txt");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -sourcetable=blob_test");
		assertEquals("Export did not fail", result.isSuccess(), false);
	}

	@Test
	public void testTextExportCompressed()
		throws Exception
	{
		File exportFile = new File(this.basedir, "zip_text_export.txt");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -sourcetable=blob_test -compress=true");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		File zip = new File(this.basedir, "zip_text_export_lobs.zip");
		assertEquals("Archive not created", true, zip.exists());
	}

	@Test
	public void testXmlExportCompressed()
		throws Exception
	{
		File exportFile = new File(this.basedir, "zip_xml_export.xml");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=xml -sourcetable=blob_test -compress=true");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		File zip = new File(this.basedir, "zip_xml_export_lobs.zip");
		assertEquals("Archive not created", true, zip.exists());
	}

	@Test
	public void testAlternateBlobExport()
		throws Exception
	{
		File exportFile = new File(this.basedir, "blob_export.txt");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -blobidcols=nr -sourcetable=blob_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("No export file created", true, exportFile.exists());

		File bfile = new File(this.basedir, "blob_export_data_#1.data");
		assertEquals("Blob data not exported", true, bfile.exists());
		assertEquals("Wrong file size", 21378, bfile.length());

		bfile = new File(this.basedir, "blob_export_data_#2.data");
		assertEquals("Blob data not exported", true, bfile.exists());
		assertEquals("Wrong file size", 7218, bfile.length());
	}

	@Test
	public void testBlobEncoding()
		throws Exception
	{
		File exportFile = new File(this.basedir, "blob_data.txt");

		StatementRunner runner = util.createConnectedStatementRunner(connection);
		runner.runStatement("wbexport -sourceTable=blob_test -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -blobType=base64;");
		StatementRunnerResult result = runner.getResult();
//		System.out.println("**************\n" + result.getMessageBuffer().toString() + "\n**************");
		assertTrue(result.isSuccess());
		assertEquals("No export file created", true, exportFile.exists());
		BufferedReader reader = new BufferedReader(new FileReader(exportFile));
		List<String> lines = FileUtil.getLines(reader);
		assertEquals(3, lines.size());
		assertEquals("NR\tDATA", lines.get(0));
		assertTrue(lines.get(1).startsWith("1\t/9j/4AAQSkZJRgABAQEBLAEsAAD/"));
		assertTrue(lines.get(2).startsWith("2\tiVBORw0KGgoAAAANSUhEUgAAAaMAAAEeCAIAAACoo+0IAAAb+UlEQVR4Xu3dP5PkxnnH8cbVLgK69C7kSIFCpQoVOnDs8AKZAYMNmOk2U3ABA5oq72tQ4NDFiKHl4MqsMi84yy"));
		runner.done();

		runner.runStatement("wbexport -sourceTable=blob_test -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -blobType=ansi;");
		result = runner.getResult();
//		System.out.println("**************\n" + result.getMessageBuffer().toString() + "\n**************");
		assertTrue(result.isSuccess());
		assertEquals("No export file created", true, exportFile.exists());
		reader = new BufferedReader(new FileReader(exportFile));
		lines = FileUtil.getLines(reader);
		assertEquals(3, lines.size());
		assertEquals("NR\tDATA", lines.get(0));
		assertTrue(lines.get(1).startsWith("1\tX'ffd8ffe00010"));
		System.out.println(lines.get(2));
		assertTrue(lines.get(2).startsWith("2\tX'"));

	}

	@Test
	public void testBlobExtensionCol()
		throws Exception
	{
		File exportFile = new File(this.basedir, "blob_ext.txt");

		StatementRunner runner = util.createConnectedStatementRunner(connection);
		runner.runStatement("wbexport -filenameColumn=fname -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true;");
		StatementRunnerResult result = runner.getResult();
//		System.out.println("**************\n" + result.getMessageBuffer().toString() + "\n**************");
		runner.runStatement("select  \n" +
					 "   case \n" +
					 "     when nr = 1 then 'first.jpg' \n" +
					 "     when nr = 2 then 'second.gif' \n" +
					 "     else nr||'.data' \n" +
					 "   end as fname,  \n" +
					 "   data \n" +
					 "from blob_test ");
		assertEquals("No export file created", true, exportFile.exists());

		File bfile = new File(this.basedir, "first.jpg");
		assertEquals("jpeg file not found", true, bfile.exists());

		bfile = new File(this.basedir, "second.gif");
		assertEquals("gif file not found", true, bfile.exists());
		runner.done();
	}

	@Test
	public void testTextBlobExport()
		throws Exception
	{
		try
		{
			File exportFile = new File(this.basedir, "blob_export.txt");
			StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -sourcetable=blob_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			assertEquals("No export file created", true, exportFile.exists());

			File bfile = new File(this.basedir, "blob_export_r1_c2.data");
			assertEquals("Blob data not exported", true, bfile.exists());
			assertEquals("Wrong file size", 21378, bfile.length());

			bfile = new File(this.basedir, "blob_export_r2_c2.data");
			assertEquals("Blob data not exported", true, bfile.exists());
			assertEquals("Wrong file size", 7218, bfile.length());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testBlobDistribution()
		throws Exception
	{
		try
		{
			File exportFile = new File(this.basedir, "blob_export.txt");
			TestUtil.executeScript(connection,
				"INSERT INTO BLOB_TEST VALUES (3,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (4,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (5,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (6,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (7,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (8,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (9,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (10,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (11,'01010101');\n"	+
				"INSERT INTO BLOB_TEST VALUES (12,'01010101');\n"	+
				"commit;\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (3,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (4,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (5,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (6,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (7,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (8,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (9,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (10,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (11,'01010101');\n"	+
				"INSERT INTO BLOB_TEST2 VALUES (12,'01010101');\n"	+
				"commit;\n"
				);
			StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -lobsPerDirectory=5 -type=text -header=true -sourcetable=blob_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			assertEquals("No export file created", true, exportFile.exists());

			File dir1 = new File(basedir, "blob_export_lobs_000001");
			assertTrue(dir1.exists());
			File bfile = new File(dir1, "blob_export_r1_c2.data");
			assertEquals("Blob data not exported", true, bfile.exists());

			File dir2 = new File(basedir, "blob_export_lobs_000002");
			assertTrue(dir2.exists());
			bfile = new File(dir2, "blob_export_r6_c2.data");
			assertEquals("Blob data not exported", true, bfile.exists());

			File dir3 = new File(basedir, "blob_export_lobs_000003");
			assertTrue(dir3.exists());
			bfile = new File(dir3, "blob_export_r11_c2.data");
			assertEquals("Blob data not exported", true, bfile.exists());


			util.emptyBaseDirectory();
			result = exportCmd.execute("wbexport -outputDir='" + exportFile.getParentFile().getAbsolutePath() + "' -lobsPerDirectory=5 -type=text -header=true -sourcetable=blob*");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			for (int i=1; i <=3; i++)
			{
				File lobDir = new File(basedir, "blob_test_lobs_00000" + Integer.toString(i));
				assertTrue(lobDir.exists());

				lobDir = new File(basedir, "blob_test2_lobs_00000" + Integer.toString(i));
				assertTrue(lobDir.exists());
			}
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testMultipleCompressedBlobExport()
		throws Exception
	{
		File dir = new File(this.basedir);
		StatementRunnerResult result = exportCmd.execute("wbexport -outputdir='" + dir.getAbsolutePath() + "' -type=text -header=true -compress=true -sourcetable=blob_test%");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		File f1 = new File(this.basedir, "blob_test.zip");
		assertTrue("No export file created", f1.exists());

		File f2 = new File(this.basedir, "blob_test2.zip");
		assertTrue("No export file created", f2.exists());

		File bfile1 = new File(this.basedir, "blob_test_lobs.zip");
		assertTrue("Blob data not exported", bfile1.exists());

		File bfile2 = new File(this.basedir, "blob_test2_lobs.zip");
		assertTrue("Blob data not exported", bfile2.exists());

		List<String> entries1 = ZipUtil.getFiles(bfile1);
		assertEquals("Not enough blob entries", 2, entries1.size());
		assertTrue("First blob not in ZIP Archive", entries1.contains("r1_c2.data"));
		assertTrue("Second blob not in ZIP Archive", entries1.contains("r2_c2.data"));

		List<String> entries2 = ZipUtil.getFiles(bfile2);
		assertEquals("Not enough blob entries", 2, entries2.size());
		assertTrue("First blob not in ZIP Archive", entries2.contains("r1_c2.data"));
		assertTrue("Second blob not in ZIP Archive", entries2.contains("r2_c2.data"));

		assertTrue("Could not delete file", f1.delete());
		assertTrue("Could not delete file", f2.delete());
		assertTrue("Could not delete file", bfile1.delete());
		assertTrue("Could not delete file", bfile2.delete());
	}

	@Test
	public void testSingleExportWithDir()
		throws Exception
	{
		StatementRunnerResult result = exportCmd.execute("wbexport -outputdir='" + basedir + "' -type=text -header=true -sourcetable=junit_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		File exportFile = new File(this.basedir, "junit_test.txt");
		assertEquals("Export file not created", true, exportFile.exists());
		assertEquals("Wrong number of lines", rowcount + 1, TestUtil.countLines(exportFile));
	}

	@Test
	public void testTextExport()
		throws Exception
	{
		File exportFile = new File(this.basedir, "export.txt");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -sourcetable=junit_test -formatFile=oracle,sqlserver");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());
		assertEquals("Wrong number of lines", rowcount + 1, TestUtil.countLines(exportFile));

		File ctl = new File(this.basedir, "export.ctl");
		assertEquals("Control file not created", true, ctl.exists());

		List<String> lines = StringUtil.readLines(ctl);
//			System.out.println("first line: " + lines.get(0));
		assertTrue(lines.get(0).startsWith("--"));
		assertTrue(lines.get(1).indexOf("skip=1") > -1);

		File bcp = new File(this.basedir, "export.fmt");
		assertEquals("BCP format file not created", true, bcp.exists());
		lines = StringUtil.readLines(bcp);
		assertEquals("7.0", lines.get(0));
		assertEquals("3", lines.get(1));
		assertTrue(lines.get(2).indexOf(" NR") > -1);
		assertTrue(lines.get(2).indexOf(" \"\\t\"") > -1);
		assertTrue(lines.get(3).indexOf(" FIRSTNAME") > -1);
		assertTrue(lines.get(4).indexOf(" LASTNAME") > -1);
		assertTrue(lines.get(4).indexOf(" \"\\n\"") > -1);

	}

	@Test
	public void testTableWhere()
		throws Exception
	{
		StatementRunnerResult result = exportCmd.execute("wbexport -outputDir='" + basedir + "' -type=text -header=true -sourcetable=junit_test, person -tableWhere=\"where nr < 10\"");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		File person = new File(this.basedir, "person.txt");
		assertTrue(person.exists());
		BufferedReader in = EncodingUtil.createBufferedReader(person, null);
		List<String> lines = FileUtil.getLines(in);
		assertEquals(11, lines.size()); // 10 rows plus header line

		File test = new File(basedir, "junit_test.txt");
		assertTrue(test.exists());
		in = EncodingUtil.createBufferedReader(test, null);
		lines = FileUtil.getLines(in);
		assertEquals(11, lines.size()); // 10 rows plus header line
	}


	@Test
	public void testXmlClobExport()
		throws Exception
	{
		File exportFile = new File(this.basedir, "export.xml");
		String data1;
		String data2;
		try (Statement stmt = connection.createStatement())
		{
			stmt.executeUpdate("CREATE MEMORY TABLE clob_test(nr integer, clob_data CLOB)");
			data1 = "This is the first clob content";
			stmt.executeUpdate("insert into clob_test values (1, '" +  data1+ "')");
			data2 = "This is the second clob content";
			stmt.executeUpdate("insert into clob_test values (2, '" +  data2+ "')");
			connection.commit();
		}

		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=xml -header=true -sourcetable=clob_test -clobAsFile=true");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());

		File dataFile1 = new File(this.basedir, "export_r1_c2.data");
		assertEquals("Clob file not created", true, dataFile1.exists());

		File dataFile2 = new File(this.basedir, "export_r2_c2.data");
		assertEquals("Clob file not created", true, dataFile2.exists());

		Reader in = EncodingUtil.createReader(dataFile1, "UTF-8");
		String content = FileUtil.readCharacters(in);
		assertEquals("Wrong clob content exported", data1, content);
		in = EncodingUtil.createReader(dataFile2, "UTF-8");
		content = FileUtil.readCharacters(in);
		assertEquals("Wrong clob content exported", data2, content);
	}


	@Test
	public void testDataModifier()
		throws Exception
	{
		File exportFile = new File(this.basedir, "export.txt");
		TestUtil.executeScript(connection,
			"create table some_stuff (id integer, some_value varchar(200));\n" +
			"insert into some_stuff values (1, 'this\r\nis\r\na\ntest');\n" +
			"commit;\n");

		StatementRunnerResult result = exportCmd.execute(
			"wbexport -file='" + exportFile.getAbsolutePath() + "' " +
			"-type=text " +
			"-encoding=iso88591 " +
			"-" + RegexModifierParameter.ARG_REPLACE_REGEX + "=(\\n|\\r\\n) " +
			"-" + RegexModifierParameter.ARG_REPLACE_WITH + "=' ' " +
			"-header=false " +
			"-sourcetable=some_stuff");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());

		String content = FileUtil.readFile(exportFile, "ISO-8859-1");
		assertEquals("1\tthis is a test", content.trim());
	}

	@Test
	public void testTextClobExport()
		throws Exception
	{
		File exportFile = new File(this.basedir, "export.txt");
		String data1;
		String data2;
		try (Statement stmt = connection.createStatement())
		{
			stmt.executeUpdate("CREATE MEMORY TABLE clob_test(nr integer, clob_data CLOB)");
			data1 = "This is the first clob content";
			stmt.executeUpdate("insert into clob_test values (1, '" +  data1+ "')");
			data2 = "This is the second clob content";
			stmt.executeUpdate("insert into clob_test values (2, '" +  data2+ "')");
			connection.commit();
		}

		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -sourcetable=clob_test -clobAsFile=true -formatFile=oracle");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());

		File dataFile1 = new File(this.basedir, "export_r1_c2.data");
		assertEquals("Clob file not created", true, dataFile1.exists());

		File dataFile2 = new File(this.basedir, "export_r2_c2.data");
		assertEquals("Clob file not created", true, dataFile2.exists());

		Reader in = EncodingUtil.createReader(dataFile1, "UTF-8");
		String content = FileUtil.readCharacters(in);
		assertEquals("Wrong clob content exported", data1, content);
		in = EncodingUtil.createReader(dataFile2, "UTF-8");
		content = FileUtil.readCharacters(in);
		assertEquals("Wrong clob content exported", data2, content);

		File ctl = new File(this.basedir, "export.ctl");
		assertEquals("Oracle loader file not written", true, ctl.exists());

		// Now check if the SQL*Loader file contains the correct
		// syntax to load the external files.
		FileReader fr = new FileReader(ctl);
		String ctlfile = FileUtil.readCharacters(fr);

		int pos = ctlfile.indexOf("lob_file_clob_data FILLER");
		assertEquals("FILLER not found", true, pos > -1);
		pos = ctlfile.indexOf("CLOB_DATA LOBFILE(lob_file_clob_data)");
		assertEquals("File statement not found", true, pos > -1);
	}

	@Test
	public void testExportWithFailingSelect()
		throws Exception
	{
		File script = new File(this.basedir, "export.sql");
		File output = new File(this.basedir, "test.txt");

		TestUtil.writeFile(script,
			"wbexport -file='" + output.getAbsolutePath() + "' -type=text -header=true;\n" +
			"select * from xxxxx;");

		BatchRunner runner = new BatchRunner(script.getAbsolutePath());
		runner.setVerboseLogging(false);
		runner.setShowProgress(false);
		runner.setBaseDir(this.basedir);
		runner.setConnection(this.connection);
		runner.setAbortOnError(true);
		runner.execute();
		assertEquals(false, runner.isSuccess());
		assertEquals(false, output.exists());
	}

	@Test
	public void testExportWithSelect()
		throws Exception
	{
		File script = new File(this.basedir, "export.sql");
		File output = new File(this.basedir, "test.txt");

		TestUtil.writeFile(script,
			"wbexport -file='" + output.getAbsolutePath() + "' -type=text -header=true;\n" +
			"select * from junit_test;");

		BatchRunner runner = new BatchRunner(script.getAbsolutePath());
		runner.setVerboseLogging(false);
		runner.setShowProgress(false);
		runner.setAbortOnError(true);

		runner.setBaseDir(this.basedir);
		runner.setConnection(this.connection);
		runner.execute();
		assertEquals("Script not executed", true, runner.isSuccess());
		assertEquals("Export file not created", true, output.exists());

		int lines = TestUtil.countLines(output);
		assertEquals("Not enough lines", rowcount + 1, lines);

		boolean deleted = output.delete();
		assertEquals("Export file is still locked", true, deleted);
	}

	@Test
	public void testSqlClobExport()
		throws Exception
	{
		try
		{
			util.disableSqlFormatting();
			TestUtil.executeScript(connection,
				"CREATE MEMORY TABLE clob_test(nr integer, clob_data CLOB);\n" +
				"INSERT INTO clob_test (nr, clob_data) values (1, 'First clob');\n" +
				"INSERT INTO clob_test (nr, clob_data) values (2, 'Second clob');\n"+
				"commit;"
			);

			File exportFile = new File(this.basedir, "clob_export.sql");
			StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sql -sourcetable=clob_test -clobAsFile=true -encoding=utf8");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			assertEquals("Export file not created", true, exportFile.exists());

			File dataFile = new File(this.basedir, "clob_export_r1_c2.data");
			assertEquals("First clob file not created", true, dataFile.exists());

			Reader in = EncodingUtil.createReader(dataFile, "UTF8");
			String contents = FileUtil.readCharacters(in);
			assertEquals("Wrong first clob content", "First clob", contents);

			dataFile = new File(this.basedir, "clob_export_r2_c2.data");
			assertEquals("Second blob file not created", true, dataFile.exists());
			in = EncodingUtil.createReader(dataFile, "UTF8");
			contents = FileUtil.readCharacters(in);
			assertEquals("Wrong second clob content", "Second clob", contents);

			ScriptParser p = new ScriptParser();
			p.setFile(exportFile);

			assertEquals("Wrong number of statements", 3, p.getSize());
			String sql = p.getCommand(0);
			String verb = SqlUtil.getSqlVerb(sql);
//			System.out.println("***********\n" + sql);
			assertEquals("Not an insert file", "INSERT", verb);

			LobFileStatement lob = new LobFileStatement(sql, this.basedir);
			assertEquals("No parameter detected", 1, lob.getParameterCount());

			LobFileParameter[] parms = lob.getParameters();
			assertNotNull("No encoding found in parameter", parms[0].getEncoding());
			assertEquals("Wrong parameter", "UTF8", parms[0].getEncoding().toUpperCase());
		}
		finally
		{
			util.restoreSqlFormatting();
		}
	}

	@Test
	public void testSqlBlobExport()
		throws Exception
	{
		try
		{
			util.disableSqlFormatting();
			File exportFile = new File(this.basedir, "blob_export.sql");
			StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sql -sourcetable=blob_test -blobtype=file");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			assertEquals("Export file not created", true, exportFile.exists());

			File dataFile = new File(this.basedir, "blob_export_r1_c2.data");
			assertEquals("First blob file not created", true, dataFile.exists());

			dataFile = new File(this.basedir, "blob_export_r2_c2.data");
			assertEquals("Second blob file not created", true, dataFile.exists());

			ScriptParser p = new ScriptParser();
			p.setFile(exportFile);

			assertEquals("Wrong number of statements", 3, p.getSize());
			String sql = p.getCommand(0);
			String verb = SqlUtil.getSqlVerb(sql);
			assertEquals("Not an insert file", "INSERT", verb);

			LobFileStatement lob = new LobFileStatement(sql, this.basedir);
			assertEquals("No BLOB parameter detected", 1, lob.getParameterCount());

			LobFileParameter[] parms = lob.getParameters();
			assertEquals("Wrong parameter", true, parms[0].isBinary());
		}
		finally
		{
			util.restoreSqlFormatting();
		}
	}

	@Test
	public void testSqlUpdateExport()
		throws Exception
	{
		File exportFile = new File(this.basedir, "update_export.sql");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sqlupdate -sourcetable=junit_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());

		ScriptParser p = new ScriptParser();
		p.setFile(exportFile);

		assertEquals("Wrong number of statements", rowcount + 1, p.getSize());
		String sql = p.getCommand(0);
		String verb = SqlUtil.getSqlVerb(sql);
		assertEquals("Not an insert file", "UPDATE", verb);
		String table = SqlUtil.getUpdateTable(sql, null);
		assertNotNull("No insert table found", table);
		assertEquals("Wrong target table", "JUNIT_TEST", table.toUpperCase());
	}

	@Test
	public void testSqlDeleteInsertExport()
		throws Exception
	{
		File exportFile = new File(this.basedir, "delete_insert_export.sql");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sqldeleteinsert -sourcetable=junit_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());

		ScriptParser p = new ScriptParser();
		p.setFile(exportFile);

		assertEquals("Wrong number of statements", (rowcount * 2) + 1, p.getSize());
		String sql = p.getCommand(0);
		String verb = SqlUtil.getSqlVerb(sql);
		assertEquals("No DELETE as the first statement", "DELETE", verb);

		String table = SqlUtil.getDeleteTable(sql);
		assertNotNull("No DELETE table found", table);
		assertEquals("Wrong target table", "JUNIT_TEST", table.toUpperCase());

		sql = p.getCommand(1);
		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("No INSERT as the second statement", "INSERT", verb);
		table = SqlUtil.getInsertTable(sql, null);
		assertNotNull("No INSERT table found", table);
		assertEquals("Wrong target table", "JUNIT_TEST", table.toUpperCase());
	}

	@Test
	public void testSqlInsertExport()
		throws Exception
	{
		File exportFile = new File(this.basedir, "insert_export.sql");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sqlinsert -sourcetable=junit_test -table=other_table");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());

		ScriptParser p = new ScriptParser();
		p.setFile(exportFile);

		assertEquals("Wrong number of statements", rowcount + 1, p.getSize());
		String sql = p.getCommand(0);
		String verb = SqlUtil.getSqlVerb(sql);
		assertEquals("Not an insert file", "INSERT", verb);
		String table = SqlUtil.getInsertTable(sql, null);
		assertNotNull("No insert table found", table);
		assertEquals("Wrong target table", "OTHER_TABLE", table.toUpperCase());
	}

	@Test
	public void testSqlInsertCreateTableExport()
		throws Exception
	{
		File exportFile = new File(this.basedir, "insert_export.sql");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -encoding=UTF8 -type=sqlinsert -sourcetable=junit_test -createTable=true -table=OTHER_TABLE");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Export file not created", true, exportFile.exists());

		ScriptParser p = new ScriptParser();
		p.setFile(exportFile);

//		String script = FileUtil.readFile(exportFile, "UTF-8");
//		System.out.println(script);

		assertEquals("Wrong number of statements", rowcount + 3, p.getSize());
		String sql = p.getCommand(0);
		String verb = SqlUtil.getSqlVerb(sql);

		// first verb must be the CREATE TABLE statement
		assertEquals("Not a CREATE TABLE statement", "CREATE", verb);
		DdlObjectInfo info = SqlUtil.getDDLObjectInfo(sql);
		assertTrue(info.getObjectName().equalsIgnoreCase("OTHER_TABLE"));

		sql = p.getCommand(1);
		verb = SqlUtil.getSqlVerb(sql);
		// then we expect the ALTER TABLE statement to define the primary key
		assertEquals("Not an ALTER TABLE statement", "ALTER", verb);

		sql = p.getCommand(2);
		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("Not an insert file", "INSERT", verb);

		String table = SqlUtil.getInsertTable(sql, null);
		assertNotNull("No insert table found", table);
		assertEquals("Wrong target table", "OTHER_TABLE", table.toUpperCase());
	}

	@Test
	public void testXmlBlobEncoding()
		throws Exception
	{
		File exportFile = new File(this.basedir, "encoded_blob_export.xml");
		StatementRunnerResult result = exportCmd.execute("wbexport -file='" + exportFile.getAbsolutePath() + "' -type=xml -blobType=base64 -sourcetable=blob_test -encoding=utf8");
		assertTrue("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess());
		assertEquals("No export file created", true, exportFile.exists());

		String file = FileUtil.readFile(exportFile, "UTF-8");
		String count = TestUtil.getXPathValue(file, "count(/wb-export/data/row-data)");
		assertEquals("2", count);

		String encoding = TestUtil.getXPathValue(file, "/wb-export/meta-data/wb-blob-encoding/text()");
		assertEquals("base64", encoding);

		String id = TestUtil.getXPathValue(file, "/wb-export/data/row-data[1]/column-data[1]/text()");
		String blob = TestUtil.getXPathValue(file, "/wb-export/data/row-data[1]/column-data[2]/text()");

		if ("1".equals(id))
		{
			assertTrue(blob.startsWith("/9j/4AAQSkZJRgABAQEBLAEsAAD/2"));
		}
	}

	@Test
	public void testXmlExport()
		throws Exception
	{
		try
		{
			StatementRunnerResult result = exportCmd.execute("wbexport -outputdir='" + basedir + "' -type=xml -sourcetable=*");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			File dir = new File(basedir);

			File[] files = dir.listFiles();
			int xmlFiles = 0;
			for (File file : files)
			{
				if (file.getAbsolutePath().endsWith(".xml"))
				{
					xmlFiles ++;
				}
			}
			assertEquals("Not all tables exported", 4, xmlFiles);

			File bfile = new File(this.basedir, "blob_test_r1_c2.data");
			assertEquals("Blob data not exported", true, bfile.exists());
			assertEquals("Wrong file size", 21378, bfile.length());

			bfile = new File(this.basedir, "blob_test_r2_c2.data");
			assertEquals("Blob data not exported", true, bfile.exists());
			assertEquals("Wrong file size", 7218, bfile.length());
		}
		finally
		{
			util.emptyBaseDirectory();
		}
	}

	@Test
	public void testExcludeTables()
		throws Exception
	{
		WbFile dir = new WbFile(util.getBaseDir());

		StatementRunnerResult result = exportCmd.execute("wbexport -header=true " +
			"-outputDir='" + util.getBaseDir() + "' " +
			"-type=text " +
			"-excludeTables=junit*,blob* " +
			"-sourceTable=* " +
			"-writeEmptyResults=false");

		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
		File[] files = dir.listFiles();
		//List<String> filenames = new ArrayList<5>();
		int count = 0;
		for (File f : files)
		{
			if (f.getName().endsWith(".txt"))
			{
				count ++;
			}
		}
		assertEquals(1, count);
	}

	@Test
	public void testEmptyWithAppend()
		throws Exception
	{
		WbFile f = new WbFile(util.getBaseDir(), "person.txt");
		try (Statement stmt = connection.createStatement())
		{
			stmt.executeUpdate("delete from person");
			connection.commit();
		}

		StatementRunnerResult result = exportCmd.execute("wbexport -header=true -file='" + f.getFullPath() + "' -type=text -sourcetable=person -writeEmptyResults=false");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
		assertFalse(f.exists());
	}

	@Test
	public void testNullString()
		throws Exception
	{
		try
		{
			WbFile f = new WbFile(util.getBaseDir(), "person.txt");
			TestUtil.executeScript(connection,
				"update person set firstname = null where nr = 1;\n" +
				"update person set firstname = 'NULL' where nr = 2;\n" +
				"delete from person where nr not in (1,2);\n" +
				"commit;");

			StatementRunnerResult result = exportCmd.execute(
				"wbexport -header=true " +
					"-file='" + f.getFullPath() + "' " +
					"-type=text " +
					"-header=false " +
					"-sourcetable=person " +
					"-nullString='\\null' -escapeText=7bit");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			BufferedReader in = new BufferedReader(new FileReader(f));
			List<String> lines = FileUtil.getLines(in);
			assertEquals(2, lines.size());
			String line = lines.get(0);
			String[] values = line.split("\t");
			assertEquals(3, values.length);
			assertEquals("\\null", values[1]);
			assertEquals("LastName1", values[2]);

			line = lines.get(1);
			System.out.println(line);
			values = line.split("\t");
			assertEquals(3, values.length);
			assertEquals("NULL", values[1]);
			assertEquals("LastName2", values[2]);
		}
		finally
		{
			util.emptyBaseDirectory();
		}
	}

	@Test
	public void testMultipleSpreadsheets()
		throws Exception
	{

		WbFile dir = new WbFile(util.getBaseDir());

		List<String> types = CollectionUtil.arrayList("xlsx", "xls", "ods");

		for (String type : types)
		{
			util.emptyBaseDirectory();
			StatementRunnerResult result = exportCmd.execute(
				"wbexport -type=" + type + " " +
				"-outputDir='" + util.getBaseDir() + "' " +
				"-sourceTable=person,junit_test ");

			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			File f1 = new File(dir, "person." + type);
			assertTrue(f1.exists());
			assertTrue(f1.length() > 1000);

			File f2 = new File(dir, "junit_test." + type);
			assertTrue(f2.exists());
			assertTrue(f2.length() > 1000);
		}
	}

	@Test
	public void testEmptyResult()
		throws Exception
	{
		try
		{
			WbFile f = new WbFile(util.getBaseDir(), "person.txt");
			try (Statement stmt = connection.createStatement())
			{
				stmt.executeUpdate("delete from person");
				connection.commit();
			}

			StatementRunnerResult result = exportCmd.execute("wbexport -header=true -file='" + f.getFullPath() + "' -type=text -sourcetable=person -writeEmptyResults=false");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertFalse(f.exists());

			result = exportCmd.execute("wbexport -header=true -file='" + f.getFullPath() + "' -type=text -sourcetable=junit_test -writeEmptyResults=false -append=true");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertTrue(f.exists());
			BufferedReader in = new BufferedReader(new FileReader(f));
			List<String> lines = FileUtil.getLines(in);
			assertEquals(rowcount + 1, lines.size());

			f.delete();

			result = exportCmd.execute("wbexport -header=true -file='" + f.getFullPath() + "' -type=text -sourcetable=junit_test -writeEmptyResults=false -append=true");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertTrue(f.exists());
			in = new BufferedReader(new FileReader(f));
			List<String> lines1 = FileUtil.getLines(in);
			assertEquals(rowcount + 1, lines1.size());

			// The second
			result = exportCmd.execute("wbexport -header=true -file='" + f.getFullPath() + "' -type=text -sourcetable=person -writeEmptyResults=false -append=true");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			assertTrue(f.exists());

			in = new BufferedReader(new FileReader(f));
			List<String> lines2 = FileUtil.getLines(in);
			assertEquals(lines1, lines2);
		}
		finally
		{
			util.emptyBaseDirectory();
		}
	}

	private WbConnection prepareDatabase()
		throws SQLException, ClassNotFoundException
	{
		util.emptyBaseDirectory();
		WbConnection wb = util.getConnection();
		Connection con = wb.getSqlConnection();

		try (Statement stmt = wb.createStatement())
		{
			stmt.executeUpdate("CREATE MEMORY TABLE junit_test (nr integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("create index idx_fname on junit_test (firstname)");

			PreparedStatement pstmt = con.prepareStatement("insert into junit_test (nr, firstname, lastname) values (?,?,?)");
			for (int i=0; i < rowcount; i ++)
			{
				pstmt.setInt(1, i);
				pstmt.setString(2, "FirstName" + i);
				pstmt.setString(3, "LastName" + i);
				pstmt.executeUpdate();
			}
			con.commit();

			stmt.executeUpdate("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100))");
			pstmt = con.prepareStatement("insert into person (nr, firstname, lastname) values (?,?,?)");
			for (int i=0; i < rowcount; i ++)
			{
				pstmt.setInt(1, i);
				pstmt.setString(2, "FirstName" + i);
				pstmt.setString(3, "LastName" + i);
				pstmt.executeUpdate();
			}

			stmt.executeUpdate("CREATE MEMORY TABLE BLOB_TEST (NR INTEGER NOT NULL PRIMARY KEY,DATA BINARY)");
			stmt.executeUpdate("INSERT INTO BLOB_TEST VALUES (1,'ffd8ffe000104a46494600010101012c012c0000ffdb004300080606070605080707070909080a0c140d0c0b0b0c1912130f141d1a1f1e1d1a1c1c20242e2720222c231c1c2837292c30313434341f27393d38323c2e333432ffdb0043010909090c0b0c180d0d1832211c213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232ffc0001108013801ce03012200021101031101ffc4001f0000010501010101010100000000000000000102030405060708090a0bffc400b5100002010303020403050504040000017d01020300041105122131410613516107227114328191a1082342b1c11552d1f02433627282090a161718191a25262728292a3435363738393a434445464748494a535455565758595a636465666768696a737475767778797a838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae1e2e3e4e5e6e7e8e9eaf1f2f3f4f5f6f7f8f9faffc4001f0100030101010101010101010000000000000102030405060708090a0bffc400b51100020102040403040705040400010277000102031104052131061241510761711322328108144291a1b1c109233352f0156272d10a162434e125f11718191a262728292a35363738393a434445464748494a535455565758595a636465666768696a737475767778797a82838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae2e3e4e5e6e7e8e9eaf2f3f4f5f6f7f8f9faffda000c03010002110311003f00f61d6bc49a47875629358bf86cd25242194e3711ff00ebac81f14bc0e47fc8c765f99ff0ae07f688da34af0f83f756e255c7d545780bb1248dd9142572e10e63ebcff85a3e07ff00a18ecbfefa3ffc4d27fc2d0f04ff00d0c563ff007d1af910ef43feb33f46a45418ff000a48d550b9f5e7fc2d0f057fd0c763ff007d1a3fe168f82873ff0009158feb5f23ed5fee0fccd3fe6ecb17e9fe354d25b95ec0faedfe22784523476f10598490650e4fcdfa53a3f885e15906135db5627a726be4886662c22763b41f950f415d069cc378f4ac6a49c56811c327b9f545bf89348ba5dd0ea10b8f63569753b16e45d478c7ad78be80e444a4007238aed6ce50613fa566aab7b933a0a3b1da477f6b27dd9d0fe3527da21233e6ad61dadbed8b247279a94fc878d9ff0002ad3da6867ec88350f1d786f47be6b4d4358b7b79d00263724100f43d3be7f4aac3e27782c71ff091d97ebfe15c5fc58f06beb1a52eb1616e64beb418755fbd2447b63b9079fcebc23681c7047f7b6f4ffebe69a95cda14148fadad7c75e19bee6db5ab6933d949ad48f57d3a50c52f23603d0d7ca3e1fd525b7952dadad65924279c7007d6bd66c6f7555881d96b196c70fbcff2c50e7622542ccf5a5bcb576c2cca7f1a9448b27dd93f5af37b5d4f53599165b8b6463d92227f99ae8e19b51043fdb6303a9fdc0ff1a5ce43a6d1d46319c1f9eb375ad7b4cd02d56eb55bd8acedda411877e85b04e3f206a08af2ebc876796076519fba471faff235e3bf1abc4f16a305968a6231dc413f9f2a160ca7e5c2907aff0011ec3a5353b8e34dc9d91e9dff000b3fc15ff431597e669a7e26f828ff00ccc5647f135f2ac842e0ed6e7d6a36620fdcc7d6aeed9bac29f589f89fe0b231ff00092597e67fc297fe167782cffccc963ff7d1af93bcc4feeb7e43fc698db0e7112e692b87d58fac8fc50f068002f88ec0f23ab91c77ed527fc2d2f057fd0c767ff7d1ff000af91d64601f27e6ec29329fdcff00c73ffaf4297987d5cfae3fe168f82bfe863b3ffbe8ff00855dd1fc67e1ef105f359e93aa41773aa19196324e172067f322be3a2ca57fd5afe15ea9f003fe47bbb239274e7ffd18955b2d4ce74524d9f4a6e346e34df31ffbbfa537cc6f5a673f32311bc71e115254f8a345047041bf8bff008aa43e3af087fd0d5a1ffe0c62ff00e2abe2ebae6f6e0ffd3463fad41b01ed53734e4dac7db3ff0009d783ff00e86bd0ff00f06117ff001547fc275e0eff00a1ab43ff00c1845ffc557c505714ee1a4e507d168e61b833ed61e3af079e9e29d13ff06117ff00154f8bc5de1c9b88bc41a5b7fbb7b19feb5f18c01848096dc845773a0c462f2f80ad27247a7b54ca561c69dcfa8175cd2241f26a764dfeedc21feb520d574e3d2f6dcffbb32ff8d7936970008a5b04d6e5bdbb862ca148c74358fb6f22fd8f99dd4bac69b101befed573eb328feb59f2f8bb448a7684df8675ebe523483a67aa822bcfb5f9a24458a4011f19563c107fc2b905d6d1656f9b0e320827918aa556fd03d8799ee43c5da2bfddba73f4b697ff0089aa979f103c2fa795179ab476ecfca89a3742df4c8af29b1d6d279154900d6c5fdad878834f5b2bd804b1c990871f327b83da9fb5d7617b0f33b5ff0085a5e06ffa18acbf33fe14eff85a7e08ff00a196cffefa3fe15f34f897c257be18bc0936c92de4188e70383ec7deb1554e391fa568a770f647d5e3e27f829c63fe123b004fac98a78f889e0e9393e26d2bf1b851fcebe4d2873f328fad30c5b86c4186230314b9989d23eb81f103c1d8c7fc251a40ff00b7c4ff001a53e3ff000728cffc251a413ed7887fad7c976da4cd0e0dfdbc8622776719ae974ad37c3734b18924b666c8cc6edb7f434eebb99fb37d8fa30fc43f06e47fc54fa567febed29cbf10bc1a7fe667d2bf1bb4ff001af34b4f0178575554692c22653d5e2908fe46a6baf811e1ebd889b1bdbab371d09224427e879a39913cad743d066f88fe0e8515a4f126998270025c2b91f50a4d41ff000b47c13838f12d88f6c935e27ab7c06f13580696c6e2cb51894121558c6e7d802319fc6bccb51b1b8d2ef26b2bdb6682e2272af1b8c3034d7909ea7d75ff000b43c13927fe124b1e7fda3fe149ff000b3fc1007fc8c7618e9f7cff00857c715a3126205fb84704e450c7185cfadffe1687823fe865b2fccd07e28f82431ff8a92cb1ec4ff857c8eeeb9c1fde7b9a8d864e0aaafd050fd0d151ee7d787e29f81fbf892c7ff1eff0aeb6c6e61bcb58ae6ddf743346b221c11952320f3ed5f0a328da7819afb73c29ff0022a68fff005e307fe8b5aa6ac44a9f29e4bfb45ffc81f42ffaf997ff004115e001323ad7d01fb4582da46843bfda65ff00d0457818e9445268e9a0af122dbcd4cadb4e41a140269c147a524ac74a85d088accd85c8352419593a6723ad2285dc01e0549121328041e286cbe4d060daa410b91e86ba1d2c332fb8e6b0c47fbd0bdb3fd2b73450cf7222eec3ad635c24acd1e8fe186df00078c715de69516ec03f75793fed5709a0a8590469ce3a9af47d357f740e3d2b08ea73d5343cf237038181c7d2aacf7411739047422894ba925b1b3d4f6f7ac4d5755b5b5679cee554f99b68dc197e9d455bd0ca2aece8a0b80e40ce7773cfeb5e31f123c01269d772eb7a544d258cadbee224193131eac075dbfd6bbfd3bc4567718114ebc739cff2adc82f12724336f0c39f7a148d629c19e3de06b0482dd6e59417930cd9e7f23dabd24cf0b4586009cf07d2a3bdf0ec304ef3e9aaa8ac773c23a0f715c9eb77f3584b1b11840e33f4350dbd596ad2675aa913cea7ef11c8ada82f038db922b82d335613cdbc93f37039aeb6c5c1895b24ff004a69dcce71b1bb0bbf96f9f9b3debe76f88eec3c79a8231180b18dbb73c6d15f47d8a877c01918af2af8d1e1c4416faf5b2edc110cf81d47f09fe95a4340a324a76678e795fbdddbb8f4a8a488f43e52fd38ab98ebde90ae6b4b9d9b14d62225c1ec334b2c458e47618c66ad9000a6e011c53727725a77b99fe49fee37e469bb3af26a629f9d23aed722b6d0a70206001c0af57f80c31e3dbdff00b07bff00e8c5af318d772ba1af50f80c07fc2757b9ff00a07bff00e8c5a86f5396bc7dd68fa2a8a28a93cc3e19bb5db73707d646ff00d08ff855751c66adddaefba9b1ff003d1bff00427a851371c019a76b9d74d6971bb6a445461c44c7e86a6f217663bfaff4c548ab82a49fba3d3e94492e853698d899bed1f77e5515e95e11b24ba823b8908c9e715e7b6d0b344d273b4b679f4aec3c33aa8b49044cd80df7456735645c1599ea7a7a80c63c8e0d74016e56d736cb031ce30e48cfe35c5596a23cdce46480735d9d85d79d12fcd818e40f4ae54b52da3cff00c51e3cd0ade7b9d2f52b591ae606d922052403d461abc966d604fa933da87580b642c8d935d97c5cd0561f1541776cbfbcbd8cb4c0f40eb8191f8115e7b05bb4570d1905b1c9c64574c546c649cae75f657122ca87eeee1c31e9f4af43f0e5c4864549475017e6ae1fc3a22d434f6b063b5dbfd5961cab751fe1f8d745a26a0c192dee7e4b88ced65f4359c91b2d8ebbc67a526b7e1ab9b5dcbe66ddf10ff6c0e2be7f64208ca956c9c83d41e33fe7dabea2d362b254585d04923a83b98e463d6bc8fe287850e91acff6adadb91697873215c6d8e4eff81ebf8514e5d06b73cf15370e3bd4fa6f946ed666c6d538e6a23c0c039cf0314acbe4da7cbc1239ad5a33a87736b756d78020c003a735bfa769362086304523bf5dc80ff3af30f0c4f24f7ca8f9da0f6af5bd313e5c939ace5743495ae5883c3f6b0ce0da43f6793a936ec507e86bb0b437fa6d9a98af2495f19db30de3f13d7f5aafa54064224083157ee8860060038c0159733b89c6e575f1eda593245ad20b3776dab3236f889009ebd4138f7fad7ccdaedf8d6bc457faa15c8ba9ddd0b0e8b9e07e55de7c52bcdf7367a72be766659003c67a0ff1af3e2a08e95d117a12a8a45568a355c88d49ed8507fa52ff00cb3cf963fddc54c57238c669857e5f5ab530e5b10793fbedff00c39ce3bd12c25f9e8c2ac743514b11939070dfce88eba3114ca175723b57db7e16ff00915347ff00af187ff405af8b1d0a42d93fc200fcabed3f0b7fc8aba47fd78c1ffa00a7133acee7947ed0cbe6695a181ff3f32ffe822bc00293dabe80fda04e74dd08ff00d3d4bffa00af0351815515a9d3875788a38a503352f967007978fc7ad385bb0c0c8a6e4ac75c6d712240afb99947e34443320ce3d4559540aa00cd2a2ed403ad63cc527b8bc8c71f5ab965235b5c0703b1155986460715a1e583146c31c73f8d4cde844a27aa7863c892ce29987ef0ad77ba490f6dc11d7b8af1af0b6aac89f662d8dbc8af4bf0f6a5b82a310549e6b18bb3396a47a9a1e2bf11278574e5bfbab669edf7843e4e372124e3a9e95e61adf8ae3f1948b69a568e6198f59e670b8fc14f35e87f1174f3aaf826fd048c0c2a2e073d4a9ce2bc1ac2f25b65516ee55a43f7c7615a3d8ba315cadf52caa6aba55ecd1cb13ee84fcec9c85fc476aedb41f109b942aadfbcc7ca09eb583a4bbd9484ccc0a4c3e7dfdfd7afb573d657cd6baabb4590a18951e83351bec6ad268f7fd2ef0ce1b7a619b18f6c5719e3cb40b6970c4025555860d6e78635d82fe38d1d9565200383cd749aef86edb5ad125b65c098a921fd491eb4d2be87226e12d4f14d0ef01753bc02bcd7a4e9d7825b700f031dabc69d6e744d5ee2c2ea3f2e681ca32918e41e08f6c735df787b5512dba8dff5a5cae3a9acad2573d4b4cb81e58503da97c4fa445af68577a74a06d994aa363eeb7506b134bbf0255c9e73815d4db4e278b6f03a62a9339dfbb2b9f265c5acd69772db5c4463962628ea7b38241fe548dc0cd7a7fc5ef0ba59de26bd6ab8170c12e54740d8f95bf100fe55e5e5739ad0f469cf9e37434a91494f233d714d29f3645171b8f62168559b71cfe74e68d59707a548471914d6e955713b94d32049b7d3826bd47e040ff8af2f7a7fc78bff00e8c5af373180ac33c1af49f813c78f2f0ffd383ffe8d5aa4f531aff0bb1f43d28ea290f5a51d4523c8ea7c4132ff00a64c4f694e3fefb351bc4c473e50fa5589541ba9baf1231ffc7cd36340a0819a6e76763b62ac84840f2f191ef8a9318f6a5c0f4a5c1cf4a4f577432de9f728b1340ebb467009a7b16b7977210547435500da700548092300f0696fb9a5ac75ba26b81cc609181c119af4af0fea2adb72de9c5786432341206438aeefc3daca829b88dd9c1e6b19c5741b67a7eb9a159f88f4c10ce804880f972775af33b4f0cae9f14b14833246ec8c4f7e78fd2bd0f4ed5c6e8c92307834dd4f49b8b979a7b648f6b73c9eb59a6d682b1e74b6e2c643310a00ef815cb5c6ab349ab4d7719c6f60403edc56cf8aa6bdb59fec7343244b8c976e87d81ae5b6804641c1e6b78abad4be5b2b9e93a2fc46b78a2b75d418472460a8623ef2fa7156fc5ff1274bd67c3d75a6da5b9792750be611855391c8fcabc6639cdc5dbca5d7393b573d055912719c7eb5d7470f07ab38a759ded12e46b149323b9c2ae70168bc5132158ce09ee6aa89180f40381da9c1c91935d4a8d36cc9d49bdcb5e1757b4d4c2caa3938e95ec9a3c0af1a37535e316f3b4732caa70ca73c57b5f87268e482094e3e75071fceb87174f93546d46a369a676da7425210580e9d2ab5ff00041c75ad389710ae3f885655f67705af3d6e7423c1bc76e64f195f648017681ff7c8ae67db1c5745e31cb78bf52c8e9201f86d1580540aeb8ec696d08f60f7a6f6c7352b023b7e748c87ef103068d3b02f32391738a4a79c7e14847a55f32ea6528be8412826271eaa6becef0bff00c8afa47fd78c1ffa00af8ce55c444fb57d9be18ff915b49ffaf287ff004014d230aab44795fc7f1ff12ed0bfebe653ff008e8af08971f77bf7ff003f957bb7c7e01b4ed10374fb4c9ffa08af0b67890796ccaadd0e7b1a5d4e9c372a826c910ab74eb4220c7210e4ff000d2aa3100213f2f193d1aa53c62a5b3b2c92ba13ad3f18a5c628a9b8e31b0d04155c0fc2ad5bcc1008db1b49a8293f0a4f529ab97e3bb7b4ba5656c2923915e8be1ad5436cdcc307debcc47ef63d8c3a74ad8d1751366fb5bb7159c958e79c753e88b478afaccc5328759176b29e854f5af17f117835f43d5ae678d956d623ba2523aa1e7ffad5e81e1dd6c4b6abb181040efd2b53c41a241e228504cee2229cb27d78a2f7473c64e12380d13484d4acd26b88b7bb7207615cc78b3455b0f1045158aa64c619c06e95eb7a7e9ada3db18e421d230364a070c3dfdebc47c6dac35cf89ee5627f918ed27d40ff00ebd5528734920955b5d8e3aaff0065ccaf2dc0b7997b4677e7ea074aedf45f8c91476ca97303bc8836ee58f1bfd3dabc995233c9193d726a5181d2bd3a5828ad5b38ea622eec6d78af5a6f13eb571a84b1180bedc0539c01d3f1a5f0edecb0dcfd9e47da5c81191d18fa5628618a5203214ddb4e386f43eb5bd6c2c250b448857945ea7a42f889ecd30ebb6446c609e73eb5e85e1fd712e628d91874e79ef5e1faadddd5c68c8f731a2bc6724e705863839e87e95a1e06d7655ba4469386e80fd2bc4941c7e4773b4923dabc556235af08ea168465da0253bfcca370fd462be6c2bc8c74da3fefac8afa8346985ddb05c83c57cf9e29d11f40f115e583025124dd11ebba363953fafe95507737c33b3e56619423b71ed4de952146ddd3ad34a9f5a67672b1a0834dd8be94e2303b5211c77a04d1115e1f38e7d3aff9eb5e97f035157c75758ff9f07e3feda2579ce074af4af82031e37baffaf17ffd1895499cf88f819eff004a3a8a43d6947515478bd4f89e5e2e661cff00ac6fa7de3408c639eb52c8a3cf95863fd637fe8469a3ad4cb7b9e8c63a0981fdea72afddfe942e73cd3f803914257d4ab25b08ab92d4e00628033c8e829e30bd29ec3516c15411d79a9a1b8920712215057d2a32bcd1bbe5c7e34ac8b715d0f41f0ff88616b50b31c374eb5d9e99e228dd1519811d39af0e49190fcae40f4abd6bacdddab7cacac3d0d66e04f2b3d8f558b4dd66dfc9b85460791ed5e73e22f09cb011369611c6306324f22a947e2cba47c3c185ff0065aaf5bf8b99a65de1914f038cfe7495d10d3b1b9f0a7c1be1ed7345befed9d315f50b39b12ef770483c838ce3d4647a56adef803c3e266316988aa4fca1647e9f9d53f0cf8c2cceb50db24cbba55752c7e50c073f31fc2bb07d46da5da41420f3c4ab5db4e5268e19a49d8e324f01e8ab964b1c1f5f31bfc6b3ee3c1da7c6095b4231fedb7f8d7a009a16046d43ff006d053648e170731a9cff00b757cd222c8f29d5746d3adb43bbb8861f2ee604dea43920e08cf53e99adbf08ea9e6c517628c5003ec71fd2b4bc59a5da3787efc8468e4f21f0438c1e3a5711e06bb0220a5c93d5bea7358d66dc6ccda8a5cda1f41e9f7a278429c6556a9ea4e164196ed599a03b6d27391b69dab5ca0b85ddc2aa966e7b0ae04b53aac78df8dc67c5da81f5607ff1d15ce819ad2d6af3fb4b5abbbbe82490e00f41c550038eb5d2b6375a218c37f6a6941ef9f6a908ef9a40081ef4ee0d26438a4a969857e9fcaaae438db72361853f4afb1fc31ff22b693ff5e50ffe802be3a61c1c8afb17c31ff22be93ff5e50ffe8029c4e5afb1e57f1f3034dd109e82e25ffd04578e780ecacb59f1d58d8ea219edae7cc46cf6ca9c7f4fcebd7ff6867f2fc3fa43719371201f8a8af1ef87b750d9f8e34a9ae24088b21f98ff00788c0fd4d3b1929da2906a7a749a26bd7da44d92f6d332a93dd3a83f963f3a8ca823a5769f18ecd2dbc4565acc4a3cab98f6311dc8ae300ca923a019ace5b9e9e1ea7346cc0734b40e69db7e6f6cfad49d36176fba7e74794dea3f3a3610cbef4f281f9c7e22aac55ae300c38dbf9d4c1893bf272298abcf18a5c9460c3a7b5459c899c22d1d5f86bc406c54c727287a135eafe1df1043736e232e30d815f3ea87f3778703d856f695e237d3c85627683d735128d8e69d1bab9ebbe348f514f0f5d5e68c8d34b17df83ae57d40f515f37eab3b49748f202b3a656443c10739e73d2bdebc39e338ee19448e0838e1aa1f889e00b3f16597f6a69bb20d4e34273b789971f74e3bfa55d2959ea714e0f95a6785c6c4a8f9b9a9370cfbfbd518e478e431b2ed604ab2ff00748edfa1ab40a95c839af6a9cd348e3b13ab1cf4a915b8cd55f3001eff004a72bb6f0322b652b90d1e83a2e989e21f0d3a339568098d8c4dc918ee0f15c5da4cfe1fd6e681d8e226c8279e2bb1f8757891ea17904a4959a1dcbb467918ac1f13a23f89ee1d01da304e462bc8c5c7926d773bf0def451ecfe08d5db50890adb4889c0ded803eb581f1934c882d8ea8a3136e36edee0fcc3f2ad0f87f72268a28b207cb58ff17751df7b65a6e4650199c03d09181fd6b96074534fda9e67b453644523278a51c9e94a5467dab567a441b7e4cd21041c77a98003b2fe148c9d8d662b10b77af4af82231e37bacf6b07ff00d1895e6ee39af4af8263fe2b4ba3ff004e2fff00a312a9239f13f033df0f534abde9b4e5ef567871dcf8b65ff8f99c7fd347ff00d08d20ef4e9815b897fdf6ff00d08d314fb734e5b9e8c761db7d3fc69db194d2a8dbf5a51c9a9b9aa80bfe34a0601f6a08e0118a553ce3e5fc7a54941c7a9a5dc3d699453b05891460fa530823ad387273cd387142dc571b8ef8e454d6d124b731ac87119eb51295c827a538452b80618dddba828a4d244cb6327c416f0daeb12456ed98b01b23dfad5359a45e92c85fa9e4d13a15b925d8e727ef0e7f1a6c2dbe7488ae379dbff7d575c5da3a9e5cd3721ff699d1995a56cedeef5661bc9c9c24ee147f10635eb1a6785fc39a75be9d1ca8ad24d70b14ad35bb6555b233bf843ffebac1f8a5e06d37c1da85a0d2ae27960bb0cce92306209f4c01c53a55b9df2d8254ec70b3c974c8d99e620f18de7fbd5b3e0495c6a262ede9f53592a4b215c15f7ae87e1cdb4926a6d2792cd19c0dfdb3538956572e85b98f7bf0fc18b205b1922b8df881a83d8da4c1180790f9487dbbd773a72f916c067000edee2bc93e225d19755b7b70df2c6ace41eec5b1ff00b2feb5e6c3591dcb738b503cc182686fbf1fe14a1738fe165a6ffcb5fc7fad74a375b015f947a52118ea3ad2e4d358e314890ebd45348e30c29547cc314addbd68191c83e43d2bec1f0c7fc8afa4ff00d7943ffa00af901972b9afaffc31ff0022be93ff005e50ff00e802ae272624f20fda354be8ba1ae7e637527fe822bc53c3112a788ec1ee32d10950903af506bdaff6897ce97a12275171291ff7c8af1ad22da49752b231b10c275395fa8eb5a1c4b73da7e2469b06ade0d2b10769a121c74c00393fa578e5801341b564c94ec6bdde78e593459518230f2b6935f3f7d9e5d3f5399376d4463bbe953257475d19f2491a2c36b918c11fdea6a9ed8ab0479f073cbaf39f5151a852036dacec7ad077d476d342b647cbcd3f34668b9a5c616c8a4a50714ec7cd9aa0d85ed41a28a910eb5f3d651e4127b90a6bd0fc3be377d2e68e2bb6291e7f791cc3047be6bcfecafdac2e566540e4755f5af424f07c9e32b18ee2f9560545ca451e3f327ae6b27a491c75d58f27f18dcdadd78c355b8b2dbf66925ddb872325474fc73546262c81860d745e30f01cde1f983c4e1e395b08bb7e63f4151695e12d66fb67976aa848ce6625457a146ac231d4f2a74e57325509032b4e2c10ee38c7a03cd767a8f8064d2ad216bbb9696ea66db1c312e067d49f4ae97c31f0dadc8492f40773cf3dab4962e11d82341eec7fc2bd16e2382ef59b88ca45e598e12cb82c4f53835c678a2512f893506f94859367d702bdc2ea33e1dd1ee25697cdb68a262dbb19181c57cff75319ef279cff00cb472ff99ae3a953dacae7a184a5cb7ec77be04d5112744276b0c6306ab7c56453e2e86751f2cd668f9f7dcc2b8eb4bc9aca71344f861cd5ad675cb9d72ea29ee140f2e311ae3d3fc9358c5599d31a6d4f98cfc9278e9437dda31ec294e6b43a0691c0a6d2939a77e14c7b11baf61d857a3fc13ff91daf7febc9bff462579d1fbf5e8ff05571e34bac74fb13ff00e8c4a5d4e7c4ff000d9ef14a3a8a4a51d4533c147c653f3732fcb8c3b7fe8469aa39e78cd4b30cddcdff005d1bff004234dddcd27b9ea4237429c13d6818cf14a073c521c66a4d451cff0077f2a70c03480f231f8628dc3d6810ceb522d276c7bd2fd29dc1ea27f09a5a45dd8c738a0f0052b00e5c071e99e6bd4fc291d9c964822281b1d315e54b9cf356adef6f2d087b69da3719c60d0d58992333c572c7278b2f4c68028948c8aca843a48af1e55861d58750568b97912f6496e06e76ce4fbd4a37b22903e56edb79aed8a52563cb9a6a4cd69f5dd6aff4f5b1bbd52ee6b6dfbfca794b26ece738e9d6a5bbd6750d49e36d46f1eede35da8d2632071dff000ac60eea768fbbeca7f9d3a393042ed3f8d6b1a715b19b9bb58b72b1685b1c60d7a5fc34b5dba35bb0407792dfad797cb288e255e84f7fc6bd77e14b1974ab74182550803f3ff1ae5c63d2c8e8c3abdcecf53bf8ac6dda467200405b1d80af0dd4ef5b50d4ee2edb3891c95c9ce07615de7c4b9a5b0b986c526dc278f738e98c1c1af3a2a08e2b929c7a9e8423a5c7531fad3e99ff007cd6e8d6436931f77269c1b6f61f8d275fee9a52210c031cd0464e69d8a315366171b8c035f5e7867fe459d2bfebca1ffd0057c867a1afaf3c33ff0022ce95ff005e50ff00e802aa07262ba1e33fb4092d69a39cf4b89867fe002b82f8710c5a86b12c040df19c807a75aeff00f6854d967a23608437129ffc745797f836e134fd7ad6e1a461e6b88ce0f626b4e87227a9f435b59edb778a411a893a9af19f8a1e1e934ebbfed280c6f1dc0dae1074e98af5f92f348b4842dc6a0a9263386957007e95c178e2f745b9d2a558b504924da768f30119fce9366914f73ceb4b944b0a73f30e1b14e23692bd893597a4c8b05c3ee65653d7eb5aa39c9239acada9eae1e5cd1f3117a76fc29cbf7a929d8e9cd51d83a9bebcfe34ea6eef6a44a1339e334ec02293ff0042a5e84d0308dc47324b8c85607f2af54d0fc7fa65869a44a434bb70100ea6bca01c639ce7d2819126e18ddeb5138dcce74d4d1eeda0dac3ac5db6a17d1a492bf29b864460f4c7a5696b5716fa55a07f2d4ccec23881f5f5af23d13c757ba3c617ca59001c64ff003aabadf8d355d66fe0ba9da24580e638a3fbb9f7ef9a8e56717b19395ba1ec1a6e96f704ddcfb659cf258f6f615ab235b69d034d2b242b1af2edc28fad798587c5216167e53594924806382319ae57c43e2dd47c44fb6790476c0fc90c64e3fe05eb4d443d836edd0dbf1cf8dc6b44d858311641b2f20e3ccc1e9f4cfe75c30c1ffd04d28180c39c50003c1ad231b1db4e0a31b2176f149834ecf1f787d69a32c093f8532ee3b3c1e681d7b7e1483a9a51d7b7e140c4fe2fc694f3d3149fc5f8d3bf0a04348c935e8df05bfe475baffaf17ffd1895e72c700d7a3fc17e3c69738ff9f07ffd1894ba98621fb8cf77a72f7a6d397bd533c28ee7c6d71febe5ff007dbff4234d634fb85ff48971ff003d18fea6983dfa54bdcf561f0880669f40e94734995b8d1d7de9a78a50714b9f933ef4c62e7e514ca2a51d4fe3436026ecb03c52e79a4c700e3b514af61317d28cf403b518a1783ba9ea08d0d3bc34bafbed96728aa70b81cd6d49f0aee204cc3ac30079c32d73d6b793d949beda4646f6e6b53fe12cd67660cc0fb94155792d998ce8a93b91cfe05d461048d54b11fec5529bc35790a64ea248039f907153cdae6a338f9e765cf5217154dae279321e6761f5a71a93ee47d5e3d4cf1a6b35e219ee4cd1a9e73c62bbef016b90e977e207c2a331db9e84571ab81c601cd38e430c3631fdda9a8dbdcd61454763adf88fa8477de265314ab22a403241c8049248fe55c9938eb41cb2e1b1f3530c6c40e94a3a2365a03f5a6d3ca67bd32b4264b517f83f868fe0fe1a4a2958570a29ce3bd3587d28b8dab098f973eb5f5d7867fe459d2bfebca1ff00d0057c8bfc06bebaf0cffc8b3a57fd7943ff00a00a23b1c98ae8790fed1602e91a117c6dfb54b9ff00be45790786b479f55b874198ed6360cd2ab6187b0f7af5dfda3b2344d0f1ff003f52ff00e822bcebc1fa80b5d2d95b692ce4d151b8aba30a34f9e4773a4691a458465230ccc79679006627be49adbfb2d8cb19416d148a4721a3073f862b99b1be46979001eb8ae9f4fb857c60018ee2b0536f73a9d3e5395f147802cef2c64bed1a05b6be87e630c67e4940e7a7f7be9c5702a77a7208cf507a8faff2af7a75720346707d6bcbbc65a30b0d47edb0a910dd31dc31c2bf5fd7ad5266f86928c8e6d7ad251de940c2fddad0f404a5ef8cd039c6083413c76140005c8eb4a5b8a407279a53f7b8a040471c1e29a339ebc76f6a5cee181f98a4d99707d38a403b3ef46ef6a42b81d6946476a0340215860d1d87a8a377b503a50160c12297af4341e5b6fb66a19a51126e2700504b9244779769689b9b927a0ace5bfbb7732301e5a603ed1c0c9aa37170f3caf28dacabd9aba5d1c58af80fc4535d090c8ef1476eca380d9c8fe46a923cca9896e7eebd10aadb8679c6453fb6471556d1cb5ac4ddcafeb564364f4a4cf4a0f9a298b83fecd37f8bf1a3f0a5ea78a0b1703a57a37c18c1f19dd7fd78bffe8c4af386fbadf4af46f82dff0023addffd78bffe8c4a9ea73e23f86cf78a55eb494abf7aa8f056e7c73703fd2663fedb7f334c6fbb524e337127fbedfccd46454bdcf5a1b21a01349525263774a572ee328abf6ba5df5ee3ecb6af2fa90302b4d7c1be2064cae96edf461fe345c5cc8c0249e307f3a5e3b74ab5776173a7ccd15edbcb03e3eebaff002e2ab6785e304f7a2c36ee2514981e94bd29087537069d456ae298828c1feed20a5a5a4901237dd351d3bfef9a451934ca96ac72671ed4ea0fdf34522d6c14514e7fbb4036379fef1a4da3d282fb4fbd4b6b65777ac12d209666efb57345c4da216f63f4cd191eabf956ec3e0bf114e3e4d2dfa7f13a8feb515d783fc41650bcb2e9371b5464b261b1f91a8e66473a31297f83f869caa09da7191d723069adc2e32bf81abb85847eadf535f5cf867fe459d2bfebca1ffd0057c8c06476afae7c33ff0022ce95ff005e50ff00e8029c7639313d0f23fda2a269b41d10a8e16e2563ff007c8af11d1eefcacc0c40e72a7debde7e3bff00c83345ee3ed12f1ff0115f3fdf5a9b6759a2fbac73c7f09f4a4ddf40a316973a3adb2bf68e5ebc70326bb8d1ef90a2906bcaec6f04d18047ef17ef0adfd3f527b76037607bd60e36677f2aa91ba3d82d6e55c0191cd3755d26d758d3e4b79d72ae3048ea08ee3deb8bd3bc471960a5c6315d4d9eac9328c3fbd099cbc928bb9e57ae68375a0df35bdc2e6323314cbf75c7b7f85671e471c8f6af61d661b4d5b4f92cee1e36ca9f29b1cc6dd8fe75e56da1ea82678c594adb4e010060f356a47652a8dad4a5d38c7148d8033c7b55ff00ec5bc0db664587d4bb0c8fc3357e1b0b1b43be606761ebd3f2a1cd23473b18d6f697178c7c8824930392abc0ad4b1f0ec93b6eba9d605f41cb55f37924abe544162887454e3f3abd64a46395c9ea6b375089542d58f83f46b83b4fdaa438e5b7815ae3e17e9574a04375770b9e98c37e86b47458970a4f26bb0b240547039a71936724eb493d0f1cd6fe196b9a6c666b555be894ff00cb1521f1eea7fa135c4b028ccb92197860782a7d3dbe86beb285015c648e3923ad713e39f8796dafda497d6091c1aa28c82bc2cc7fbadfd0f5ad1053c53bda4782e73f301467e5a92e2da4b29e5b5b9478ee22251d241ca906a3000183547a11926ae0cd8ce4f41587aa5eab111a9e075abd7f742188852b9f6233582ff36e6907dfe540a71387175f9744468d88cefe5738af5dd1349593e06eaa4c196653386c679520823f235e4c91abb221385720337a57bedbea966df0d2ff004eb375012c9d768fa1cd5a47991573c7f4b60d6601fe1622b440c0ac7d164fdcbc79e7767f4ad64276d433dbc3caf042669db79f6a41d452a939a0e9109c03f4af46f82a7fe2b3ba3ff4e2ff00fa312bce0fde35e8ff0005b3ff0009add723fe3c5fff0046254adce6c4fc0fd0f79a55fbd494abf7aa8f056e7c7771feba4ff7dff99a8ea49f89a6ff00ae8dfccd46abbd89fd6948f5e2b41464b00bd4d6ce93a589640642338ce2a9e9f10dc646e83a5743a2806566f5ac652636b43aed1ad9624540063d05761688b80368fc05733a790029ae9eca45e2926dee612658bbd2ed750b6686e605951860861fc8f6af2af157c3a7b057bcd237cb6eb92d0b72ea3d8f715ece80491f1d454535b961db1fa551319b4cf970641e7f2a515e89f10fc23f6395b58b087111ff008f88c0fba7fbc07a579e7438c03ee2ae3b9d09dd051450064815a3600386008ef452ff000ad2e3e5fbcb49683b0da77fdf348dd3b7e3d692986c2e5ffd8a7e47a8a6ff00df34a9d282d31532c28dc19801f3538100f26afe8d6627bcc91f28350dd84dd8bfa2f87dae5926bb19563c257a2e95610c48ab1c6a00eca3154b4cb4daa091d0f15d3d95bed230bc566ce794ae6959db2803728fc456dc16ea00200fa567daa118ad88091d285b98ca4d1ca78bfe1de9be24b4699156db5055fdddc280031f4603a8fd6be7cbfb09f4dd426b0bb8cc73c0c5644f7f5fa7a7e35f5bb00c0eec6307a578f7c64d047956dae471a6f8cf937242f2c0fdd27e9d2b41d1abad99e42dc026beb7f0cffc8b3a57fd7943ff00a00af91df924646dafae7c33ff0022c695ff005e50ff00e802ad0f16eed1e57f1e4ffc4b7443dc5cc9ff00a00af147532c6cadd19715ed3f1f38d3f431dbed32ff00e822bc5a3e56a646f858a95331e073697b827183b49f6adcdc78c7a75ac9d562c14980c6ee0d68dac9e7da238ebd0d29772e8c9c66e0cb704ec8dcb568aeb13c40047edeb59008030453b209fad66d1d5c8a46d26b73b3077969ff00db13cdf28660bdce6b0c72b8ab49f280077a56b89d348d16b966ef93eb4e4cfde279a8205c2e78a98e781c5672ec4a4588577b13902b62cc7ccbc56444547dedb5afa7b64afbd4913d8ec349ea2baeb32028ae474ae58575566d95c715b238aa1bf011b055865f91b81d39e3ad52b77fbb57c1dbb76d5adce76ac79d7c41f012ebdb6fec9960bb8f891c0cf989f4f5ae7b49f871a66d5376d25c3ff7598a8fc863f99af6271f2f4047d2b225b210cc6440006ea0512ba37857972dae72f17c34f0a4e845c6890bf1d43b03f9835c178c3e08186296ff00c30f238c64d8c8727fe00fd0fd0fe75ee16c9c7181565a3f97919e29c5b329de5b9f123dbc914ed1cd1b47286dbe511f303d31835d6785ad59f4bd5658a458f642c0a4eadcf073823807d8d7bdf8a7c0fa36a53bdf4ba65bc931e643b30cff009579e6a3e07b3b6b3bcfecb80b432c6c5a01230746c1c1420fcc07f74e7d01ad79c8e4b6c79368effbf9130395eded8adbce00383d2b0b4f568b5278a4528ea0a91d08c7b1fa56e8181d07e3e9499ea60dde014bb7e6c53b68f4a0a8c52b9d972393ef8af49f82807fc26775ff005e4fff00a312bcde4fbadf857a4fc151ff001595d7fd793ffe8c4a5739b11f033dde957ef5252af5a6784b73e3eb8005c4de9e637f3a8e3e87eb4f93fd74dfefb7f334c55e40fef75cd4ccf629688bf0e52dd178dc6b7f481b707d6b0d547c9ec2b7b4ac6075ac2454f63afb26c8c574764dd2b9ab2c704e2ba1b2270bd2889cb23a4b42703e956c8dca47b552b4390b5a0067f2ab5b199977f6714f0bc52a878dd76b29ee2be75f10690fa1eb77360c7211b721f543c83fd3f0afa5e65c8ed5e49f16b4855163a9a2e1b79b7908fcc7f5fce9adcd69bd6c797e79c51d0521cf53dc679a4ddf3fb569766f61c451d052514b98448bf30c123eb9a367bd37ee9edf9d28638e2a9157ee2bf6a54e948fda81f2ae69f41fda1c7a5753e1cb7c1563dce6b958db7ba835dde811e614c0fa5633629bba3b0d3d30a3d8d747683eef4ac3b14c601adfb518231c545ce466ac03ad684230b5421ceded57e2e82ad194f626c719ae6bc71a67f6a785352b5032ed0165faafcc3f95749515ca091429e41e29914dd99f231208e060741fad7d75e19ff91634affaf287ff004015f245c284b891318d8cc3f53fe15f5bf867fe458d2bfebca1ff00d00569137c4bbd8f2bf8fbff0020ed0ffebe65ff00d04578b2f4af69f8f7ff0020fd13febe65ff00d06bc510fca334a474e0ff008688ef2312da4a31c85ddd6a9e9d38591a2202eec639a75cc373148f3c243291ce39acb56742255cef0d9a764d58cead4e5a8a4d6c7452c9b065b34e0d822abc32a5c40ae3bf5c7634f3200796c7bf4a8b1db1a89a4d1663e403572dc6e9074aa313123e5ea6ae5a3e1c138c567246b7ba3508c20c629a396e334d9250aa29b136f07915162116d4e0f5ad6d3ce02f22b149c0eb57ec253b80cd26ee6738dd1dee96795c5757658c5717a549c2d759632640ab89c551599d05b9c28abaae7205675b370315755ba72335a239e44fdaa29230548352c633f7a98ea41ad1eba90a2d10c4bb4e3156874a83bd585195344426569d782319fad71bacc1f63bc2533b5fe6539ef5daba92096ae43c7f73fd9be19b9d4d632e6d70e107719c7f3e6a1a358bb1f3ef8ae2897e205d8b7c053cb85e99c73500fbb54219e5d4354b9bf9b25a672c71ea7918fa55fc866c13d69b7d0f4b0abdd7e648bf74d07af7a44eb413935474c76118e14fcb5e8ff04db3e33bbffaf27ffd1895e727ee57a2fc14ff0091d6effebc5fff0046254f530c4ff0d9ef47a9a72f7a43d4d2af7aa3c28fc47c797076dc4a07fcf56fe66a341965c7ad3ae1bf7f2faef6fe6698872cbf5a891eac764696e190bcf22b7b4ac0418eb5ceab7ce0706b734b3c8e2b199acb5476762dc0ce39ae82d4fddae7ac00201c56fd99e56847233a2b2390bf5ad6539158966dcf15b111f947d2accd8e65c83f4ae33e22d80bcf076a3c7cf0a0994fba91fd335dad647882d7edba25f5b7796de441f52a71421c1d99f30923071c027a7a53029341041c13c8183f5a72e3b56875a7a0d0a734e000a5a2a6e17173467e6f6f5a6e47ad2d36db01dbc93cf4a09dc3069a08a33e94efdc2ec96d90b4ea31c035e8da1465214ae034d50f7099f5af48d2c6d8d718ac9ee29ec74f69fc271cd6eda72464561d97256b7ad7a523999a76e060d5f88616a9c236802ae463031568c66494927f55a5a8e4201c9a644373e4bbeff8ff00bbff00aeadff00a1357d69e19ff91634affaf287ff004015f245f1cdf5d607fcb57ffd08d7d6fe19ff0091634aff00af287ff4015a44debec8f2af8f873a7e87ff005f32ff00e822bc5109cd7b4fc7d60ba6e8649ff97997affba2bc503d12474611da08941e38ac2bc8f1a837a0e6b6b00ae0f4ef59fa940ec04ca30d8c38f4a98e8cbc5439e9dd6e55b4b8fb2dc1ddca375a9aee4dac181c86e95401f33218283dbad2845008639248e2acf3e9e22508f297adb522872ea48fef0ef5af6d718707000619e47f9f6ac2b7f291c3c859b07e45da3e63f9d6cae4423cc3f39033d38eff00d2a6491df84a9295d3341a72533c8c55db23b949cd61f99b701caeee98cd6ae92e1e17e9906b19ab23b1be8689ab76242b67826aa375ed562d5b6c8bd2b121ec767a5c9c2f6e2bacb1638ceeae274e98e57debacb19781d2b58bd0e2a8b53aab56cd684672b58d6b292a0e79ad489db03918ad1339e48b89feae8a629e714fa0ca4c632e39a724854629d51b8da78e95a35604ee2336e2735cf78da013f83b5988e086b29bf3da706b7c9c0ac2f17ca2dfc23ac48d8c0b39067dc8c0fe62a19ad35767cc504420458d71d3ff00d75281b4939a71e46411c5303316e48ab3d984631490ff009b66ec738e94a4e6917ee8fbbf874a052db52d0d3263071c57a47c14ff0091daeffebc5fff004625799e7a7cdf8fa57a67c13e3c6b763fe9c5ff00f462525b9cb88fe1b3decf5340ea283d4d03a8aa3c3ea7c6f39ff4997febab7f33480f4e9d69273fe932ff00d746fe6698adc8fad292d4f520ed1468918e95b5a5b61940ac63f7456a69c7054fb5653357b1dc583602d74168dd3dab98d3d895539ae8acdba62a11cf23a3b4e4e6b6a2ff0056b58963c8adb8beed523263ea1b803e5fad4bfc5f854331c633d07cc7f0a6247cb3abc1f63d6350b7ce3cab978f1ecac455453c66b6fc716cf69e36d5e3718fdf971ee1803fd6b058f6ad2c75c5e84bbbdff4a4ddeffa547bbe5c0a4dc7d68e51f32266f5a6ee351ee34ef30d160e643f77f9c51ba985863814231df8eb4583991b5a347bee071d2bd074dfb8bf5ae13468f0d91c1f5aeef4dc6d5ac5ee2a9b1d3d9f6adfb4c15e7d6b06cfeead6f59ff005a68e77b1af12f4f4ab89c0ed5562ab4bd29c7631a82b1c54333aaa93fc383fcaa56c639aa57b26cb59598f02363f9034d8a07c9f72fbae6761d4c8c7f5afaebc31ff22be93ff5e50ffe802be3e2fbb736473cfe7d6bec1f0cff00c8b1a57fd79c3ffa00ad626b88d91e51fb409034cd04fadc49d3fdd15e2109f94119c57b77ed0adb748d13febbcaa7fef915e1714985e5d3af414495d1be19fb85c1498ce4119cf18a456c8a4dd86c16193daa4e84ee549b4e0ee1e26086ab0d3a73d4a1fa8ad277da33cf3d3eb4e5208c8e148e284d994b0d07ef1560b48e0f99c977f5eb8ab8181e7f888ce2838ea698d205193814f7348a8c5592242e33863573433b44a03eece1b06b2de51e59219722ade8d3ecbdf2c12372f3f515325eeb34524ec744769152c4f86041150038a746df30e9d6b9ac53474ba74bf779aeb2c24240e95c3d8cd82062ba9b09fee8aa89c9551d8d94bc0e462b66071c0ae6ace6e074adab797ee9c8ad11ccd58da562578a915b3f5aa904bc8c1ab6a771c5689d8c9c6e3aa2391f29f5a96a37fbd56f622244df2b76ae27e28df0b5f02de21fbf70f1c23ea5b71fd16bb893e6439af21f8cba8016ba6e9a0fced2b5cb0cf418da33f893f9567d4e9a2af24793138fad21ed4d39c76cd283c8c0aae63d4b0a0f27de8390b938c1a31b4e4f4148cd9e4f14b6434397eefe7fcabd2be099cf8d6ebfebc5ff00f462579a7f7bfcf7af4af825cf8daefdac1fff0046251131c4ff000d9ef87a9a075141ea681d4551e1f53e31b86ff4994fac8dfccd4793da9d7057ed5301ff003d1b8ff811a8c75fa714367a71d91ac0e513e95a7a71e541c565afdc5fa569589031f76b9d9bbd8ec34f90e028238ae92cdcf19ef5c8e9cdf30e6ba8b16e178f6a9473c8ea6c1be51cd6ec5caafd2b9ed3d86457416edba31568ca44a7a8a865f9949f406a56e80d44e720fe03f5a7d093c07e2cdbfd9fc662551c4f6a8c4fb8c8fe40570d5e9bf196d996f749bdfe168de13f50411fccd79883ce38ad226f1d84ddeffad28639e383484a8ec290104f19cd3e62ae8786dbd7146ee31fad34fcb834cc9cd3b7992da449bc74a92dbe66aadbc06e6add827eed4f1c8cd448aa7a9d4690a02a74aed74ffba98ae43484c2ae45765a7ae547158f51d43a3b1fe0fad6eda720d61da2e31c56edb0c2823d6a91833660ebcd59078155613d2ac74c55a319ad418f4e4564eb52f97a35ec87f82de43ff8e9ad363815cff8b25f2bc2babbe718b397ff0040349954d6a7cb31e4403d768afb23c31ff22b693ff5e50ffe802be36438817e83f957d93e18ff00915b49ff00af287ff4015bc475cf25fda20edd1b42ff00af993ff4115e08bcad7bd7ed107fe257a07fd7d49ffa08af02524a673c74c538bd0d30d2b44b4b330e0f39eb5243281f293fa55442738353c2c3711839a1c558ece6f774258ae09e1b393d302909f3f9e460d418ff0065ff00ef9a6bb7cefdb9a5cbd839d0f327dde3a7ad1232b3654605405c9f6a8fcc03a7354ad7d4995444c64014f7f4a75ac9e4df47231e8e326a1789b69e79ede9514a40cbee0413c567257673cb10ae77bb8151c8c1a457c37b552d3ee45cd846f9190369fad4bbb3d3d6b071b3b1db097324cd9b494ef5e462ba8d3a60aa318ae3ada4e41cd745613820566b4667551da594e38e6b76da5ce0f15c9d94a303dab7ed24e00ad13b9c72474303fcc2b42370c0562dbc8081ed5a50be40aa4ccda3455942d31fa5314f715229cd55ac66c427e5e7ad7967c5cf0d9bbd361d62da12d359a88a623ab424f5fa83d7d8d7aab2e38f5aa37b6f15d5b4b6f3206865428ebea08c1fd2836849c6499f270ec320923340c120569788f479740f10ded8480fee64f95c8c6e53ca91ed8c56603cf06868f562d3498714525291d38a4301cd7a67c12ff91ceeff00ebc5ff00f46257990af4df825ff23b5dff00d783ff00e8c4aa898e23f86cf7c3d4d03a8a4a51d4551e1f53e2b9db3773f5e19bbffb4c3fa5443faad170e16e2e09e81d8ffe3e6950e6551c553d11df17b1b638e956ed1b154c9c62acdb31df584d68763d8ea34d93a735d5d9374fad71da731da39aeab4f7c906b2473c8eaec4f0315d15af09f8d73960f8ae8ad89c73568c59631902abcc709cd58c718aad38e869891e63f17e159bc3104eca7305d2827d9811fe15e27965f97807d2be88f1ed8fdbfc1da9c0a32e22f317eaa777f4af9cc3027278278c67d2ae3a32d3260d823da9371c633c526d04e4d1b40ed55c8c4e42d347a50071d29a5c2fff005aa9dee26ec233fef154639fd2b62c530b1a83d3b562c3187bb5c0c8ce0fd6ba2b34c3ae7d6a2a3d0de92ea74fa5a0c29c575d64004c0e2b98d31064015d5da0c274ae70a9b9bd66bb40e47a56edaa65540ac3b4adbb6fba2a918c8d584608a9ea08ced19a797abbd8ca4b511ce2b97f1cbe3c17ad11f7bec727f2ae95cf06b96f1b9dde0fd6476168ff00ca922e28f98d998a373f747feca2becdf0bffc8aba47fd7943ff00a00af8b8b661cf76fd38afb47c2fff0022b693ff005e50ff00e802ba23b13591e47fb457fc82b40ffafb93ff004115e0391bf8e33dbd2bdf7f68aff904f87ffebee4ff00d0457cf884e38cd3e6b0a83b22c75e7ad1cfad420fa1a7eff6abb753a94c9158a1ca9c5349c0a6873ed48305b14596e273ec2b0238dca78f5a595513bf3e9e950b1ce471d29acc3150e5a9937d6e213c9a1a5ff482d8e3a7bd010f9d8cf1d69aaabf2f27767915273b9731d168129f265523ab6e27deb54b0527a556b1b616b67b307711b98fa9a998e540ac656b9ed508da2932ddbca15b15bd6129e31f5ae6a3600d6c58498c1cfeb59cd2be82a88ecec66ced15d0dacbc2f35c7d8cbd315d259cb90288b39248e9ad9f254e6b5207c605605a49c0e456bc0e368c55a660d1af1b6454a18818aa70be40ab60823356999c90a4e40a6b0c8edf8d2d35c6460e3068123c93e30685e6db5aeb91f583f71383d7693f29fcf8af21c6466bea5d734d8757d26eec675052e10a927b1ec7f03cd7cbf796b3595e4f6970a12581cc6f19ec41c1ff003ef48f470b52eacc84b018a09a5a283a80702bd37e090c78d6effebc9fff00462579957a67c1239f1a5d0ffa707ffd18955131c47f0d9ef87a9a0751494a3a8aa3c3ea7c4d27fc7e4fff005d1bff00426a48509bc8075e7e634e90ff00a5cf93c091bff4234b67cdec78e9cfe94e5b1df4f746c6ec9c54d6ed8700556078cfa8a9e0ff005a2b096e769d1e9d21e075aeaec0e429c5723619c02b8aeaec09017a5668c2675762c7d4574b66d9001ae56c8e3078cd74b62d91f855a3091a27a1aab39e82ac64e3b5569f92055128cbbe884d0c89c00e854e7dc62be57b9b76b2bb96d8afcf131465f705abeacb8191f4af9cbc7d6a6d3c71a8865c2cc44cbc75dc013fae6b4a7b968c2cf3d697b73554ccc3d29f34920e8063d7d2ab91df5071b0fec693a8a2a3b8eab428f7324b99d8bb64bb86fc63238addb1562e39acab48c246ab9e2b62c57e6ac6476c15a275ba5afdd2475ae9ed01da2b9cd346556ba5b4076afad423299bb660e0702b5ed87cbdab22d38515b76e38078cd346723493b52e714d4ed8eb4ac7007ad3ea47518fc8eb5cb78cd33e10d5c0ef6927fe824d74aed8ea462b9df142f9be19d557aeeb39bff4034168f97037fa39e3a2e3ebc57da5e17ff915f49ffaf183ff004015f157fcb13feeb7fe82b5f6af85ff00e457d27febc60ffd0057447632add0f24fda23fe415a067fe7ea5ffd0457821ff59f87f857bcfed1bce8fa07fd7d49ff00a08af9fa59048463a015324dee654d5cb5226f3ee3bd47e7bf4c0fca93ed009ce0fe55006c8c66af9fc8da2bb964c18c7cff00a54464f9dcaf7cd1e73f9b8fdde7a67b53cfde9be9fd2939360a4fa90cdfeb5bf0a9e4e57155bf82acbc71ac7dc63d2b36ae4ce37491000ac598920679f5abda55b34f74bb94623f99bebdaa96ef91707935d26976df67b40cc3e773cd36ec8d30d49ca7aec8b8cdc007b5441864fa9a256c0f7aad14e166f2dbbd676b9ec269685956c1f6ad4b298061c8e2b289da6a5865da78c66a5aba0a91ba3b1b39f2473d2ba3b29b3815c469f3fcaa4d751633676fb54ad19c352363adb3954edf6ad9b79718f435ccda4a36e46335b56ae48049aa39da37a06f987bd5e8dfdeb1e09391eb5a713861c55264345a07345354f414b93ea2b433b1148b93e9cd7857c58d0fec3af45aac63115fafccbfdd917afe7c1af74939435c87c40d1bfb67c2b7f1c69bae211e7c3eb95cf03ea322a3a9bd1972c91f3ce79ea314b9e3349b72dc0f947afaf7a3031d299ea0b5e95f040e7c6b75ff005e0fff00a312bcd6bd2be090c78deebfebc1ff00f46251131c47f0d9efa7a9a51d4507a9a075156787d4f88e738ba9c6402656ff00d0cd49a7b837eddb6a1155efbfe3e27ffaeadffa1354da51dd78edfec93fa8aa7b1e853dd1b09d0ffbb534276b2d4153467256b04761bda730dd8c8c575764dfbb523b571f60f8718aeaf4f6071f4ac8c2a1d5d89271d2ba5b293017dab96b13c8c574b627a1ab460f635bf87f1a8a5fb869e0e4645452701b9a721228cc010322bc4be2f5908b50d3af3030d1b42cfe841c8fe66bdba5195af3cf89d60b77e149e62b96b561328fd0fe869c5d98cf11dded4e3c638eb4d08b9071caf4a9302b449bd84c842045c2fae79a7ac5b9d723a6697e5c77a92d86ee7d7a50e4fa8e31bb2fc23e5e07b0adbd3e3ddb41ef593129c2f5adcd3d3e61ed5948ed7f09d2e9abb71cd74d64010b5ced80e01ae86d17eed668e591bb6c3e515b96a3815896bfc3cd6ddaf038ab4497c605230cd00e40348c78a6664129c03d2b0b5b5dfa45f20c736f20ff00c74d6c4cc496e4565df80d6770bfde89c7e86a7a9513e516c983dbff00b1afb57c2dff0022ae91ff005e307fe802be2619313a3e4051c0afb63c29ff0022a6907fe9c60ffd16b5d3132aaf53c93f68eff902e81ff5f527fe822be7cb7ff8f8c57d05fb46ff00c82340ff00afa93ff4115f3cc1feb57eb43d51940b9e4a671b7f5a8c887a6f6fca9ff694c746fcaab738f6ace29f5368dfab13ad3c28c5357ef5498abb37b17143546052d2e0518a7cacb45cb0845cde2290028e4d746e428c0e838154349b7315b9988e64fd2aec9bf07a56527767761e9da372acae49ea7ad665d4c22954c670c0e78f5ad094e14f1ef58739676c8e9ea6aa2b422bd4e5573a0590320718218669d13e1c13dcd50d366f320319fbc9c7e156f3e9f9d4b5635a5539a299b965300ca39aea2c26f956b86b69983039f9aba3d3eeced00f5ace4ba93555f53b6b37181c8addb57c2a935cada4f82be98addb494605099c7247496ee073dab4a171918ef5856f2022b4a090607a8aa464d1b311ce33527cbed55627cad5956c8e6ad322435862aa4ea08cf07eb56d8e6abcaa7181c7d694b71c4f9b7c65a3b687e28bcb550044cde721f546e83f0248fc2b08f4af5cf8b7a479fa6db6ab1212f6f27972b0fee3743f8371f8d7910fc3f0a0f4a94f9a201baf22bd2fe081cf8d6e87fd3838ffc8895e686bd27e087fc8ed75ff5e2ff00fa3129adc588f819eff4529ea692acf10f87ae64d975719e9e6374ebf7cd4fa460dcc9e9b7fa8aa973ff002109ff00ebb1ff00d08d5dd17ef4bf414e4775176367fe59b7d0d3e1fe0fa533fe59b7d29f19e5795ace5b9dbd4d7b13f38aeaac08de2b94b2ceeed8ae9f4f38032474ac7a99d43aab238e2ba5b16f94572f644903a6dae86cdc8dbe95473b3781caf150b6036d3de88e5c26294d39044aae720e6b075ab44bcb1b8b6700a4d1b2367dc62b75beee7d6b36f40dad9fa1a94f503e5d78da199e26c8f2db69fc0b5381fde11ec3fad6cf8c6c3fb3bc577d1e00590f98b8ff006b9fe79ac084e24fc0ff004aea5ac476d2e5973daa5b08f6439fef1cd57910347b3a0238f6ad0b68f08140c0c5448aa45f817eefe75b960a2b120182b5bb60b923eb594dea743d8e9ac97e5ae86d060af4ac0b1505793deb7ed40e2a51cf2372dbee8c56cdb75ac5b5e140ad58090704f1ed42dcc8d156c50c73480d21aa96c16d4a92f39acfb9e55c7a2b0fd2af49d2a94e0139fc2a57c411d8f9265fddc92f1dcad7db5e15ff00915748ff00af183ff4015f14dde56ee603a6e7fe66bed6f0b7fc8aba47fd78c1ff00a00ae9899553c8bf68dff904e81ff5f527f215f3ad7d15fb46ff00c827c3ff00f5f527f215f3cc3feb57eb4d6c61157268f3b47fa9fc7ad5ae7daab7d99fda98b8ed59b499d0a37ea2af4a7520a777ade0b43625da623bb83db153db406e67485412377cc5bb8aadfbeff6ff005ae8b44b32911b871866e067d2a2a6c5528ae6d4d386df0a36f4031492c2761f9455f85463a8a91a0dc3b7d315caee7629a4f467297b16d523b1f4ac0662f26c6391d7df15d9ea16df29c7e95c7df0315d676e3dbd6b681c98cd63743ac6731df107a37073f4ada2df293e9d6b9a663e6075eb8c9f6adb82712a236467b66aa4ae2c1d5b2e565e8db6b66b66c26e6b0837ad5db59b0e01ac9a3d0ba923bad3a6c80715d15a4c3815c669770368e79ae92ce6e17a566b438e71d4ea2da5c1cf6ad58250581ae7ed64054035a96f20c0c1cd59835a9bd0484918236d5e56cd63db382a3a568432e7d29a7a90d16ea27191ce29e1b8a4229c9dc48c7d634f8b54d32e6ca6c2a4f1b47923a6475fc3ad7cd179692585f4f6970bb6682468dc7fb40e3fcfd6bea57195238fc6bc4be2ae87f63d762d4e288086f061d8ff00cf451fe1fa83496e7461a76767d4e0abd27e08ff00c8ef77ff005e2fff00a312bcdabd27e077fc8ed77ff5e2ff00fa312aa3b9d388f819efe7a9a4a0f534559e21f104ff00f1f937fd756fe6f53e92bb5a6527a05aa77dfebe7c7fcf56ff00d09aae6967f792ff00b8b4e5b9d9451ae1b9f9b9a901008aaead8073d4ff001531ef608cfef26518feef358d9b6762925bb3a1b26191ef5d2d838e0d79ec5e20b4848f9ddb1d08157e0f1fda5ba802ce66c7fb6054fb395cce7523dcf58b17185ae86d1f3b715e250fc56484809a4b39f79b1fc855b4f8d7343f73438cff00bd39ff000aa54e473fb589ef709e0e69646e801af0bff85f37e3e54d0ecfead339ff000a63fc76d60fddd1b4f07d4c8c7fad57b37b8bdaa3dc1beef38fc6b3aeb9420e0e474f5af1597e37f8898fc965a6a7d371ff00d9aa8c9f17fc4d3921a4b18c7a2c1ffeba39193ed11a3f146ccaead69761722589a327fda073fd6b855445390066ac6b3e30d5b5f489751921748d8b26d8c2e0fe158bf6c94765fcab44acac3754d5009e0f02b422e00e45736b79300482bc7ad4a9a8dc375641f5ef438dca857516759029041045743a7e0b0eff00406bcccea374ad859d863d0e29575ad454fcb7932ffc0cd66e9dcb7894cf73b18ce0707f235bd6e3007ca7f0af9c7fb7f5539ff8995da8ff006656ff001a43ad6ac391aa5d9ffb786ff1a15232758fa92ddb8e4e3ebc56a5bb74f987f3af92535fd622391abde83ed3b1feb56a2f17788a320c7af6a031ff004dcff8d3f662f6a7d709215c77cd0d2060a46377b57cc163f123c5d692a05d666917b79a16453f98aea6c3e35eaf1285bfd3acee4f7742d1b63d76f23f4a4e0c4aa267b7336462aaccdd4715c5691f16bc3fa9055badf632138fde8de99ff7874fc40aeb4dc452c6248a4492260487460c0f1ea38a8e5699a4649ec7cab7c3fd2e73e8cffcdabed5f0affc8a7a37fd7841ff00a00af8a2edb7c93b67ef3b9fd457db1e16ff00914f46ff00af183ff45ad7447631abb9e41fb48ffc81342ffaf997ff004115f3ddbffaf5fc7f957d0ffb467fc81340ff00afc93ff4115f3fd9ff00ab3f5fe9449d91925a58b03eee3355d3fd79ff007aa5ff0096dff01a8613fbd51ee6b382d4e88682c5feb17ea2a6915045e62e47b544cbb095e29ccc56d588fa56daa469276574364b9903011e31fecd27daae24520c9263d33c5185ea683b7159731c7ede4203213c4b20faf340baba46c2dd3e7fd973516d90ff00cb4fd69be49fef2feb55a0d4df72e7f68df72a6e6523dd89a865b895f9906fc1c86a48dc3e78e9da95d772951d0d2ba4c4eac9e8c4e3224078f4c5113c901ca1c83dc522af94a4311cd3b2dfe54ff85377e80a528bba2c0d4258ce1c03f855b83578c7de5c7a5647da33d8fe747eeb3d7f9d26afba378e26a4773b3b1f11d9c4c3749b79c7435bf67e30d2507cf7a8bdf956ff000af2f58d1c703f114ffb3afa52f66984b18fa9ecf6de3bd01701b52880ff0075bfc2b4e0f881e1a0e01d5a3ffbe5bfc2bc11d03019233eb4c78f7bb3671e99a1462ccfdbb67d1f17c45f0ba633aa2e7feb937f855b5f8a3e148ffe620edfeec0d5f3488401d49fc7a55d8a350a3046074f9aa9530f6ca47d1fff000b6fc2c17e592f1fe96f8fe669a7e2f7870f022be3ff006c87f8d7cf68066acc5ce7ad1c8839d9ef27e2a78764ff009677a33eb18ff1ac8f16789fc37e27f0edd59c777243311be132c247ce0ee00633d7a7e35e4d1e3ae6ac2952410471eb472a2a352cee5074689ca38da53a8f4af4bf81e7fe2b7bb07fe7c1ff00f462570775187b52700edfba476aeebe06a81e37bbf5360f9ffbf894ad66773abcf4a4d9f401eb45145367947c3b72bbaf261ff4d5bff427a7dadc259a3b93b8903814cba1fe9571ff005d1bff00426aa33020807b8cd538dce952e58dc9ee3519e7fbce71e838aa64e69d21cbff000fe1de9b4ac6729396e14a09072091494532428a28a0028a28a0076f7fefb7e74da28a0028a721507e6cfe152f9a9fed7e54989b6418cf4a5552c4014a8541f98123daa51246a7807ea050c642460d396273fc07f954cd72241875e3da9be647fdc1fad1761a0c681d7ae3f3a7f92be8df9d1e6c6bf71587d79a91640c38f4a4ef7224e48625bfef769edfad4df64c805631f8b527989ea3f3152472e3a723d6a84a5aea2456930dc10647f776e6a4f2da25c0e31d8922a652bfc4054c8c01048c8f7a1a2ec56dfcf2b838c0e6b7bc35e2fbdf0e4cf146cd2da48ac1e26e76923823d39c567490a3a923e6aa3e5794090df20ec7b0a3951716d155972aeccbc804124ff157db7e14ff00914f47ff00af183ff45ad7c4a42aa48dbd5cb0afb6bc2bff00229e8dff005e307fe8b5a48a99ccfc45f87c7c7d63636dfda9f60fb2c8d26efb3f9bbf70c74dcb8ae013f66cda38f1503ff70e3ffc768a2a91085ff866f5ff00a1a7ff0029a3ff008e52ff00c3388ffa1a47fe0b87ff001ca28a95a15762ff00c3397fd4d3ff0094e1ff00c729a7f6702cbb4f8a88ff00b87f1ffa328a28b04a6ec37fe19b3fea6bff00ca70ff00e3947fc3361ffa1a8ffe0b47ff001ca28aab19d83fe19b73ff00335ffe537ffb6527fc3349ff00a1a47fe0bbff00b751450d5803fe19b307fe46927e9a68ff00e394eff866bffa9aff00f29a3ff8e5145263486ffc3360ff00a1afff0029a3ff008e53bfe19b9bfe86b3ff0082effed945155625a1bff0cd4fff004389ff00c171ff00e3b47fc33537fd0dabff0082c1ff00c768a2917617fe19b9bfe86b1ff82dff00edb4eff866e3ff004361ff00c177ff006ca28a1225c5219ff0cd247fccd23ff05dff00dba9ff00f0cdc7fe86b3ff0082dffed9451498342afece18ff0099a89ffb877ff6ca957f6760a3fe468ffca77ff6ca28a62e544cbfb3d95ff999f8ff00b07fff006ca957f67f2bd3c4bff923ff00db28a281d8913e0395393e23fcacb1ff00b52a51f02c8ebe22cffdb97ff6ca28a0ad847f81798d93fe121c823fe7cb1ffb52b5bc17f0d8781f5b93527d656e8496e6131fd9bcbc64a9ce779feefeb45152d0f9ddac77adaa58a1da6e541fa1a7c77b6929012753fe7de8a29a26e78ac9fb3b79b24922f8ac8dec5801600e3273ff003d2a19bf66d129cffc257b4faff6767ff6ad14551447ff000ccc7fe86dff00ca6fff006da3fe1998ff00d0dbff0094dffedb45152487fc3331ff00a1b7ff0029bffdb68ff8666ffa9bc7fe0bbffb6d1450313fe199ff00ea6eff00ca6fff006da3fe199ffea6effca6ff00f6da28a761d85ff8667ffa9bbff29bff00db693fe19a0f7f168ffc177ff6da28a2c160ff008667ff00a9bbff0029bffdb697fe199bfea6d3ff0082dffedb45140584ff008667ff00a9bbff0029bffdb68ff8667ffa9bbff29bff00db68a28b0585ff008666ff00a9b4ff00e0b7ff00b6d27fc333ff00d4ddff0094dffedb45140583fe199ffea6effca6ff00f6da3fe199ff00ea6eff00ca6fff006da28a2c160ff8667ffa9bbff29bff00db69cbfb3485393e2d6fc34ec7fed5a28a04d0bff0cd527fd0deff00f82eff00edd4f4fd9b5941dde2cddf5d3bff00b6d145244b57245fd9d31ff3349fa7f677ff006ca78fd9e0a8c0f149ff00c17fff006ca28aa608913f67c65ff99a7ff29fff00db29affb3cf980e7c518cffd381ffe3b45148b2b9fd9c414dbff00094ff0edcff677ff006daf6cd1ec4e9da5d9d917dff668121dd8c6edaa0671dba51450c19fffd9')");
			stmt.executeUpdate("INSERT INTO BLOB_TEST VALUES (2,'89504e470d0a1a0a0000000d49484452000001a30000011e0802000000a8a3ed0800001bf949444154785eeddd3f93e4c679c7f1c6d52e02baf42ee4488142a50a153a70ecf0029901830d98e936537001039a2aef6b50e0d0c588a1e5e0caac322f38cb2fc30ab0011cf4cdc367fadfd3e8017a8099efa75853338da71bd825e7c70798ddc530cfb3c3da9e9e9e9c73efdfbf0f9e63234f4f4f977c872f9c8efd1b483a0037ef8d5500008747d201b87d241d80dbf7a05fe8ebb2cfcfcfa97a00d8973ffce10f56894abaf8e3a71f7ef8212c07803df9f69b3f5a25ce053d5decfbefbf2f1700c08a3e7dfc6095fceccbafbeb64a3efb9c74fc3c11809df8d5af7f639538b7f00adb8323e6d6308ee3344dfed1aa0560f8e9c7bf5825cbf4feec751c4779f44f8439abb0d55c61a945ab49c0f9b0b3ca01f4f6e6ea0ddd74d29c1192325621803bf5d02de6741fe7541f24f4d99f144b8d1fc96d157a53728a8bd6d435f13124f7a82b832f8473586087fa9dbdeacecb3771e5624f478c4c89b7c6737ddcc42bc88814e81a1d527e534d25804dd57fc69ad32fe916194f1ab65e22175ef11e8939a00f1f7317865da7a49398d08f8562e9a1966eddc8a458b50056a303ee92b0eb947453c5a9eb21ce07b768240124c5d1d61c76c6ef487420d92131374517e064c4a764b055fac47250063b9ace2fe105c5b93de62a65dc3c0c00952a7fd3ab46bfa4d36d5d30982bce8d1416d171997b929c587e5979d800f6297df63a0c2fc9712449d3474307ec533ae9e6f96d72fcb8b60ea064c70a60271e9ceae0e6f9ad3c7ff72e37050036b4e857f72b3d0cc38befe07cc6e9e700d059f3a7ab650f72a29a3c637d7c7c8c070160238b3e6fad8fc57e9fbd02408dadfe3edd8d19869ddec4d61fd86e0f0fd88fc3ff7dba6118e47153c339abbc965e6ad1b212703eecac72006bbae59f329915abb6c546cb02585dbfb3576964a4adf34921e3e5974e254b794a8e3e6dcced3d18294cd15bcd631ececf583987053aeb9774f2f64ec6872e0b360523c194c26a4100c50710cfad09a0a0207900852f10407ffd92ae926e882ae5a6c411930c4a07e0d6754a3a0994e1fcd4352ed3dd505c907449d374c95c0047d1e9b3577d4e37cff346f96286a39ca22637c583006e43a79e2e47e78e4f4033710a53e22b68c12c895aff3c9e6b1e8f8cc8be0a0710cc92ad5cbc033aeb97741225c9f19a973a5ccc29e51de5968a470a0535c743a2017bd0e9ec15d2fad1d001fd9174fde49a4d005b23e900dc3e920ec0ed333e91f8bbbfffe7720100ace83ffffc4f56490b7a3a00b76f5f49f7b7bf7e6795845699525ec46f6dd811809de89d745ba7c6dffefa9dfec72ab7fdedafdf7df1cbdf3be7bef8e5ef575910407ffd7e72b80f9f4a124f00e07a269d3444d2d6492af9f1e4cbca11574cb7ca15e2f1604ddfd691a1c0e1f44b3a89091d167170049be2e264d6940328de5df200ca0706e0b8fa255d33f3ea98994ae60aa2be12c081a4936e185edebd4b6e69943b75ad6156964f2a834e2d5923cc7d0138a24e77ccd1e78c5ffcf2f76d8152c8a92fd6fe6074ddd5005cd783736e185efc8b797e2bcfd7ede972744225e32f2ec84dc97576c9102caca0c7833517b5a200f6e361185e7c07e7334e3f5f57f05140305e787ef994f24b57dc17801bf0464e54573f63bd0dd2e8d1d001c7d5fb77248e28d78d02380a920ec0ed23e900dcbef4cfd3cdf3dbe7e767e7dcfffdcfbf240b00600b3ffdf817aba4053d1d80dbb7afa4336ff67a03e40e61562180d5f44ebaeddee783e2ac5d6c71009eb95fb943d876c700207080dff0afc14d54815bf2e77ffb77ab64997e49272d4c70836719d751150f0623c959015f93ab2f6cd52f7559a1b266e5208ee311005f7ef5b555d2644e71ee5fdfbd7bf7dbdffe36b9b599537777d6236641b22c781e7c45f92fad740cc1d6e049f9c02abfbaf20870e7fefbbffea3e1d1d4e96f995c2eb8aa75ba2277d65279c9eb5f717dfdd6b64aafbe128073ee57bffecd4f3ffea5fed15aefb337ceb96178f1ff04cf5724e9a01feb4984c9449dd6e5b9ee74a65c28ae5fadbed2a50e1b4099196dc1a3b5de676ffcdf32d1bfe7bf454337a74e0c974ace5a3744ea57abaf6cfb6281fb64465b634f77ddbf65222d8f0e0e3de86322785928886b927bc96d2da7524da55e39289ecfcfaccddd0177c88cb6b69eaedf67afbaad8bc783e741597956b2c06556aed91a1f46cd8199c5004c66b435f6745601d6246d1d0d1d9064465b5b4f47d2f596eb6d01b8ce3ddd55aed90180196df474000ecf8cb6b69ecef844e2f5f5b55c00002bfaf4f1434dc02d0d3b7a3a003b52136d4b63ce1d34e9c6711cc7d1aaea441f897fbe9f63030ec78cb6635ca7bb3c0bc6719ca6699a26b77c9d51b16a17f307e69c9ba6698bf5817b6046db1df574c2274b258948094a007b63465b5b4fd7ef7724a4cd91b62ee8cbca2f9dca353fe85ba7f8518aa54cef2e50bfbbb8462f18ac2fc7e3002c61465b5b4fd72fe98248f283411c24371546921383682b278eb9bb9a2304b01633dadac2ae5fd26daa3971741357a9610a804a35d1b634e65cb7eb74e3388eeab3889d84c5a8aedc59b53f9b14ab16c03266b4b55da7eb9474fac4f02a19316df079e8ea0b0230a3adada7bbf2d9eb74fe1942f0323faf451076c1ee722a8f702a5e7f0450c98cb6b6b0eb9774baad8bc76b5ec62be446924f82e7c991f252352f015ca226da96c69ceb76f67a0fa4dda3a1039a99d1b6ebeb747722d7b702a864461b3d1d80c333a38d9e0ec0e199d1d6d6d3199f483c3e3e960b00604566b4b5851d3d1d801da989b6a531e77249370c2fc971c4e45e5f5621009b196d6b5ea7dbee8e39ebe642799de15ca1b25eb04eeeaed500da98d1d6ded30dc38bff27787e74f33c4b0c71d741e010cc686bebe91e86e1c577703eddf4f37549cb13dcda59c67d5b248fc1a660916024571f0b6a727b74f935658fc3f9dda9e311004b99d1d6d8d3c989ea7667ac9ebea3b334593e1a74cfa5434736e9b8498ee4ea63714d728f7a5350af0b00accb8cb6c69ece2ae84d87481c58e56b61e5adb99a726cd5ac09602d66b435f67456c13a866118d4671195f1312b2ed57395eb63e60a31734d002b32a3adada7eb947473ead4b55e65328aa5f535b6581340c08cb6357bbaadafd989799e8793c22689c8a0584692f5b1c2ee925b0bc720cf657a61bf002a99d1d6d6d3f5bb4ea7dbba783c7e6e16c723c9a029af997ba9c3ce9d23ce80ed98d1b6664f77b7820ece246d5dfd14000566b4edbda73b8486b4caf5aa001a98d1464f07e0f0cc686bebe9483a003b62465b5b4f973e7b1d869777ef9c73eef5f5355900005bf8f4f1434dc02d0dbb2bff9409006835d1b634e6dcaefe9649eed6abe62d59af486e06661502a862465be3753affb74cf4eff96fdad0ad180de545c67385ca7ac13a72b7c3a9e20ed9006a98d1d6d8d375fb5b269d4dd32431c46d0981a330a3adada7ebf7f374d2f504b77f8ebb211909122a1ef723c13a855c0b6afc31248f24b7a6ec713cbf7d753c02a081196d8d3d9d55b01a7dd767e9b37c3ae8b64b8fe8e8498e27d7299c48c6353ae664abde14d4eb0200ab33a36def3d5dbd5c5495af85d55c298b6bcab155b326801599d1b6664fb7fa35bb711c47f55944394126a566bc66ab4b756d26734d00eb32a3adada7eb74f63aa54e5d4ddb35774b6db1268098196d6d3ddd95cf5ea7e8b2da747e51ac302e233e3a93b3b4785fe5ad357b947de9e7009a99d1d61676fd924eb775f17861445e962b9341935baafcb2109ac419b0a99a685b1a73aedbd9eb218c279571262d5efd14006566b4b55da7ebd7d3ed5f435ae51a55006dcc685bb3a7bbcaefbd0280196d6d3d5da79f3201801a66b4b5f5740f4e7570f3fc569efbbf4ff7f8f8989e07001b30a3ad2dec7aff2d130028a889b6a531e76ef86f99dc9be41d6c57b7d15e365ad66db9323662465bdb75bade9fbdfabb05d6df335057d6cff2caf5fa3db068d9b2e080fd93e4fa4bbf9c1ac11bbb79fd781df36883efa7592f92df31575ca47ef14547823d30a3adb1a7b30a7664f5ff5ee7938dfecf7f951b24ce8a555b12ac535ecda7497d7d52bc484ec3e2380a33daf6ded3499a04b7828e1b9f60c4bf9c4fff73d65b655372a29e9bdc9a94dcbb1e49d6ccaa77088e2a9812af933c3cbd66d0f52cfd72e26373a99d260f3b5847bf948985faf8ab4e7e99c915bcf8dfbe7e94b2f85f8d0c06df40ec9c196d6bf6745b5cb393ffecfca3fe0fdd1b54024a812ed64bc97fbeb3fa2f3e58aabca3a464593092acd1ef25bf295e3cb772cd9a35abc92c212385bdc8cb78efc13a7a17c13afe495c2c65b9894171bc483c3d98a257d3eb3b1c93196d7befe972726fa4647159bc5479ab8ce8dd9517c9693b602df9ce972743aab97329f191b41d5b7956e168e37f7d3587ed151671d621e13698d1b6664fb73af91fb57ef466455e06ff63af61feff3cd8918c98657d2cdaaff9c56eadfe6883ccaa9f88fb64465b5b4fd729e9e6538ab9fcc95d107f71c15a820c9d33e7b34ba3762dc9fdce7bbdd864f6d1b963bed6b7173b6746dbae7bba9c595d9af16f89e0e55cdddfe9a5821197da513c3757168cd42c35641ad85c6579cdc2dcdce2494bf79253f8b716afac8f339ea8ff37132f52ff6fdf4b7e5b86cbbe587466465b5b4fd7ef3a9dfcb79b1ccfbdd423c10a8527f1dc786beea57984f148cd52c9e7f5876716b47d7b73dfc3dc3ab9ef92595fa82c6f2a7cafcadfd2fa7cc4ae98d1b6664fc7df32413773e6ea419ba02ba4a13b1c33dada7aba7e3f657250bc4f3a58f19b3c9fc8cb723df6c68cb6f69e6e185efc3fc17300e8cc8cb6c69e8ebf6502603fcc686bebe91eca7fcbe4f5f5351e04808d7cfaf8a126e09686dd957fca0400b49a685b1a738ea46b50bedf36f7c0062e61465be3753aab60657de2a0bcfea8146a729b92e44688e5fb67032833a36dcd9eeee63f94984e48256057cc686bebe9fafd8e84644a70376819979772fbd45c4d305298929b1ecbed227790c14bbd72704800ea99d1d6d6d3f54b3a79ff2783499ec765714d4d8e1476e152b916d72457c815035885196d6d61d72fe9722474449c6571cd22c95dc8a620fb005c514db42d8d39d72de92447c6f35357573ca314353505e6f4a0ef2b1703d88e196d6d61d7e9b3577d3ee84f51e31a9d8653e6b3820b63e8c2e900b656136d4b63ce75ebe97274a20557e5e41c36ae0946e2589491e474175da76b58212ed68b27cb0098cc686b0bbb7e492799921c4fbe94e78b66c52366717230b74261650017aa89b6a531e7ba9dbdde3ce9fb68e8804b98d1163c5aeb7d46d2ad26d7b402a867461b3d1d80c333a38d9e0ec0e199d1d6d6d3199f483c3e3e960b00604566b4b5851d3d1d801da989b6a531e7f690748b6ee8291aa6b8d4acf23a43f186ad00566746db31aed305d9e16f522737ac2b4c310de7ac72db90bafb32804d99d176d49e4ee46e585779233b494c7d133c00c762465b5b4fd7ef7724741fe754c7e45ffabe29e89e829b13cba6e4cba4b826372b5e5c17c8e139005b32a36def3d9d345ceebced8a5bb0f944a7de703acf0d26cec553cbb8c65c9c1355e08acc68db7b4f77b986aeaa3eb6ea2b016cc78cb6157abaa7a7a75cdd85e45302fdb888345cf573e336b06056ac5a005b31a3adada7fb39e99e9e9edebf7f5f28bdc49c39755daa7962bdfa2405b03a33da56e8e9f66c38a90f3be901834b72f13aba52e2389855bf5f00cdcc686bebe93e5fa7dbb4a1f3745b170ce60a824eb03c317e6ebed4230419b00766b4b5f7741d62ee88a4ada3a103ba31a3adada73bccd9eb5524db4900db31a3adb1a7a3a103b01f66b435f674c41c80fd30a3adada7337e72f8f5f5b55c00002bfaf4f1434dc02d0d3baed301d8919a685b1a73ee66926e1c477d63ecd5ef60bdfa8242ee2866150277c18cb6c6eb7456c1ca1aded8717190657203ece0b969d161b8d37e574cd2f1fc36de563970fbcc68bbeb9eae9be9846002b660465b5b4fd7ef6f99e8b34ba7da191d19c188b4667e44776dfa495c602eb5b4b82c5eb3f0442af5f12747803b6446db9a3ddd30bc24c72f21a769eed4193975b2a9c34546742e942340172417d7f1da5c9c943c60b3b25006dc3333dada7aba74d2cdf3dbe4781fe38955685bb4544db1d49869e56b82d52a8f04b85b66b4b5f5740f4e7570f3fc569ebf7b979bd242dee1e3f9a96b5261934c9797e5c431f348ab299ece4f4217d1736bf605dc2133dadac2eecd30bcccf35b69e2f4f3154da953575332502625de9ab3289bcce2b8598bb7ca171bc4f1a2c306ee4d4db42d8d39e7dc83ceb872e916e2c8d0233a2cdcf28c282ce54e89292b978b631261f1c49ce4fa32c5ec4f817b60465b5bd8f5fbec55dedec971170541b226d814af992c288c14e6968bf5cbf2c4421980404db42d8d39b7879fa71b4fee3008a41fbccf2f1f8899d1163c5aeb7dd6afa7cbb9f37778aed505ee93196d6bf67457b966070066b4b5f574d73f7b050061465b5b4f679cbd3e3e3e960b00604566b4b5851d3d1d801da989b6a531e748bafee47e635621708fcc683bc675ba86f7795c3c28c9294bc547359ccb4f5d66c8dc391b806746dbbdf4743e2c84555e25b9ceea7b016032a36def3d9d3447410315b74e7127a5e72673a7b042e5b82e08573faf89eb8375f46330ae0f9eb60e8899d1b6f79e4edf255a1aa5413568be4c8f4810941babc214d9648eebadb26c21f85c66bf43268b01d430a3adada7bbfeef48c476d2e9048125f9a8b32c3e54d994ac07506646dbae7b3a698ef463ceac14caf6e040870a1c82196d6d3d5da7a49b53a7aea664c774c58eafd0a0258f8a860e58ca8cb65df774393e0bf4e5303d22c9982bf083f19436c18e82bd14ea2bf71bc474e52ce0ae98d1d6d6d3f5bb4ea7dbba78dca9b6a850b3b42078b2743c563e12920bb890196d87ece99c6a9d6e2f26e22f4adaba9bfc7a81cb99d1b6f79e2ee786dff0c92f2dd7db027037dcd3018030a3adada723e900ec88196d6d3d9d71f6fafafa5a2e0080157dfaf8a126e096861d3d1d801da989b6a531e7e2a41b869761784996e28ae416625621706c66b41de33a5dc33b362e1e95e49498aeac9f9553b3f7f15ca1d2349eee9138156fb60ddc0033da2eeae90ed4caf9b7bdb0ca13da66097d00e5dcb9f038813b6446db9a3ddd16a927ad8d7e74e7bd4f30929c9b4c8ddc0ab945923541716e657d0093ba357550191bcff7ee8a7bd18fc93d0237c98cb68b7abac016f77b9dd42d9ca5cd19557fe4cbc654c7546e8b0a53ca8bc43581e4ca3971659c7d3ab6e479722f41c00177c28cb6b69eeefabf23113333a5a74b0e268e2a3d520e32620ef7c98cb6f69e6e8b73d580b436fa3167520a657decea60809b67465b5b4ff766185ee273d5d5b36f4a9dba9ae2409c2ace2237e2f71b1c00e798c0bacc686bebe91e7ccc49d8c993e7e7e7eca4f5c4c9359d5f59d323c14ba989a7c48245ca35c9119958b33b2f38ce5c597941bf55c6c956dc3633dadac2aedf753ac9a9e4b853b950a86928d009a59f946be24de5c178b59a9aca97c0fda889b6a531e7faffe4706c3ce1ed9d234d1fdf25dc3c33dadaaed3f5ebe97278ebd6c8f589c08d31a3eda83d1d000833dada7a3a920ec08e98d1d6d6d31967af8f8f8fe502005891196d6d61474f0760476aa26d69ccb95b4dbadc1d5af740ee0d661502f7c88cb6635ca76b789f07778316852939e559c3b94265bd609de1fc76dd9949c0fd32a3ed5e7abaf964f5a4f0cbea27003a33a3adada7ebf7f374124cd2d6f934d181158c48dcf891e45da27353f44497592a374b0b6afc1edb0e7e38bf9b753c02c08cb6bdf774d22e39d534f9b7baeea1f4884e8d422224a7e4c665a9dcac405ca3634eb6ea4d41bd2e00506646dbde7bba7a85dcc9c94d292f55b3a3b8a61c5b356b02c831a36dd73ddd300cc3e9a4d55971302b85322d37a5bc94b9a3b86b33996b022830a3adada7eb947473ead4d5647664f13adb35774b6db12670f3cc68db754f9733cff370128f4832ea82606b724acd52b9595a7c78e5ad357b94e985fd0277cb8cb6b69eaedf753addd6c5e3aef849a80e3597128c17ea83287491dc52e59735070fc06446db217b3a97ead10e64e9c14b5b573f05b82b66b4edbda7cb39f41bbee1e073bd2d00d7b9a75bfd8e390050c38cb6b69eeefa67af0020cc686bebe91267afbaa17b7d7d8d0b0060239f3e7ea809b8a56197e8e9e2dbbf02401f35d1b634e61c67af35b6bb97b6dcf1cb2a04ee85196dc7b84ed7f0ded6c572cbc4427da5a58b8c8a555b456e69384577d406ee96196d77d1d35df76680d309c1046cc48cb6357bba2d2ed5493714b47571af14b74ec94e2a9e18cf8d5fba68d7c9cae448527217b92752399edfa39af4043c33dada7aba7e3f39ecdfccf2e80783e7f148b9890b2a83f8482ea5077387111fa4cbc87d2de54a003966b4b5855dbfa4ab77497713875d50908b9b9a9d4a8d995966f20248aa89b6a531e7ba259d6444f2dc2dd01c07d2b5c9f3faa56a2af5fa4bc5c7062066465b5bd875fa4442e7ce344d956ff54b322550bf94593915afa9e9562e6eeb2abf70e06ed544dbd29873dd7aba9c3835f488ce0bfd24d712e626ba54c4e8650b3b4d92082bef424bae2f5338b1053c33dadac2ae5fd2c93b3c39ee8a57c164446f8a9f17269a2f9be7962716ca00c46aa26d69ccb96e67af05e3c9bd65c164b5a8c01d32a32d78b4d6fbac5f4f9773cf6ff25c2b0adc2d33da8edad3018030a3adada723e900ec88196d6d3d9d71f6faf8f8582e00801599d1d61676f4740076a426da96c69c23e92e27f7fab20a01d8cc683bc675ba865c08ee065d78d9df707e076bab1c80c18c367a3a008767465b5b4fd7efe7e9a4e5096eedac5ba160446e8aea4774f7a41f83f58359b9650bf5353572007acac0fdaa81cb98d1b6f79e4edfd1799e678992f9c497e9111d2be5048967052bc73b2ad7d7ac09607566b4edbda7ab675ef09a53dd533c2ba8895f3a8bb926807599d1b6eb9e6e1806698b9c9532b352280b04b3fc13d96ff0b2b23b2baf09607566b4b5f5749d924e92c2559c8a8a42a01456d0b382b2cafdc60a6b025891196dbbeee972a4459228d123928c661b15cf2abf2cefb772cdf9fc274b064e6c818b99d1d6d6d3f5bb4ea7dbba78dca53ee88c6bea3799dd5cc3911064c0d6cc683b644fe7a246e98a1a8e44daba45b300e498d1b6f79e2e673f01d17624b95e15400333da8edad3018030a3adada723e900ec88196d6d3d5df6ead2f3f3f30f3ffcf0fdf7df27b702c0163e7dfc6095fceccbafbefef69b3fd67476d7bf4e07005a4d7239e79e9f9fad929fdd45d2f5bff996df63fffd02b7a1feb4b452efeb7472df3fabf067bad8df2f31f7b2c1a8b8fc812dda8b04dc54bc3d36806eeea2a7cba9efb92acb00ec53bfa493ee26b89db3ee7a8211c9173fa21b25fd18ac1fccd2e29a98ec2bd84bd0a025f7129fb1720e0bec41bfa40b52c30f06cfe391ca7b3fc7b36a56d6e1259bf4bee29032f702602d5f7ef5753cf8ed377f8c074dbdafd3d5184f7205cd8d52bcf27492dc5dc32e00ac250eb5b698733ee99e9e9eacb24b49bee8c79c49299435d86e65005bd0d1d61c734ef77492775b04dfa44e42eb83a6dcd6e536d5e04c13380a1f7097c49c73eec1e7dad3d3d3fbf7effdd0fbf7ef9f9e9e7ef18b5f1427ae233e6d9ca26b67325248b7c2ac424d701aebeaf6a5e5f622d3dbceb2016817c69cf33d9d64dca6745b178c075d5e6e2437dd5c27571317b8ccbe7223f19a0076a8df67af39419f7538f1f14fa94f99015cd11bdfd0056d5d9f2ecf3b7a4f943cfeb807047045d7efe900405bf4abfb95483a003b92fc69e1cb95fe3edd3ffec3ef929b00603f6afeca13f7790170fbf6f8db6000b0ae07e7fed7aa018063a3a70370fb483a00b7cfff2d933f05a34f4f7ff2ffa4a600c0c1fc3f29d84475475eb4170000000049454e44ae426082')");

			stmt.executeUpdate("CREATE MEMORY TABLE BLOB_TEST2 AS SELECT * FROM BLOB_TEST");
			con.commit();
			pstmt.close();
		}

		return wb;
	}


}
