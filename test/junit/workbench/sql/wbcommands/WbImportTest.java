/*
 * WbImportTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.db.exporter.RowDataConverter;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.util.Base64;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.ZipOutputFactory;
/**
 *
 * @author Thomas Kellerer
 */
public class WbImportTest
	extends WbTestCase
{
	private TestUtil util;
	private String basedir;
	private WbImport importCmd;
	private WbConnection connection;

	public WbImportTest()
		throws Exception
	{
		super("WbImportTest");
		util = getTestUtil();
		util.prepareEnvironment();
		basedir = util.getBaseDir();
	}

	@Before
	public void setUp()
		throws Exception
	{
		connection = prepareDatabase();
		importCmd = new WbImport();
		importCmd.setConnection(this.connection);
	}

	@After
	public void tearDown()
		throws Exception
	{
		this.connection.disconnect();
	}

	@Test
	public void testFailedInsert()
		throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			File importFile  = new File(this.basedir, "insert_fail.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			out.println("1\tArthur\tDent");
			out.println("nan\tFord\tPrefect");
			out.println("3\tZaphod\tBeeblebrox");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport " +
				"-encoding=utf8 " +
				"-file='" + importFile.getAbsolutePath() + "' " +
				"-type=text " +
				"-header=true " +
				"-continueonerror=false " +
				"-table=junit_test_pk");

			assertEquals("Import did not fail", result.isSuccess(), false);
			String msg = result.getMessageBuffer().toString();
			assertTrue(msg.indexOf("Error importing row 2") > -1);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

	}

	@Test
	public void testPreAndPostStatements()
	{
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			File importFile  = new File(this.basedir, "table_statements.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			out.println("1\tArthur\tDent");
			out.println("2\tFord\tPrefect");
			out.println("3\tZaphod\tBeeblebrox");
			out.close();

			stmt = this.connection.createStatement();
			stmt.executeUpdate("insert into junit_test_pk (nr, firstname, lastname) values (1, 'Mary', 'Moviestar')");
			stmt.executeUpdate("insert into junit_test_pk (nr, firstname, lastname) values (2, 'Harry', 'Handsome')");
			this.connection.commit();

			StatementRunnerResult result = importCmd.execute("wbimport " +
				"-encoding=utf8 " +
				"-file='" + importFile.getAbsolutePath() + "' " +
				"-preTableStatement='delete from ${table.name}' " +
				"-postTableStatement='update ${table.name} set nr = nr * 10' " +
				"-type=text " +
				"-header=true " +
				"-continueonerror=false " +
				"-table=junit_test_pk");

			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			rs = stmt.executeQuery("select nr, firstname, lastname from junit_test_pk order by nr");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String fname = rs.getString(2);
				String lname = rs.getString(3);
				if (nr == 10)
				{
					assertEquals("Arthur", fname);
					assertEquals("Dent", lname);
				}
				else if (nr == 20)
				{
					assertEquals("Ford", fname);
					assertEquals("Prefect", lname);
				}
				else if (nr == 30)
				{
					assertEquals("Zaphod", fname);
					assertEquals("Beeblebrox", lname);
				}
				else
				{
					fail("Wrong ID retrieved!");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Test
	public void testFunctionConstant()
		throws Exception
	{
		try
		{
			File importFile  = new File(this.basedir, "constant_func_import.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("firstname\tlastname");
			out.println("Arthur\tDent");
			out.println("Ford\tPrefect");
			out.println("Zaphod\tBeeblebrox");
			out.close();

			Statement stmt = this.connection.createStatementForQuery();
			stmt.executeUpdate("create sequence seq_junit start with 1");

			StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -constantValues=\"nr=${next value for seq_junit}\" -type=text -header=true -continueonerror=false -table=junit_test_pk");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			ResultSet rs = stmt.executeQuery("select count(*) from junit_test_pk");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values imported", 3, count);

			rs.close();

			rs = stmt.executeQuery("select nr, lastname, firstname from junit_test_pk order by nr");
			if (rs.next())
			{
				int id = rs.getInt(1);
				String lname = rs.getString(2);
				String fname = rs.getString(3);
				if (id == 1)
				{
					assertEquals("Wrong lastname", "Dent", lname);
					assertEquals("Wrong firstname", "Arthur", fname);
				}
				else if (id == 2)
				{
					assertEquals("Wrong lastname", "Prefect", lname);
					assertEquals("Wrong firstname", "Ford", fname);
				}
			}
			else
			{
				fail("First row not imported");
			}
			rs.close();

			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testConstantWithSelect()
		throws Exception
	{
		File importFile  = new File(this.basedir, "constant_func_import.txt");
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
		out.println("id\tfirstname\ttyp_cod\tlastname\ttyp_nam");
		out.println("1\tArthur\teno\tDent\tone");
		out.println("2\tFord\towt\tPrefect\ttwo");
		out.println("3\tZaphod\teerth\tBeeblebrox\tthree");
		out.close();

		String script = "CREATE TABLE person2 (id integer, firstname varchar(20), lastname varchar(20), type_id integer);\n"
			+ "commit;";
		TestUtil.executeScript(connection, script);

		script = "CREATE TABLE type_lookup (id integer, type_name varchar(10), type_code varchar(10));\n"
			+ "insert into type_lookup values (1, 'one', 'eno');\n"
			+ "insert into type_lookup values (2, 'two', 'owt');\n"
			+ "insert into type_lookup values (3, 'three', 'eerth');\n"
			+ "commit;";
		TestUtil.executeScript(connection, script);

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' " +
			" -importColumns=id,firstname,lastname " +
			" -constantValues=\"type_id=$@{select id from type_lookup where type_name = $5 and type_code = $3}\" " +
			" -type=text -header=true -continueonerror=false -table=person2"
		);
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("select count(*) from person2");
		int count = -1;
		if (rs.next())
		{
			count = rs.getInt(1);
		}
		assertEquals("Not enough values imported", 3, count);

		rs.close();

		rs = stmt.executeQuery("select id, lastname, firstname, type_id from person2 order by id");
		if (rs.next())
		{
			int id = rs.getInt(1);
			String lname = rs.getString(2);
			String fname = rs.getString(3);
			int typeId = rs.getInt(4);
			if (id == 1)
			{
				assertEquals("Wrong lastname", "Dent", lname);
				assertEquals("Wrong firstname", "Arthur", fname);
				assertEquals("Wrong type_id", 1, typeId);
			}
			else if (id == 2)
			{
				assertEquals("Wrong lastname", "Prefect", lname);
				assertEquals("Wrong firstname", "Ford", fname);
				assertEquals("Wrong type_id", 2, typeId);
			}
		}
		else
		{
			fail("Nothing imported!");
		}
		SqlUtil.closeAll(rs, stmt);

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testTwoCharDelimiter()
		throws Exception
	{
		File importFile  = new File(this.basedir, "two_char.txt");
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
		out.println("nr\t\tlastname\t\tfirstname");
		out.println("1\t\tDent\t\tArthur");
		out.println("2\t\tPrefect\t\tFord");
		out.println("3\t\tBeeblebrox\t\tZaphod");
		out.close();

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -delimiter='\\t\\t' -type=text -header=true -continueonerror=false -table=junit_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		Statement stmt = this.connection.createStatementForQuery();
		ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
		int count = -1;
		if (rs.next())
		{
			count = rs.getInt(1);
		}
		assertEquals("Not enough values imported", 3, count);

		rs.close();

		rs = stmt.executeQuery("select lastname, firstname from junit_test where nr = 1");
		if (rs.next())
		{
			String lname = rs.getString(1);
			String fname = rs.getString(2);
			assertEquals("Wrong lastname", "Dent", lname);
			assertEquals("Wrong firstname", "Arthur", fname);
		}
		else
		{
			fail("First row not imported");
		}
		rs.close();

		stmt.close();
		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testConstantValues()
		throws Exception
	{
		File importFile  = new File(this.basedir, "constant_import.txt");
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
		out.println("nr\tlastname");
		out.println("1\tDent");
		out.println("2\tPrefect");
		out.println("3\tBeeblebrox");
		out.close();

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -constantValues=\"firstname=Unknown\" -type=text -header=true -continueonerror=false -table=junit_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		Statement stmt = this.connection.createStatementForQuery();
		ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
		int count = -1;
		if (rs.next())
		{
			count = rs.getInt(1);
		}
		assertEquals("Not enough values imported", 3, count);

		rs.close();

		rs = stmt.executeQuery("select lastname, firstname from junit_test where nr = 1");
		if (rs.next())
		{
			String lname = rs.getString(1);
			String fname = rs.getString(2);
			assertEquals("Wrong lastname", "Dent", lname);
			assertEquals("Wrong firstname", "Unknown", fname);
		}
		else
		{
			fail("First row not imported");
		}
		rs.close();

		stmt.close();
		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testPartialColumnXmlImport()
		throws Exception
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             " \n" +
             "    <generating-sql> \n" +
             "    <![CDATA[ \n" +
             "    select id, lastname, firstname from person \n" +
             "    ]]> \n" +
             "    </generating-sql> \n" +
             " \n" +
             "    <created>2006-07-29 23:31:40.366 CEST</created> \n" +
             "    <jdbc-driver>HSQL Database Engine Driver</jdbc-driver> \n" +
             "    <jdbc-driver-version>1.8.0</jdbc-driver-version> \n" +
             "    <connection>User=SA, URL=jdbc:hsqldb:d:/daten/db/hsql18/test</connection> \n" +
             "    <database-product-name>HSQL Database Engine</database-product-name> \n" +
             "    <database-product-version>1.8.0</database-product-version> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <!-- The following information was retrieved from the JDBC driver's ResultSetMetaData --> \n" +
             "    <!-- column-name is retrieved from ResultSetMetaData.getColumnName() --> \n" +
             "    <!-- java-class is retrieved from ResultSetMetaData.getColumnClassName() --> \n" +
             "    <!-- java-sql-type-name is the constant's name from java.sql.Types --> \n" +
             "    <!-- java-sql-type is the constant's numeric value from java.sql.Types as returned from ResultSetMetaData.getColumnType() --> \n" +
             "    <!-- dbms-data-type is retrieved from ResultSetMetaData.getColumnTypeName() --> \n" +
             " \n" +
             "    <!-- For date and timestamp types, the internal long value obtained from java.util.Date.getTime() \n" +
             "         is written as an attribute to the <column-data> tag. That value can be used \n" +
             "         to create a java.util.Date() object directly, without the need to parse the actual tag content. \n" +
             "         If Java is not used to parse this file, the date/time format used to write the data \n" +
             "         is provided in the <data-format> tag of the column definition \n" +
             "    --> \n" +
             " \n" +
             "    <table-name>junit_test</table-name> \n" +
             "    <column-count>3</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>LASTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>FIRSTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>OTHER</java-sql-type-name> \n" +
             "      <java-sql-type>1111</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>Dent</cd><cd>Arthur</cd></rd> \n" +
             "<rd><cd>2</cd><cd>Beeblebrox</cd><cd>Zaphod</cd></rd> \n" +
             "<rd><cd>3</cd><cd>Prefect</cd><cd>Ford</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "partial_xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			String cmd = "wbimport -importcolumns=nr,lastname -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, lastname, firstname from junit_test");
			int rowCount = 0;

			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", rowCount, nr);
				String lastname = rs.getString(2);
				switch (nr)
				{
					case 1:
						assertEquals("Wrong data imported", "Dent", lastname);
						break;
					case 2:
						assertEquals("Wrong data imported", "Beeblebrox", lastname);
						break;
					case 3:
						assertEquals("Wrong data imported", "Prefect", lastname);
						break;
				}
				String firstname = rs.getString(3);
				assertNull("Omitted column imported", firstname);

			}
			assertEquals("Wrong number of rows", rowCount, 3);
			rs.close();
			stmt.close();
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEscapedQuotes()
		throws Exception
	{
		String data =
			"nr\ttestvalue\n" +
			"1\tone\n" +
			"2\twith\"quote";
		// Test with escape character
		String content = StringUtil.replace(data, "\"", "\\\"");

		File datafile = new File(this.basedir, "escaped_quotes1.txt");
		BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(datafile, "UTF-8", false));
		out.write(content);
		out.close();

		Statement stmt = this.connection.createStatement();
		stmt.executeUpdate("create table imp_test (nr integer, testvalue varchar(100))");

		String cmd = "wbimport -quoteChar='\"' -header=true -continueOnError=false -encoding='UTF-8' -file='" + datafile.getAbsolutePath() + "' -type=text -table=imp_test -quoteCharEscaping=escape";
		StatementRunnerResult result = importCmd.execute(cmd);
		assertEquals("Import did not succeed: " + result.getMessageBuffer(), result.isSuccess(), true);

		ResultSet rs = stmt.executeQuery("select testvalue from imp_test where nr = 2");
		String value = null;
		if (rs.next()) value = rs.getString(1);
		rs.close();
		assertEquals("Wrong value imported", "with\"quote", value);

		if (!datafile.delete())
		{
			fail("Could not delete input file: " + datafile.getCanonicalPath());
		}

		// test with duplicated quotes
		content = StringUtil.replace(data, "\"", "\"\"");

		datafile = new File(this.basedir, "escaped_quotes2.txt");
		out = new BufferedWriter(EncodingUtil.createWriter(datafile, "UTF-8", false));
		out.write(content);
		out.close();

		stmt.executeUpdate("delete from imp_test");
		this.connection.commit();

		cmd = "wbimport  -quoteChar='\"' -header=true -continueOnError=false -encoding='UTF-8' -file='" + datafile.getAbsolutePath() + "' -type=text -table=imp_test -quoteCharEscaping=duplicate";
		result = importCmd.execute(cmd);
		assertEquals("Import did not succeed: " + result.getMessageBuffer(), result.isSuccess(), true);

		rs = stmt.executeQuery("select testvalue from imp_test where nr = 2");
		value = null;
		if (rs.next()) value = rs.getString(1);
		rs.close();
		stmt.close();
		assertEquals("Wrong value imported", "with\"quote", value);

		if (!datafile.delete())
		{
			fail("Could not delete input file: " + datafile.getCanonicalPath());
		}

	}

	@Test
	public void testImportQuotedColumn()
	{
		String xml1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             "    <created>2007-04-11 21:33:05.000 CEST</created> \n" +
             "    <jdbc-driver>HSQL Database Engine Driver</jdbc-driver> \n" +
             "    <jdbc-driver-version>1.8.0</jdbc-driver-version> \n" +
             "    <connection>User=SA, URL=jdbc:hsqldb:c:/daten/db/hsql18/test</connection> \n" +
             "    <database-product-name>HSQL Database Engine</database-product-name> \n" +
             "    <database-product-version>1.8.0</database-product-version> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>TEST1</table-name> \n" +
             "    <column-count>1</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>Pr\u00e4fix</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>one</cd></rd> \n" +
             "<rd><cd>two</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "quoted_column_xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml1);
			out.close();

			Statement stmt = this.connection.createStatement();
			stmt.executeUpdate("create table qtest (\"Pr\u00e4fix\" varchar(100))");

			String cmd = "wbimport -continueOnError=false -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=qtest";
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import did not succeed", result.isSuccess(), true);

			ResultSet rs = stmt.executeQuery("select count(*) from qtest");
			int rows = 0;
			if (rs.next()) rows = rs.getInt(1);
			assertEquals("Wrong number of rows imported", 2, rows);
			rs.close();

			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}

			xmlFile = new File(this.basedir, "quoted_column_xml_import.xml");
			out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			String xml2 = StringUtil.replace(xml1, "<column-name>Pr\u00e4fix</column-name>" , "<column-name>\"Pr\u00e4fix\"</column-name>");
			out.write(xml2);
			out.close();

			// Re-run with quoted column name
			result = importCmd.execute(cmd);
			assertEquals("Import did not succeed", result.isSuccess(), true);

			rs = stmt.executeQuery("select count(*) from qtest");
			rows = 0;
			if (rs.next()) rows = rs.getInt(1);
			assertEquals("Wrong number of rows imported", 4, rows);
			SqlUtil.closeAll(rs, stmt);

			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testMissingXmlColumn()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>junit_test</table-name> \n" +
             "    <column-count>4</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>LASTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>FIRSTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"3\"> \n" +
             "      <column-name>EMAIL</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>Dent</cd><cd>Arthur</cd></rd> \n" +
             "<rd><cd>2</cd><cd>Beeblebrox</cd><cd>Zaphod</cd></rd> \n" +
             "<rd><cd>3</cd><cd>Prefect</cd><cd>Ford</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "missing_xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			String cmd = "wbimport -continueOnError=false -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import succeeded", false, result.isSuccess());

			cmd = "wbimport -encoding='UTF-8' -continueOnError=true -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
			result = importCmd.execute(cmd);
			assertEquals("Import failed", true, result.isSuccess());

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int rows = 0;
			if (rs.next()) rows = rs.getInt(1);
			assertEquals("Wrong number of rows imported", 3, rows);
			SqlUtil.closeAll(rs, stmt);
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBooleanLiterals()
	{
		try
		{
			File importfile = new File(this.basedir, "bool_literal.txt");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(importfile, "UTF-8", false));
			out.write("nr,flag\n");
			out.write("1,yes\n");
			out.write("2,5\n");
			out.write("3,99\n");
			out.write("4,no\n");
			out.write("5,no\n");
			out.close();

			// Test importing correct true/false values
			String cmd = "wbimport -literalsFalse='no,99' -literalsTrue='yes,5' -type=text -header=true  -table=bool_test -continueOnError=false -delimiter=',' -booleanToNumber=true -encoding='UTF-8' -file='" + importfile.getAbsolutePath() + "'";
			StatementRunnerResult result = importCmd.execute(cmd);
			String msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());

			Statement stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from bool_test where flag = false");
			int rows = 0;
			if (rs.next()) rows = rs.getInt(1);
			assertEquals("Wrong number of rows imported", 3, rows);
			rs.close();

			rs = stmt.executeQuery("select count(*) from bool_test where flag = true");
			rows = 0;
			if (rs.next()) rows = rs.getInt(1);
			assertEquals("Wrong number of rows imported", 2, rows);
			rs.close();

			stmt.executeUpdate("delete from bool_test");
			this.connection.commit();

			// Test importing incorrect values
			// as -continueOnError=false is supplied no rows should make into the table
			cmd = "wbimport -literalsFalse='no,false' -literalsTrue='yes,true' -type=text -header=true  -table=bool_test -continueOnError=false -delimiter=',' -booleanToNumber=true -encoding='UTF-8' -file='" + importfile.getAbsolutePath() + "'";
			result = importCmd.execute(cmd);
			msg = result.getMessageBuffer().toString();
			assertEquals(msg, false, result.isSuccess());

			rs = stmt.executeQuery("select count(*) from bool_test");
			rows = 0;
			if (rs.next()) rows = rs.getInt(1);
			assertEquals("Rows were imported", 0, rows);
			rs.close();

			stmt.executeUpdate("delete from bool_test");
			this.connection.commit();

			// Test importing incorrect values
			// as -continueOnError=true is supplied only 3 rows should make into the table
			cmd = "wbimport -literalsFalse='no,false' -literalsTrue='yes,true' -type=text -header=true  -table=bool_test -continueOnError=true -delimiter=',' -booleanToNumber=true -encoding='UTF-8' -file='" + importfile.getAbsolutePath() + "'";
			result = importCmd.execute(cmd);
			msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());
			assertEquals(msg, true, result.hasWarning());

			rs = stmt.executeQuery("select count(*) from bool_test");
			rows = 0;
			if (rs.next()) rows = rs.getInt(1);
			assertEquals("Wrong number of rows imported", 3, rows);
			rs.close();

			SqlUtil.closeAll(rs, stmt);

			if (!importfile.delete())
			{
				fail("Could not delete input file: " + importfile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAutoConvertBooleanToNumber()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>junit_test</table-name> \n" +
             "    <column-count>2</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>INT_FLAG</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>int</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>true</cd></rd> \n" +
             "<rd><cd>2</cd><cd>false</cd></rd> \n" +
             "<rd><cd>3</cd><cd>gaga</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "bool_convert_xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			// Test importing only correct true/false values
			String cmd = "wbimport -continueOnError=false -startRow=1 -endRow=2 -booleanToNumber=true -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=bool_int_test";
			StatementRunnerResult result = importCmd.execute(cmd);
			String msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());
//			System.out.println("messages: " + msg);
			Statement stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from bool_int_test");
			int rows = 0;
			if (rs.next()) rows = rs.getInt(1);
			assertEquals("Wrong number of rows imported", 2, rows);
			SqlUtil.closeAll(rs, stmt);

			stmt = this.connection.createStatement();
			stmt.executeUpdate("delete from bool_int_test");
			this.connection.commit();
			SqlUtil.closeStatement(stmt);

			cmd = "wbimport -continueOnError=false -booleanToNumber=true -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=bool_int_test";
			result = importCmd.execute(cmd);
			msg = result.getMessageBuffer().toString();
			assertEquals("Import did not fail", false, result.isSuccess());

			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testNumericLiterals()
		throws Exception
	{
		File importfile = new File(this.basedir, "bool_numeric_values.txt");
		BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(importfile, "UTF-8", false));
		out.write("nr,int_flag\n");
		out.write("1,true\n");
		out.write("2,ja\n");
		out.write("3,true\n");
		out.write("4,no\n");
		out.write("5,nein\n");
		out.write("6,yes\n");
		out.write("7,false\n");
		out.close();

		// Test importing correct true/false values
		String cmd = "wbimport -literalsFalse='false,no,nein' -literalsTrue='true,ja,yes' -type=text -header=true  -table=bool_int_test -continueOnError=false -delimiter=',' -numericFalse='-24' -numericTrue='42' -encoding='UTF-8' -file='" + importfile.getAbsolutePath() + "'";
		StatementRunnerResult result = importCmd.execute(cmd);
		String msg = result.getMessageBuffer().toString();
		assertEquals(msg, true, result.isSuccess());

		Statement stmt = this.connection.createStatement();
		ResultSet rs = stmt.executeQuery("select count(*) from bool_int_test where int_flag = -24");
		int rows = 0;
		if (rs.next())
		{
			rows = rs.getInt(1);
		}
		assertEquals("Wrong number of rows imported", 3, rows);
		rs.close();

		rs = stmt.executeQuery("select count(*) from bool_int_test where int_flag = 42");
		rows = 0;
		if (rs.next())
		{
			rows = rs.getInt(1);
		}
		assertEquals("Wrong number of rows imported", 4, rows);
		rs.close();
		stmt.close();
		assertTrue("Could not delete input file: " + importfile.getCanonicalPath(), importfile.delete());
	}

	@Test
	public void testTextClobImport()
	{
		try
		{
			File importFile  = new File(this.basedir, "import_text_clob.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\ttext_data");
			out.println("1\ttext_data_r1_c2.data");
			out.println("2\ttext_data_r2_c2.data");
			out.close();
			String data1 = "This is a CLOB string to be put into row 1";
			String data2 = "This is a CLOB string to be put into row 2";

			File datafile = new File(this.basedir, "text_data_r1_c2.data");
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(datafile), "UTF-8"));
			out.print(data1);
			out.close();

			datafile = new File(this.basedir, "text_data_r2_c2.data");
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(datafile), "UTF-8"));
			out.print(data2);
			out.close();

			StatementRunnerResult result = importCmd.execute("-- this is the import test\nwbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -clobIsFilename=true -type=text -header=true -continueonerror=false -table=clob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, text_data from clob_test order by nr");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				String data = rs.getString(2);
				assertEquals(1, nr);
				assertEquals(data1, data);
			}
			else
			{
				fail("Not enough values imported");
			}
			if (rs.next())
			{
				int nr = rs.getInt(1);
				String data = rs.getString(2);
				assertEquals(2, nr);
				assertEquals(data2, data);
			}
			else
			{
				fail("Not enough values imported");
			}
			SqlUtil.closeAll(rs, stmt);
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRegularImport()
		throws Exception
	{
		int rowCount = 10;
		try
		{
			String name = "\u0627\u0644\u0633\u0639\u0631 \u0627\u0644\u0645\u0642\u062A\u0631\u062D \u0644\u0644\u0645\u0633\u0647\u0644\u0643";
			File importFile  = new File(this.basedir, "regular_import.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			for (int i = 0; i < rowCount; i++)
			{
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			// Make sure encoding is working
			out.println("999\tUnifirst\t"+name);
			rowCount ++;

			// test for empty values (should be stored as NULL)
			out.println("  \tempty nr\tempty");
			rowCount ++;

			// Check that quote characters are used if not specified
			out.println("42\tarthur\"dent\tempty");
			rowCount ++;
			out.close();

			StatementRunnerResult result = importCmd.execute("-- this is the import test\nwbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -multiline=false -type=text -header=true -continueonerror=false -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values imported", rowCount, count);

			rs.close();

			rs = stmt.executeQuery("select lastname from junit_test where nr = 999");
			if (rs.next())
			{
				String sname = rs.getString(1);
				assertEquals("Unicode incorrectly imported", name, sname);
			}
			else
			{
				fail("Unicode row not imported");
			}
			rs.close();

			rs = stmt.executeQuery("select firstname from junit_test where nr = 42");
			if (rs.next())
			{
				String sname = rs.getString(1);
				assertEquals("Embedded quote not imported", "arthur\"dent", sname);
			}
			else
			{
				fail("Row with embedded quote not imported");
			}
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testColumnLimit()
		throws Exception
	{
		try
		{
			File importFile  = new File(this.basedir, "col_limit.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			out.println("x1\tArthur\tDent");
			out.println("x2\tZaphod\tBeeblebrox");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -colSubstring=nr=1:5 -maxLength='firstname=50,lastname=4' -type=text -header=true -continueonerror=false -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String fname = rs.getString(2);
				String lname = rs.getString(3);
				if (nr == 1)
				{
					assertEquals("Wrong lastname", "Dent", lname);
					assertEquals("Wrong firstname", "Arthur", fname);
				}
				else if (nr == 2)
				{
					assertEquals("Wrong lastname", "Beeb", lname);
					assertEquals("Wrong firstname", "Zaphod", fname);
				}
				else
				{
					fail("Wrong lines imported");
				}
			}

			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testSkipImport()
		throws Exception
	{
		try
		{
			File importFile  = new File(this.basedir, "partial_skip.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			out.println("1\tArthur\tDent");
			out.println("2\tZaphod\tBeeblebrox");
			out.close();

			StatementRunnerResult result = importCmd.execute("-- this is the import test\nwbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -filecolumns=nr,$wb_skip$,lastname -type=text -header=true -continueonerror=false -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String fname = rs.getString(2);
				String lname = rs.getString(3);
				assertNull("Firstname imported for nr=" + nr, fname);
				if (nr == 1)
				{
					assertEquals("Wrong lastname", "Dent", lname);
				}
				else if (nr == 2)
				{
					assertEquals("Wrong lastname", "Beeblebrox", lname);
				}
				else
				{
					fail("Wrong lines imported");
				}
			}

			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testPartialTextImport()
		throws Exception
	{
		int rowCount = 100;
		try
		{
			File importFile  = new File(this.basedir, "partial1.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			for (int i = 0; i < rowCount; i++)
			{
				int id = i+1;
				out.println(id + "\tFirstname" + id + "\tLastname" + id);
			}
			out.close();

			StatementRunnerResult result = importCmd.execute("-- this is the import test\nwbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -filecolumns=nr,firstname,lastname -type=text -header=true -continueonerror=false -startrow=10 -endrow=20 -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select min(nr), max(nr), count(*) from junit_test");
			if (rs.next())
			{
				int min = rs.getInt(1);
				int max = rs.getInt(2);
				int count = rs.getInt(3);
				assertEquals("Import started at wrong id", 10, min);
				assertEquals("Import ended at wrong id", 20, max);
				assertEquals("Wrong number of rows imported", 11, count);
			}
			else
			{
				fail("No data imported");
			}
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testUnmatchedFileColumn()
		throws Exception
	{
		try
		{
			File importFile  = new File(this.basedir, "partial2.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("1\tArthur");
			out.println("2\tZaphod");
			out.close();

			StatementRunnerResult result = importCmd.execute("" +
				"wbimport -encoding=utf8 " +
				"-file='" + importFile.getAbsolutePath() + "' " +
				"-type=text " +
				"-header=false " +
				"-continueonerror=false " +
				"-table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String fname = rs.getString(2);
				String lname = rs.getString(3);
				assertNull("Lastname not null for nr=" + nr, lname);
				if (nr == 1)
				{
					assertEquals("Wrong lastname", "Arthur", fname);
				}
				else if (nr == 2)
				{
					assertEquals("Wrong lastname", "Zaphod", fname);
				}
				else
				{
					fail("Wrong lines imported");
				}
			}

			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testPartialColumnTextImport()
		throws Exception
	{
		try
		{
			File importFile  = new File(this.basedir, "partial2.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			out.println("1\tArthur\tDent");
			out.println("2\tZaphod\tBeeblebrox");
			out.close();

			StatementRunnerResult result = importCmd.execute("-- this is the import test\nwbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -filecolumns=nr,firstname,lastname -importcolumns=nr,lastname -type=text -header=true -continueonerror=false -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String fname = rs.getString(2);
				String lname = rs.getString(3);
				assertNull("Firstname imported for nr=" + nr, fname);
				if (nr == 1)
				{
					assertEquals("Wrong lastname", "Dent", lname);
				}
				else if (nr == 2)
				{
					assertEquals("Wrong lastname", "Beeblebrox", lname);
				}
				else
				{
					fail("Wrong lines imported");
				}
			}

			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testMultiLineImport()
	{
		int rowCount = 10;
		try
		{
			File importFile  = new File(this.basedir, "multi.txt");
			String content = "firstname\tlastname\tnr\n" +
				"First\t\"Last\nname\"\t1\n" +
				"first2\tlast2\t2\n" +
				"first3\t\"last3\nlast3last3\"\t3\n" +
				"first4\t\"last4\tlast4\"\t4\n";

			TestUtil.writeFile(importFile, content);

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -multiline=true -quotechar='\"' -type=text -header=true -continueonerror=false -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			int count = 0;
			while (rs.next())
			{
				count ++;
				int nr = rs.getInt(1);
				String first = rs.getString(2);
				String last = rs.getString(3);
				assertEquals("Wrong nr imported", count, nr);
				if (count == 1)
				{
					assertEquals("Wrong firstname imported", "First", first);
					assertEquals("Wrong firstname imported", "Last\nname", last);
				}
				else if (count == 2)
				{
					assertEquals("Wrong firstname imported", "first2", first);
					assertEquals("Wrong firstname imported", "last2", last);
				}
				else if (count == 3)
				{
					assertEquals("Wrong firstname imported", "first3", first);
					assertEquals("Wrong firstname imported", "last3\nlast3last3", last);
				}
				else if (count == 4)
				{
					assertEquals("Wrong firstname imported", "first4", first);
					assertEquals("Wrong firstname imported", "last4\tlast4", last);
				}
			}
			assertEquals("Wrong number of rows imported", 4, count);
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testZipMultiLineImport()
	{
		try
		{
			File importFile  = new File(this.basedir, "zipmulti.txt");

			File archive = new File(this.basedir, "zipmulti.zip");
			ZipOutputFactory zout = new ZipOutputFactory(archive);
			PrintWriter out = new PrintWriter(zout.createWriter(importFile, "UTF-8"));

			out.print("nr\tfirstname\tlastname\n");
			out.print("1\tFirst\t\"Last\n");
			out.print("name\"\n");
			out.close();
			zout.done();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + archive.getAbsolutePath() + "' -multiline=true -quotechar='\"' -type=text -header=true -continueonerror=false -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			int count = -1;
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong nr imported", 1, nr);

				String first = rs.getString(2);
				assertEquals("Wrong firstname imported", "First", first);

				String last = rs.getString(3);
				assertEquals("Wrong firstname imported", "Last\nname", last);
			}
			else
			{
				fail("No data imported");
			}
			rs.close();
			stmt.close();
			if (!archive.delete())
			{
				fail("Could not delete archive! " + archive.getAbsolutePath());
			}
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testEmptyStringIsNull()
		throws Exception
	{
		try
		{
			File importFile  = new File(this.basedir, "import_empty.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			out.println("1\tFirstname\t");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -emptyStringIsNull=true -type=text -filecolumns=nr,firstname,lastname -header=false -table=junit_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr,firstname,lastname from junit_test order by nr");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong values imported", nr, 1);
				String first = rs.getString(2);
				assertEquals("Wrong firstname", "Firstname", first);

				String last = rs.getString(3);
				assertNull("Lastname not null", last);
			}
			rs.close();

			result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -emptyStringIsNull=false -type=text -filecolumns=nr,firstname,lastname -deleteTarget=true -header=false -table=junit_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			rs = stmt.executeQuery("select nr,firstname,lastname from junit_test order by nr");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong values imported", nr, 1);
				String first = rs.getString(2);
				assertEquals("Wrong firstname", "Firstname", first);

				String last = rs.getString(3);
				assertNotNull("Lastname is null", last);
			}

			if (rs.next())
			{
				fail("Too many rows");
			}
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testNullNumeric()
		throws Exception
	{
		try
		{
			File importFile  = new File(this.basedir, "import_null_numeric.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			out.println("nr\tamount\tprod_name");
			out.println("1\t1.1\tfirst");
			out.println("2\t\tsecond");
			out.println("3\t3.3\tthird");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -decimal=. -file='" + importFile.getAbsolutePath() + "' -type=text -header=true -table=numeric_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, amount, prod_name from numeric_test order by nr");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				double amount = rs.getDouble(2);
				if (rs.wasNull()) amount = -1;

				switch (nr)
				{
					case 1:
						assertEquals(1.1, amount, 0.1);
						break;
					case 2:
						assertEquals(-1, amount, 0.1);
						break;
					case 3:
						assertEquals(3.3, amount, 0.1);
				}
			}
			SqlUtil.closeAll(rs, stmt);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testMissingTarget()
		throws Exception
	{
		int rowCount = 10;
		try
		{
			File importFile  = new File(this.basedir, "dummy.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			for (int i = 0; i < 10; i++)
			{
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -type=text -filecolumns=nr,firstname,lastname -header=false -table=not_there");
			String msg = result.getMessageBuffer().toString();
			assertEquals("Export did not fail", false, result.isSuccess());
			assertEquals("No proper message in result", true, msg.indexOf("NOT_THERE not found") > -1);

			result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -type=text -header=false -table=not_there");
			msg = result.getMessageBuffer().toString();
			assertEquals("Export did not fail", false, result.isSuccess());
			assertEquals("No proper message in result", true, msg.indexOf("NOT_THERE not found") > -1);

			result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -type=text -header=true -table=not_there");
			msg = result.getMessageBuffer().toString();
			assertEquals("Export did not fail", false, result.isSuccess());
			assertEquals("No proper message in result", true, msg.indexOf("NOT_THERE not found") > -1);

			importFile.delete();
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testNoHeader()
		throws Exception
	{
		int rowCount = 10;
		try
		{
			File importFile  = new File(this.basedir, "import_no_header.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			for (int i = 0; i < rowCount; i++)
			{
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -multiline=true  -type=text -filecolumns=nr,firstname,lastname -header=false -table=junit_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values imported", rowCount, count);
			rs.close();
			rs = stmt.executeQuery("select nr,firstname,lastname from junit_test order by nr");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong values imported", nr, 0);
			}
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testColumnsFromTable()
		throws Exception
	{
		int rowCount = 10;
		try
		{
			File importFile  = new File(this.basedir, "import_tbl_cols.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			for (int i = 0; i < rowCount; i++)
			{
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -multiline=true -type=text -header=false -table=junit_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values imported", rowCount, count);
			rs.close();
			rs = stmt.executeQuery("select nr,firstname,lastname from junit_test order by nr");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong values imported", nr, 0);
			}
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testDirImport()
		throws Exception
	{
		int rowCount = 10;
		try
		{
			util.emptyBaseDirectory();

			File importFile  = new File(this.basedir, "junit_test.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			//out.println("nr\tfirstname\tlastname");
			for (int i = 0; i < rowCount; i++)
			{
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			out.close();

			out = new PrintWriter(new FileWriter(new File(this.basedir, "datatype_test.txt")));
			//out.println("int_col\tdouble_col\tchar_col\tdate_col\ttime_col\tts_col");
			out.println("42\t42.1234\tfortytwo\t2006-02-01\t22:30\t2006-04-01 22:34:14\t");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -header=false -continueonerror=false -sourcedir='" + importFile.getParent() + "' -type=text");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values in table junit_test", rowCount, count);

			rs = stmt.executeQuery("select count(*) from datatype_test");
			count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values in table datatype_test", 1, count);

			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testMultiFileSingleTableImport()
		throws Exception
	{
		int rowCount = 10;
		try
		{
			util.emptyBaseDirectory();

			File importFile  = new File(this.basedir, "multi_test1_nohead.mtxt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			for (int i = 0; i < 5; i++)
			{
				out.print("to-ignore\t");
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			out.close();

			importFile  = new File(this.basedir, "multi_test2_nohead.mtxt");
			out = new PrintWriter(new FileWriter(importFile));
			for (int i = 5; i < rowCount; i++)
			{
				out.print("to-ignore\t");
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			out.close();

			Statement stmt = this.connection.createStatementForQuery();
			stmt.executeUpdate("DELETE FROM junit_test");
			this.connection.commit();

			StatementRunnerResult result = importCmd.execute("wbimport -header=false -continueonerror=false -sourcedir='" + importFile.getParent() + "' -type=text -extension=gaga -table=junit_test");
			String msg = result.getMessageBuffer().toString();
			assertFalse(result.isSuccess());
			assertTrue(msg.indexOf("No files with extension gaga found in directory") > -1);

			result = importCmd.execute("wbimport -header=false -fileColumns=$wb_skip$,nr,firstname,lastname -continueonerror=false -sourcedir='" + importFile.getParent() + "' -type=text -extension=mtxt -table=junit_test");
			assertTrue("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values in table junit_test", rowCount, count);

			importFile  = new File(this.basedir, "multi_test1_nohead.mtxt");
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

			importFile  = new File(this.basedir, "multi_test2_nohead.mtxt");
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testMultiFileSingleTableImportWithHeader()
		throws Exception
	{
		int rowCount = 10;
		try
		{
			util.emptyBaseDirectory();

			File importFile  = new File(this.basedir, "multi_test1_head.mtxt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			out.println("gaga\tnr\tfirst_name\tlast_name");
			for (int i = 0; i < 5; i++)
			{
				out.print("to-ignore\t");
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			out.close();

			importFile  = new File(this.basedir, "multi_test2_head.mtxt");
			out = new PrintWriter(new FileWriter(importFile));
			out.println("gaga\tnr\tvorname\tnachname");
			for (int i = 5; i < rowCount; i++)
			{
				out.print("to-ignore\t");
				out.print(Integer.toString(i));
				out.print('\t');
				out.println("First" + i + "\tLastname" + i);
			}
			out.close();

			Statement stmt = this.connection.createStatementForQuery();
			stmt.executeUpdate("DELETE FROM junit_test");
			this.connection.commit();

			StatementRunnerResult result = importCmd.execute("wbimport -header=true -fileColumns=$wb_skip$,nr,firstname,lastname -continueonerror=false -sourcedir='" + importFile.getParent() + "' -type=text -extension=mtxt -table=junit_test");
			assertTrue("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values in table junit_test", rowCount, count);

			importFile  = new File(this.basedir, "multi_test1_head.mtxt");
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}

			importFile  = new File(this.basedir, "multi_test2_head.mtxt");
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testMappedImport()
		throws Exception
	{
		int rowCount = 10;
		try
		{
			File importFile  = new File(this.basedir, "import.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			out.println("nr\tpid\tfirstname\tlastname");
			for (int i = 0; i < rowCount; i++)
			{
				out.print(Integer.toString(i));
				out.println("\t42\tFirst" + i + "\tLastname" + i);
			}
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -type=text -continueonerror=true -header=true -table=junit_test");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			assertEquals("Not enough values imported", rowCount, count);
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testDataTypes()
		throws Exception
	{
		try
		{
			File importFile  = new File(this.basedir, "import_types.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			out.println("int_col\tdouble_col\tchar_col\tdate_col\ttime_col\tts_col\tnchar_col");
			out.println("42\t42.1234\tfortytwo\t2006-02-01\t22:30\t2006-04-01 22:34\tnvarchar");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -decimal='.' -type=text -header=true -table=datatype_test -dateformat='yyyy-MM-dd' -timestampformat='yyyy-MM-dd HH:mm'");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select int_col, double_col, char_col, date_col, time_col, ts_col from datatype_test");
			if (rs.next())
			{
				int i = rs.getInt(1);
				double d = rs.getDouble(2);
				String s = rs.getString(3);
				Date dt = rs.getDate(4);
				Time tt = rs.getTime(5);
				Timestamp ts = rs.getTimestamp(6);
				assertEquals("Wrong integer value", 42, i);
				assertEquals("Wrong varchar imported", "fortytwo", s);
				assertEquals("Wrong double value", 42.1234, d, 0.01);
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				java.util.Date d2 = df.parse("2006-02-01");
				assertEquals("Wrong date imported", d2, dt);

				df = new SimpleDateFormat("HH:mm");
				d2 = df.parse("22:30");
				java.sql.Time tm = new java.sql.Time(d2.getTime());
				assertEquals("Wrong time imported", tm, tt);

				df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				d2 = df.parse("2006-04-01 22:34");
				assertEquals("Wrong timestamp imported", d2, ts);
			}
			else
			{
				fail("No rows imported!");
			}
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testZippedTextBlobImport()
	{
		int rowCount = 10;
		try
		{
			File importFile  = new File(this.basedir, "blob_test.txt");

			File archive = new File(this.basedir, "blob_test.zip");
			ZipOutputFactory zout = new ZipOutputFactory(archive);
			Writer w = zout.createWriter(importFile, "UTF-8");

			PrintWriter out = new PrintWriter(w);
			out.println("nr\tbinary_data");
			out.println("1\tblob_data_r1_c1.data");
			out.close();

			w.close();
			zout.done();

			File blobarchive = new File(this.basedir, "blob_test" + RowDataConverter.BLOB_ARCHIVE_SUFFIX + ".zip");
			zout = new ZipOutputFactory(blobarchive);
			OutputStream binaryOut = zout.createOutputStream(new File("blob_data_r1_c1.data"));

			byte[] testData = new byte[1024];
			for (int i = 0; i < testData.length; i++)
			{
				testData[i] = (byte)(i % 255);
			}
			binaryOut.write(testData);
			binaryOut.close();

			zout.done();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + archive.getAbsolutePath() + "' -decimal='.' -multiline=true -encoding='UTF-8' -type=text -header=true -table=blob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", 1, nr);

				Object blob = rs.getObject(2);
				assertNotNull("No blob data imported", blob);
				if (blob instanceof byte[])
				{
					byte[] retrievedData = (byte[])blob;
					assertEquals("Wrong blob size importee", 1024, retrievedData.length);
					assertEquals("Wrong content of blob data", retrievedData[0], 0);
					assertEquals("Wrong content of blob data", retrievedData[1], 1);
					assertEquals("Wrong content of blob data", retrievedData[2], 2);
					assertEquals("Wrong content of blob data", retrievedData[3], 3);
				}
				else
				{
					fail("Wrong blob data returned");
				}
			}
			else
			{
				fail("No rows imported");
			}
			rs.close();
			stmt.close();
			if (!archive.delete())
			{
				fail("Could not delete input file: " + archive.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testVerboseXmlImport()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             " \n" +
             "    <generating-sql> \n" +
             "    <![CDATA[ \n" +
             "    select id, lastname, firstname from person \n" +
             "    ]]> \n" +
             "    </generating-sql> \n" +
             " \n" +
             "    <wb-tag-format>long</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>junit_test</table-name> \n" +
             "    <column-count>3</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>LASTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>FIRSTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<row-data row-num=\"1\">" +
						 "  <column-data index=\"0\">1</column-data>" +
						 "  <column-data index=\"1\">Dent</column-data>" +
						 "  <column-data index=\"2\">Arthur</column-data>" +
						 "</row-data> \n" +
             "<row-data row-num=\"1\">" +
						 "  <column-data index=\"0\">2</column-data>" +
						 "  <column-data index=\"1\">Beeblebrox</column-data>" +
						 "  <column-data index=\"2\">Zaphod</column-data>" +
						 "</row-data> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "xml_verbose_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
//			System.out.println("cmd=" + cmd);
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			int rowCount = 0;

			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", rowCount, nr);
			}
			assertEquals("Wrong number of rows", rowCount, 2);
			rs.close();
			stmt.close();
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testXmlImport()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             " \n" +
             "    <generating-sql> \n" +
             "    <![CDATA[ \n" +
             "    select id, lastname, firstname from person \n" +
             "    ]]> \n" +
             "    </generating-sql> \n" +
             " \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>junit_test</table-name> \n" +
             "    <column-count>3</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>LASTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>FIRSTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>Dent</cd><cd>Arthur</cd></rd> \n" +
             "<rd><cd>2</cd><cd>Beeblebrox</cd><cd>Zaphod</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
			//System.out.println("cmd=" + cmd);
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			int rowCount = 0;

			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", rowCount, nr);
			}
			assertEquals("Wrong number of rows", rowCount, 2);
			rs.close();
			stmt.close();
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testXmlImportChangeStructure()
	{
		String xml = "<?xml version=\"1.1\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             "    <created>2008-06-06 21:25:24.828 CEST</created> \n" +
             "    <jdbc-driver>PostgreSQL Native Driver</jdbc-driver> \n" +
             "    <jdbc-driver-version>PostgreSQL 8.3 JDBC3 with SSL (build 603)</jdbc-driver-version> \n" +
             "    <connection>User=thomas, Database=wbtest, URL=jdbc:postgresql://localhost/wbtest</connection> \n" +
             "    <schema></schema> \n" +
             "    <catalog>wbtest</catalog> \n" +
             "    <database-product-name>PostgreSQL</database-product-name> \n" +
             "    <database-product-version>8.3.1</database-product-version> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>info</table-name> \n" +
             "    <column-count>5</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>id</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>int4</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>firstname</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>varchar(50)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>lastname</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>varchar(50)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"3\"> \n" +
             "      <column-name>birthday</column-name> \n" +
             "      <java-class>java.sql.Date</java-class> \n" +
             "      <java-sql-type-name>DATE</java-sql-type-name> \n" +
             "      <java-sql-type>91</java-sql-type> \n" +
             "      <dbms-data-type>date</dbms-data-type> \n" +
             "      <data-format>yyyy-MM-dd</data-format> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"4\"> \n" +
             "      <column-name>salary</column-name> \n" +
             "      <java-class>java.math.BigDecimal</java-class> \n" +
             "      <java-sql-type-name>NUMERIC</java-sql-type-name> \n" +
             "      <java-sql-type>2</java-sql-type> \n" +
             "      <dbms-data-type>numeric(12,2)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>Arthur</cd><cd>Dent</cd><cd longValue=\"-880682400000\">1942-02-04</cd><cd>100.41</cd></rd> \n" +
             "<rd><cd>4</cd><cd>Mary</cd><cd>Moviestar</cd><cd longValue=\"946767600000\">2000-01-02</cd><cd>42.42</cd></rd> \n" +
             "<rd><cd>2</cd><cd>Zaphod</cd><cd>Beeblebrox</cd><cd longValue=\"-299466000000\">1960-07-06</cd><cd>123.45</cd></rd> \n" +
             "<rd><cd>3</cd><cd>Tricia</cd><cd>McMillian</cd><cd longValue=\"334620000000\">1980-08-09</cd><cd>567.89</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";

		String sql = "CREATE TABLE info \n" +
             "( \n" +
             "   salary    decimal(12,2), \n" +
             "   birthday  date, \n" +
             "   Lastname varchar(50)  NULL, \n" +
             "   id        int4         NOT NULL, \n" +
             "   FIRSTNAME  varchar(50)  NULL \n" +
             ")";
		try
		{
			File xmlFile = new File(this.basedir, "xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			Statement stmt = this.connection.createStatement();
			stmt.executeUpdate(sql);

			String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=info";
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			ResultSet rs = stmt.executeQuery("select count(*) from info");
			int rowCount = 0;

			if (rs.next())
			{
				rowCount = rs.getInt(1);
			}

			assertEquals("Wrong number of rows", rowCount, 4);
			rs.close();
			stmt.close();


			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testXmlImportCreateTable()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             " \n" +
             "    <generating-sql> \n" +
             "    <![CDATA[ \n" +
             "    select id, lastname, firstname from person \n" +
             "    ]]> \n" +
             "    </generating-sql> \n" +
             " \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>not_there_table</table-name> \n" +
             "    <column-count>3</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>LASTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>FIRSTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>Dent</cd><cd>Arthur</cd></rd> \n" +
             "<rd><cd>2</cd><cd>Beeblebrox</cd><cd>Zaphod</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -createTarget=true";
			//System.out.println("cmd=" + cmd);
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from not_there_table");
			int rowCount = 0;

			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", rowCount, nr);
			}
			assertEquals("Wrong number of rows", rowCount, 2);
			rs.close();
			stmt.close();
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPartialXmlImport()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             " \n" +
             "    <generating-sql> \n" +
             "    <![CDATA[ \n" +
             "    select id, lastname, firstname from person \n" +
             "    ]]> \n" +
             "    </generating-sql> \n" +
             " \n" +
             "    <created>2006-07-29 23:31:40.366 CEST</created> \n" +
             "    <jdbc-driver>HSQL Database Engine Driver</jdbc-driver> \n" +
             "    <jdbc-driver-version>1.8.0</jdbc-driver-version> \n" +
             "    <connection>User=SA, URL=jdbc:hsqldb:d:/daten/db/hsql18/test</connection> \n" +
             "    <database-product-name>HSQL Database Engine</database-product-name> \n" +
             "    <database-product-version>1.8.0</database-product-version> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <!-- The following information was retrieved from the JDBC driver's ResultSetMetaData --> \n" +
             "    <!-- column-name is retrieved from ResultSetMetaData.getColumnName() --> \n" +
             "    <!-- java-class is retrieved from ResultSetMetaData.getColumnClassName() --> \n" +
             "    <!-- java-sql-type-name is the constant's name from java.sql.Types --> \n" +
             "    <!-- java-sql-type is the constant's numeric value from java.sql.Types as returned from ResultSetMetaData.getColumnType() --> \n" +
             "    <!-- dbms-data-type is retrieved from ResultSetMetaData.getColumnTypeName() --> \n" +
             " \n" +
             "    <!-- For date and timestamp types, the internal long value obtained from java.util.Date.getTime() \n" +
             "         is written as an attribute to the <column-data> tag. That value can be used \n" +
             "         to create a java.util.Date() object directly, without the need to parse the actual tag content. \n" +
             "         If Java is not used to parse this file, the date/time format used to write the data \n" +
             "         is provided in the <data-format> tag of the column definition \n" +
             "    --> \n" +
             " \n" +
             "    <table-name>junit_test</table-name> \n" +
             "    <column-count>3</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>LASTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>FIRSTNAME</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n";
		String xmlEnd = "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "xml_import2.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			for (int i=0; i < 100; i++)
			{
				int id = i + 1;
				out.write("<rd><cd>" + id + "</cd><cd>Lastname" + id + "</cd><cd>Firstname" + id + "</cd></rd>\n");
			}
			out.write(xmlEnd);
			out.close();

			String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -startRow = 15 -endrow = 24 -table=junit_test";
			//System.out.println("cmd=" + cmd);
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select min(nr), max(nr), count(*) from junit_test");
			if (rs.next())
			{
				int min = rs.getInt(1);
				int max = rs.getInt(2);
				int count = rs.getInt(3);
				assertEquals("Import started at wrong id", 15, min);
				assertEquals("Import ended at wrong id", 24, max);
				assertEquals("Wrong number of rows imported", 10, count);
			}
			else
			{
				fail("No data imported");
			}
			rs.close();
			stmt.close();
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEncodedBlobImport()
		throws Exception
	{
		try
		{
			util.copyResourceFile(this, "encoded_blob_input.xml");

			File xmlFile = new File(this.basedir, "encoded_blob_input.xml");

			StatementRunnerResult result = importCmd.execute("wbimport -encoding='ISO-8859-1' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=blob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test");

			String xmlContent = FileUtil.readFile(xmlFile, "ISO-8859-1");

			int id1 = Integer.parseInt(TestUtil.getXPathValue(xmlContent, "/wb-export/data/row-data[1]/column-data[1]"));
			String blob1 = TestUtil.getXPathValue(xmlContent, "/wb-export/data/row-data[1]/column-data[2]");

			int id2 = Integer.parseInt(TestUtil.getXPathValue(xmlContent, "/wb-export/data/row-data[2]/column-data[1]"));
			String blob2 = TestUtil.getXPathValue(xmlContent, "/wb-export/data/row-data[2]/column-data[2]");

			int rowCount = 0;
			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);

				Object blob = rs.getObject(2);
				assertNotNull("No blob data imported", blob);

				String blobString = Base64.encodeBytes((byte[])blob);
				if (nr == id1)
				{
					assertEquals(blob1, blobString);
				}
				else if (nr == id2)
				{
					assertEquals(blob2, blobString);
				}
			}
			rs.close();
			stmt.close();
			assertEquals(2, rowCount);
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testXmlBlobImport()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             " \n" +
             "    <generating-sql> \n" +
             "    <![CDATA[ \n" +
             "    select * from blob_test \n" +
             "    ]]> \n" +
             "    </generating-sql> \n" +
             " \n" +
             "    <created>2006-07-30 00:05:59.316 CEST</created> \n" +
             "    <jdbc-driver>HSQL Database Engine Driver</jdbc-driver> \n" +
             "    <jdbc-driver-version>1.8.0</jdbc-driver-version> \n" +
             "    <connection>User=SA, URL=jdbc:hsqldb:d:/daten/db/hsql18/test</connection> \n" +
             "    <database-product-name>HSQL Database Engine</database-product-name> \n" +
             "    <database-product-version>1.8.0</database-product-version> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>blob_test</table-name> \n" +
             "    <column-count>2</column-count> \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>BINARY_DATA</column-name> \n" +
             "      <java-class>byte[]</java-class> \n" +
             "      <java-sql-type-name>BINARY</java-sql-type-name> \n" +
             "      <java-sql-type>-2</java-sql-type> \n" +
             "      <dbms-data-type>BINARY</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd dataFile=\"test_r1_c2.data\"/></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			File dataFile = new File(this.basedir, "test_r1_c2.data");
			FileOutputStream binaryOut = new FileOutputStream(dataFile);
			byte[] testData = new byte[1024];
			for (int i = 0; i < testData.length; i++)
			{
				testData[i] = (byte)(i % 255);
			}
			binaryOut.write(testData);
			binaryOut.close();

			StatementRunnerResult result = importCmd.execute("wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=blob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test");
			int rowCount = 0;

			if (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", 1, nr);

				Object blob = rs.getObject(2);
				assertNotNull("No blob data imported", blob);
				if (blob instanceof byte[])
				{
					byte[] retrievedData = (byte[])blob;
					assertEquals("Wrong blob size imported", 1024, retrievedData.length);
					assertEquals("Wrong content of blob data", retrievedData[0], 0);
					assertEquals("Wrong content of blob data", retrievedData[1], 1);
					assertEquals("Wrong content of blob data", retrievedData[2], 2);
					assertEquals("Wrong content of blob data", retrievedData[3], 3);
				}
				else
				{
					fail("Wrong blob data returned");
				}
			}
			else
			{
				fail("Not enough data imported");
			}
			rs.close();
			stmt.close();
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testXmlClobImport()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             " \n" +
             "    <generating-sql> \n" +
             "    <![CDATA[ \n" +
             "    select * from blob_test \n" +
             "    ]]> \n" +
             "    </generating-sql> \n" +
             " \n" +
             "    <created>2006-07-30 00:05:59.316 CEST</created> \n" +
             "    <jdbc-driver>HSQL Database Engine Driver</jdbc-driver> \n" +
             "    <jdbc-driver-version>1.8.0</jdbc-driver-version> \n" +
             "    <connection>User=SA, URL=jdbc:hsqldb:d:/daten/db/hsql18/test</connection> \n" +
             "    <database-product-name>HSQL Database Engine</database-product-name> \n" +
             "    <database-product-version>1.8.0</database-product-version> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>clob_test</table-name> \n" +
             "    <column-count>2</column-count> \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>TEXT_DATA</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>LONGVARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>-1</java-sql-type> \n" +
             "      <dbms-data-type>LONGVARCHAR</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd dataFile=\"test_r1_c2.data\"/></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "xml_import.xml");
			BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false));
			out.write(xml);
			out.close();

			File datafile = new File(this.basedir, "test_r1_c2.data");
			String data1 = "This is a CLOB string to be put into row 1";

			Writer pw = EncodingUtil.createWriter(datafile, "UTF-8", false);
			pw.write(data1);
			pw.close();

			StatementRunnerResult result = importCmd.execute("wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=clob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, text_data from clob_test");
			int rowCount = 0;

			if (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", 1, nr);

				String data = rs.getString(2);
				assertEquals(data, data);
			}
			else
			{
				fail("Not enough data imported");
			}
			rs.close();
			stmt.close();
			if (!xmlFile.delete())
			{
				fail("Could not delete input file: " + xmlFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testCommit()
	{
		try
		{
			util.emptyBaseDirectory();
			File dbFile = new File(util.getBaseDir(), "commit_test");
			WbConnection wb = util.getConnection(dbFile);
			importCmd.setConnection(wb);

			Statement stmt = wb.createStatement();
			stmt.executeUpdate("CREATE TABLE junit_test (nr integer, firstname varchar(100), lastname varchar(100))");
			wb.commit();
			stmt.close();

			String data = "nr;firstname;lastname\n1;Arthur;Dent\n2;Zaphod;Beeblebrox\n";
			File dataFile = new File(util.getBaseDir(), "commit_test_data.txt");
			FileWriter w = new FileWriter(dataFile);
			w.write(data);
			w.close();

			String cmd = "wbimport -file='" + dataFile.getAbsolutePath() + "' -type=text -delimiter=';' -table=junit_test -header=true";
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());
			wb.disconnect();

			// Shutdown and restart the engine to make sure the data was committed
			wb = util.getConnection(dbFile);
			stmt = wb.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test");
			int row = 0;
			while (rs.next())
			{
				row ++;
				if (row == 1)
				{
					int nr = rs.getInt(1);
					assertEquals("Wrong data imported", 1, nr);
					String firstname = rs.getString(2);
					assertEquals("Wrong data imported", "Arthur", firstname);
					String lastname = rs.getString(3);
					assertEquals("Wrong data imported", "Dent", lastname);
				}
				else if (row == 2)
				{
					int nr = rs.getInt(1);
					assertEquals("Wrong data imported", 2, nr);
					String firstname = rs.getString(2);
					assertEquals("Wrong data imported", "Zaphod", firstname);
					String lastname = rs.getString(3);
					assertEquals("Wrong data imported", "Beeblebrox", lastname);
				}
				else
				{
					fail("Wrong number of rows imported");
				}
			}
			rs.close();
			wb.disconnect();

			// Make sure the import command has released all file handles
			if (!dataFile.delete())
			{
				fail("Could not delete dataFile=" + dataFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testNoCommit()
	{
		try
		{
			util.emptyBaseDirectory();
			File dbFile = new File(util.getBaseDir(), "no_commit_test");
			WbConnection wb = util.getConnection(dbFile);
			importCmd.setConnection(wb);

			Statement stmt = wb.createStatement();
			stmt.executeUpdate("CREATE TABLE junit_test (nr integer, firstname varchar(100), lastname varchar(100))");
			wb.commit();
			stmt.close();

			String data = "nr;firstname;lastname\n1;Arthur;Dent\n2;Zaphod;Beeblebrox\n";
			File dataFile = new File(util.getBaseDir(), "no_commit_test_data.txt");
			FileWriter w = new FileWriter(dataFile);
			w.write(data);
			w.close();

			String cmd = "wbimport -file='" + dataFile.getAbsolutePath() + "' -type=text -delimiter=';' -commitEvery=none -table=junit_test -header=true";
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());
			wb.disconnect();

			// Shutdown and restart the engine to make sure the data was committed
			wb = util.getConnection(dbFile);
			stmt = wb.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from junit_test");
			int count = -1;
			if (rs.next()) count = rs.getInt(1);
			rs.close();
			assertEquals("Wrong number of rows in table", 0, count);
			wb.disconnect();

			if (!dataFile.delete())
			{
				fail("Could not delete datafile: " + dataFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testZippedXmlBlobImport()
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             " \n" +
             "    <generating-sql> \n" +
             "    <![CDATA[ \n" +
             "    select * from blob_test \n" +
             "    ]]> \n" +
             "    </generating-sql> \n" +
             " \n" +
             "    <created>2006-07-30 00:05:59.316 CEST</created> \n" +
             "    <jdbc-driver>HSQL Database Engine Driver</jdbc-driver> \n" +
             "    <jdbc-driver-version>1.8.0</jdbc-driver-version> \n" +
             "    <connection>User=SA, URL=jdbc:hsqldb:d:/daten/db/hsql18/test</connection> \n" +
             "    <database-product-name>HSQL Database Engine</database-product-name> \n" +
             "    <database-product-version>1.8.0</database-product-version> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <!-- The following information was retrieved from the JDBC driver's ResultSetMetaData --> \n" +
             "    <!-- column-name is retrieved from ResultSetMetaData.getColumnName() --> \n" +
             "    <!-- java-class is retrieved from ResultSetMetaData.getColumnClassName() --> \n" +
             "    <!-- java-sql-type-name is the constant's name from java.sql.Types --> \n" +
             "    <!-- java-sql-type is the constant's numeric value from java.sql.Types as returned from ResultSetMetaData.getColumnType() --> \n" +
             "    <!-- dbms-data-type is retrieved from ResultSetMetaData.getColumnTypeName() --> \n" +
             " \n" +
             "    <!-- For date and timestamp types, the internal long value obtained from java.util.Date.getTime() \n" +
             "         is written as an attribute to the <column-data> tag. That value can be used \n" +
             "         to create a java.util.Date() object directly, without the need to parse the actual tag content. \n" +
             "         If Java is not used to parse this file, the date/time format used to write the data \n" +
             "         is provided in the <data-format> tag of the column definition \n" +
             "    --> \n" +
             " \n" +
             "    <table-name>blob_test</table-name> \n" +
             "    <column-count>2</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>NR</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>BINARY_DATA</column-name> \n" +
             "      <java-class>byte[]</java-class> \n" +
             "      <java-sql-type-name>BINARY</java-sql-type-name> \n" +
             "      <java-sql-type>-2</java-sql-type> \n" +
             "      <dbms-data-type>BINARY</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd dataFile=\"test_r1_c2.data\"/></rd> \n" +
             "</data> \n" +
             "</wb-export>";
		try
		{
			File xmlFile = new File(this.basedir, "xml_import.xml");

			File archive = new File(this.basedir, "blob_test.zip");
			ZipOutputFactory zout = new ZipOutputFactory(archive);
			Writer w = zout.createWriter(xmlFile, "UTF-8");
			w.write(xml);
			w.close();
			zout.done();

			File blobarchive = new File(this.basedir, "blob_test" + RowDataConverter.BLOB_ARCHIVE_SUFFIX + ".zip");
			zout = new ZipOutputFactory(blobarchive);
			OutputStream binaryOut = zout.createOutputStream(new File("test_r1_c2.data"));

			byte[] testData = new byte[1024];
			for (int i = 0; i < testData.length; i++)
			{
				testData[i] = (byte)(i % 255);
			}
			binaryOut.write(testData);
			binaryOut.close();
			zout.done();

			StatementRunnerResult result = importCmd.execute("wbimport -encoding='UTF-8' -file='" + archive.getAbsolutePath() + "' -type=xml -table=blob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test");
			int rowCount = 0;

			if (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", 1, nr);

				Object data = rs.getObject(2);
				Object blob = rs.getObject(2);
				assertNotNull("No blob data imported", blob);
				if (blob instanceof byte[])
				{
					byte[] retrievedData = (byte[])blob;
					assertEquals("Wrong blob size imported", 1024, retrievedData.length);
					assertEquals("Wrong content of blob data", retrievedData[0], 0);
					assertEquals("Wrong content of blob data", retrievedData[1], 1);
					assertEquals("Wrong content of blob data", retrievedData[2], 2);
					assertEquals("Wrong content of blob data", retrievedData[3], 3);
				}
				else
				{
					fail("Wrong blob data returned");
				}
			}
			else
			{
				fail("Not enough data imported");
			}
			rs.close();
			stmt.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testTextBlobImport()
	{
		try
		{
			File importFile  = new File(this.basedir, "blob_test.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			out.println("nr\tbinary_data");
			out.println("1\tblob_data_r1_c1.data");
			out.close();

			FileOutputStream binaryOut = new FileOutputStream(new File(this.basedir, "blob_data_r1_c1.data"));
			byte[] testData = new byte[1024];
			for (int i = 0; i < testData.length; i++)
			{
				testData[i] = (byte)(i % 255);
			}
			binaryOut.write(testData);
			binaryOut.close();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -decimal='.' -type=text -header=true -table=blob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", 1, nr);

				Object blob = rs.getObject(2);
				assertNotNull("No blob data imported", blob);
				if (blob instanceof byte[])
				{
					byte[] retrievedData = (byte[])blob;
					assertEquals("Wrong blob size imported", 1024, retrievedData.length);
					assertEquals("Wrong content of blob data", retrievedData[0], 0);
					assertEquals("Wrong content of blob data", retrievedData[1], 1);
					assertEquals("Wrong content of blob data", retrievedData[2], 2);
					assertEquals("Wrong content of blob data", retrievedData[3], 3);
				}
				else
				{
					fail("Wrong blob data returned");
				}
			}
			else
			{
				fail("No rows imported");
			}
			rs.close();
			stmt.close();
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testEncodedBlob()
	{
		try
		{
			File importFile  = new File(this.basedir, "blob2_test.txt");
			PrintWriter out = new PrintWriter(new FileWriter(importFile));
			byte[] testData = new byte[1024];
			for (int i = 0; i < testData.length; i++)
			{
				testData[i] = (byte)(i % 255);
			}

			out.println("nr\tbinary_data");
			out.print("1\t");
			out.println(Base64.encodeBytes(testData));
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -decimal='.' -type=text -header=true -table=blob_test -blobType=base64");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", 1, nr);

				Object blob = rs.getObject(2);
				assertNotNull("No blob data imported", blob);
				if (blob instanceof byte[])
				{
					byte[] retrievedData = (byte[])blob;
					assertEquals("Wrong blob size imported", 1024, retrievedData.length);
					assertEquals("Wrong content of blob data", retrievedData[0], 0);
					assertEquals("Wrong content of blob data", retrievedData[1], 1);
					assertEquals("Wrong content of blob data", retrievedData[2], 2);
					assertEquals("Wrong content of blob data", retrievedData[3], 3);
				}
				else
				{
					fail("Wrong blob data returned");
				}
			}
			else
			{
				fail("No rows imported");
			}
			rs.close();
			stmt.close();
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testBadFile()
	{
		try
		{
			File importFile  = new File(this.basedir, "bad_import.txt");
			File badFile = new File(this.basedir, "import.bad");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			out.println("1\tMary\tMoviestar");
			out.println("2\tHarry\tHandsome");
			out.println("1\tZaphod\tBeeblebrox");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -multiline=false -type=text -header=true -continueonerror=true -table=junit_test_pk -badFile='" + badFile.getCanonicalPath() + "'");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			assertEquals("Bad file not created", true, badFile.exists());

			BufferedReader r = new BufferedReader(new FileReader(badFile));
			String line = r.readLine();
			r.close();
			assertEquals("Wrong record rejected", "1\tZaphod\tBeeblebrox", line);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr from junit_test_pk order by nr");
			int row = 0;
			while (rs.next())
			{
				int id = rs.getInt(1);
				if (row == 0)
				{
					assertEquals("Wrong ID imported", 1, id);
				}
				else if (row == 1)
				{
					assertEquals("Wrong ID imported", 2, id);
				}
				else
				{
					fail("Too many rows");
				}
				row ++;
			}
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
			if (!badFile.delete())
			{
				fail("Could not delete bad file: " + badFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPartialFixedWidthImport()
	{
		try
		{
			File importFile = new File(this.basedir, "fixed_import.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("  1      MaryMoviestar      ");
			out.println("  2     HarryHandsome       ");
			out.println("  3Zaphod    Beeblebrox     ");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -trimValues=true -file='" + importFile.getAbsolutePath() + "' -multiline=false -type=text -header=false -filecolumns=nr,firstname,lastname -importcolumns=nr,lastname -columnWidths='nr=3,firstname=10,lastname=15' -continueonerror=true -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr,firstname,lastname from junit_test_pk order by nr");
			while (rs.next())
			{
				int id = rs.getInt(1);
				String firstname = rs.getString(2);
				String lastname = rs.getString(3);
				if (id == 1)
				{
					assertNull("Firstname not null", firstname);
					assertEquals("Wrong Lastname imported", "Moviestar", firstname);
				}
				else if (id == 2)
				{
					assertNull("Firstname not null", firstname);
					assertEquals("Wrong Lastname imported", "Handsome", firstname);
				}
				else if (id == 3)
				{
					assertEquals("Wrong Firstname", "Zaphod", firstname);
					assertEquals("Wrong Lastname imported", "Beeblebrox", lastname);
				}
				else
				{
					fail("Wrong id retrieved");
				}
			}
			rs.close();
			stmt.close();
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFixedWidthImport()
	{
		try
		{
			File importFile = new File(this.basedir, "fixed_import.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("  1      MaryMoviestar      ");
			out.println("  2     HarryHandsome       ");
			out.println("  3Zaphod    Beeblebrox     ");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -trimValues=true -file='" + importFile.getAbsolutePath() + "' -multiline=false -type=text -header=false -filecolumns=nr,firstname,lastname -columnWidths='nr=3,firstname=10,lastname=15' -continueonerror=true -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr,firstname,lastname from junit_test_pk order by nr");
			while (rs.next())
			{
				int id = rs.getInt(1);
				String firstname = rs.getString(2);
				String lastname = rs.getString(3);
				if (id == 1)
				{
					assertEquals("Wrong Firstname imported", "Mary", firstname);
					assertEquals("Wrong Lastname imported", "Moviestar", firstname);
				}
				else if (id == 2)
				{
					assertEquals("Wrong Firstname imported", "Harry", firstname);
					assertEquals("Wrong Lastname imported", "Handsome", firstname);
				}
				else if (id == 2)
				{
					assertEquals("Wrong Firstname imported", "Zaphod", firstname);
					assertEquals("Wrong Lastname imported", "Beeblebrox", firstname);
				}
				else
				{
					fail("Wrong id retrieved");
				}
			}

			rs.close();
			stmt.executeUpdate("delete from junit_test");
			connection.commit();
			stmt.close();

			result = importCmd.execute("wbimport -encoding=utf8 -trimValues=false -file='" + importFile.getAbsolutePath() + "' -multiline=false -type=text -header=false -filecolumns=nr,firstname,lastname -columnWidths='nr=3,firstname=10,lastname=15' -continueonerror=true -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			stmt = this.connection.createStatementForQuery();
			rs = stmt.executeQuery("select nr,firstname,lastname from junit_test_pk order by nr");
			while (rs.next())
			{
				int id = rs.getInt(1);
				String firstname = rs.getString(2);
				String lastname = rs.getString(3);
				if (id == 1)
				{
					assertEquals("Wrong Firstname imported", "      Mary", firstname);
					assertEquals("Wrong Lastname imported", "Moviestar      ", firstname);
				}
				else if (id == 2)
				{
					assertEquals("Wrong Firstname imported", "     Harry", firstname);
					assertEquals("Wrong Lastname imported", "Handsome       ", firstname);
				}
				else if (id == 2)
				{
					assertEquals("Wrong Firstname imported", "Zaphod    ", firstname);
					assertEquals("Wrong Lastname imported", "Beeblebrox     ", firstname);
				}
				else
				{
					fail("Wrong id retrieved");
				}
			}

			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDeleteTargetFails()
	{
		try
		{
			Statement stmt = this.connection.createStatement();
			stmt.executeUpdate("create table parent_table (id integer primary key, some_val integer)");
			stmt.executeUpdate("insert into parent_table (id, some_val) values (1, 1)");
			stmt.executeUpdate("insert into parent_table (id, some_val) values (2, 1)");
			stmt.executeUpdate("insert into parent_table (id, some_val) values (3, 1)");

			stmt.executeUpdate("create table child (id integer primary key, parent_id integer, data integer, foreign key (parent_id) references parent_table(id))");
			stmt.executeUpdate("insert into child (id, parent_id, data) values (1, 1, 1)");
			stmt.executeUpdate("insert into child (id, parent_id, data) values (2, 1, 2)");
			this.connection.commit();

			File importFile  = new File(this.basedir, "imp_delete_test.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("id\tsome_val");
			out.println("1\t42");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -type=text -header=true -continueonerror=false -table=parent_table -deleteTarget=true");
			assertEquals("Import did not fail", false, result.isSuccess());
			String msg = result.getMessageBuffer().toString();
//			System.out.println(" ***** message=" + msg);
			assertEquals("No error reported", true, msg.toLowerCase().indexOf("integrity constraint violation") > 0);

			ResultSet rs = stmt.executeQuery("select count(*) from parent_table");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			SqlUtil.closeAll(rs, stmt);
			assertEquals("Wrong number of rows", 3, count);
			if (!importFile.delete())
			{
				fail("Could not delete input file: " + importFile.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEmptyFileWithContinue()
		throws Exception
	{
		File importFile  = new File(this.basedir, "bad_import.txt");
		importFile.createNewFile();

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -type=text -header=false -continueonerror=true -table=junit_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testEmptyFileCondition()
		throws Exception
	{
		WbFile importFile  = new WbFile(this.basedir, "bad_import.txt");
		importFile.createNewFile();

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -type=text -header=false -emptyFile=ignore -table=junit_test");
		assertTrue("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess());

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}

		importFile.createNewFile();
		result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -type=text -header=false -continueOnError=false -emptyFile=fail -table=junit_test");
		String error = result.getMessageBuffer().toString();
		String expected = ResourceMgr.getFormattedString("ErrImportFileEmpty", importFile.getFullPath());
		assertFalse(result.isSuccess());
		assertEquals(expected, error);

		importFile.createNewFile();
		result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -type=text -header=false -continueOnError=false -emptyFile=warning -table=junit_test");
		error = result.getMessageBuffer().toString();
		expected = ResourceMgr.getFormattedString("ErrImportFileEmpty", importFile.getFullPath());
		assertTrue(result.isSuccess());
		assertTrue(result.hasWarning());
		assertEquals(expected, error);
	}


	@Test
	public void testDependencyXmlImport()
	{
		try
		{
			File f1 = new File(basedir, "file1.xml");
			File f2 = new File(this.basedir, "file2.xml");
			File f3 = new File(this.basedir, "file3.xml");

			String f1_content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>A_CHILD1_CHILD</table-name> \n" +
             "    <column-count>3</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>ID</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>CHILD_ID</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>INFO</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(50)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>1</cd><cd>info_1</cd></rd> \n" +
             "<rd><cd>2</cd><cd>2</cd><cd>info_2</cd></rd> \n" +
             "<rd><cd>3</cd><cd>3</cd><cd>info_3</cd></rd> \n" +
             "<rd><cd>4</cd><cd>4</cd><cd>info_3</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";

			String f2_content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>CHILD1</table-name> \n" +
             "    <column-count>3</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>ID</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>BASE_ID</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"2\"> \n" +
             "      <column-name>INFO</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(50)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>1</cd><cd>info</cd></rd> \n" +
             "<rd><cd>2</cd><cd>2</cd><cd>info</cd></rd> \n" +
             "<rd><cd>3</cd><cd>1</cd><cd>info</cd></rd> \n" +
             "<rd><cd>4</cd><cd>2</cd><cd>info</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";

			String f3_content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "<wb-export> \n" +
             "  <meta-data> \n" +
             "    <created>2007-09-15 19:31:31.718 CEST</created> \n" +
             "    <jdbc-driver>H2 JDBC Driver</jdbc-driver> \n" +
             "    <jdbc-driver-version>1.0.57 (2007-08-25)</jdbc-driver-version> \n" +
             "    <connection>User=, Catalog=TESTDEPENDENCYTEXTIMPORT, URL=jdbc:h2:mem:testDependencyTextImport</connection> \n" +
             "    <database-product-name>H2</database-product-name> \n" +
             "    <database-product-version>1.0.57 (2007-08-25)</database-product-version> \n" +
             "    <wb-tag-format>short</wb-tag-format> \n" +
             "  </meta-data> \n" +
             " \n" +
             "  <table-def> \n" +
             "    <table-name>ZZBASE</table-name> \n" +
             "    <column-count>2</column-count> \n" +
             " \n" +
             "    <column-def index=\"0\"> \n" +
             "      <column-name>ID</column-name> \n" +
             "      <java-class>java.lang.Integer</java-class> \n" +
             "      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
             "      <java-sql-type>4</java-sql-type> \n" +
             "      <dbms-data-type>INTEGER</dbms-data-type> \n" +
             "    </column-def> \n" +
             "    <column-def index=\"1\"> \n" +
             "      <column-name>INFO</column-name> \n" +
             "      <java-class>java.lang.String</java-class> \n" +
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(50)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n" +
             "<rd><cd>1</cd><cd>info</cd></rd> \n" +
             "<rd><cd>2</cd><cd>info</cd></rd> \n" +
             "</data> \n" +
             "</wb-export>";

			FileWriter out = new FileWriter(f1);
			out.write(f1_content);
			out.close();

			out = new FileWriter(f2);
			out.write(f2_content);
			out.close();

			out = new FileWriter(f3);
			out.write(f3_content);
			out.close();

			WbFile f = new WbFile(basedir);
			StatementRunnerResult result = importCmd.execute("wbimport -sourcedir='" + f.getFullPath() + "' -type=xml -checkDependencies=true");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from zzbase");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Wrong row count for zzbase", 2, count);
			}
			else
			{
				fail("No rows in zzbase");
			}

			rs = stmt.executeQuery("select count(*) from child1");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Wrong row count for child1", 4, count);
			}
			else
			{
				fail("No rows in zzbase");
			}

			rs = stmt.executeQuery("select count(*) from a_child1_child");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Wrong row count for a_child1_child", 4, count);
			}
			else
			{
				fail("No rows in zzbase");
			}

			if (!f1.delete())
			{
				fail("Could not delete input file: " + f1.getCanonicalPath());
			}
			if (!f2.delete())
			{
				fail("Could not delete input file: " + f2.getCanonicalPath());
			}
			if (!f3.delete())
			{
				fail("Could not delete input file: " + f3.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDependencyTextImport()
	{
		try
		{
			File f1 = new File(basedir, "a_child1_child.txt");
			File f2 = new File(this.basedir, "child1.txt");
			File f3 = new File(this.basedir, "zzbase.txt");

			FileWriter out = new FileWriter(f1);
			out.write("id\tchild_id\tinfo\n");
			out.write("1\t1\tinfo_1\n");
			out.write("2\t2\tinfo_2\n");
			out.write("3\t3\tinfo_3\n");
			out.write("4\t4\tinfo_3\n");
			out.close();

			out = new FileWriter(f2);
			out.write("id\tbase_id\tinfo\n");
			out.write("1\t1\tinfo\n");
			out.write("2\t2\tinfo\n");
			out.write("3\t1\tinfo\n");
			out.write("4\t2\tinfo\n");
			out.close();

			out = new FileWriter(f3);
			out.write("id\tinfo\n");
			out.write("1\tinfo\n");
			out.write("2\tinfo\n");
			out.close();

			// Fill the tables with some data
			// to be able to test the -deleteTarget option
			Statement stmt = this.connection.createStatement();
			for (int i=100; i < 120; i++)
			{
				stmt.executeUpdate("insert into zzbase (id, info) values (" + i + ", 'info" + i + "')");
			}
			for (int i=100; i < 120; i++)
			{
				stmt.executeUpdate("insert into child1 (id, base_id, info) values (" + i + ", " + i+ ", 'info" + i + "')");
			}
			for (int i=100; i < 120; i++)
			{
				stmt.executeUpdate("insert into a_child1_child (id, child_id, info) values (" + (i - 99) + ", " + i+ ", 'info" + i + "')");
			}
			this.connection.commit();

			WbFile f = new WbFile(basedir);
			StatementRunnerResult result = importCmd.execute("wbimport -sourcedir='" + f.getFullPath() + "' " +
				"-type=text " +
				"-header=true " +
				"-deleteTarget=true " +
				"-checkDependencies=true");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			ResultSet rs = stmt.executeQuery("select count(*) from zzbase");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Wrong row count for zzbase", 2, count);
			}
			else
			{
				fail("No rows in zzbase");
			}

			rs = stmt.executeQuery("select count(*) from child1");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Wrong row count for child1", 4, count);
			}
			else
			{
				fail("No rows in zzbase");
			}

			rs = stmt.executeQuery("select count(*) from a_child1_child");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Wrong row count for a_child1_child", 4, count);
			}
			else
			{
				fail("No rows in zzbase");
			}

			if (!f1.delete())
			{
				fail("Could not delete input file: " + f1.getCanonicalPath());
			}
			if (!f2.delete())
			{
				fail("Could not delete input file: " + f2.getCanonicalPath());
			}
			if (!f3.delete())
			{
				fail("Could not delete input file: " + f3.getCanonicalPath());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testMultiImportWithWarning()
		throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			File importFile  = new File(this.basedir, "junit_test.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname\tnot_there");
			out.println("1\tArthur\tDent\t1");
			out.println("2\tFord\tPrefect\t1");
			out.println("3\tZaphod\tBeeblebrox\t1");
			out.close();

			importFile  = new File(this.basedir, "zzbase.txt");
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("id\tinfo");
			out.println("1\tArthur");
			out.println("2\tFord");
			out.println("3\tZaphod");
			out.close();

			StatementRunnerResult result = importCmd.execute("wbimport -header=true -continueonerror=true -sourcedir='" + importFile.getParent() + "' -type=text");
			String msg = result.getMessageBuffer().toString();
//			System.out.println("**********\n" + msg);
			assertTrue(result.isSuccess());

			String toFind = "Column \"not_there\" ";
			int pos = msg.indexOf(toFind);
			assertTrue(pos > -1);

			int pos2 = msg.indexOf(toFind, pos + toFind.length());
			assertTrue(pos2 == -1);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private WbConnection prepareDatabase()
		throws SQLException, ClassNotFoundException
	{
		util.emptyBaseDirectory();
		WbConnection wb = util.getConnection();

		Statement stmt = wb.createStatement();
		stmt.executeUpdate("CREATE TABLE junit_test (nr integer, firstname varchar(100), lastname varchar(100))");
		stmt.executeUpdate("CREATE TABLE junit_test_pk (nr integer primary key, firstname varchar(100), lastname varchar(100))");
		stmt.executeUpdate("CREATE TABLE numeric_test (nr integer primary key, amount double, prod_name varchar(50))");
		stmt.executeUpdate("CREATE TABLE datatype_test (int_col integer, double_col double, char_col varchar(50), date_col date, time_col time, ts_col timestamp, nchar_col nvarchar(10))");
		stmt.executeUpdate("CREATE TABLE blob_test (nr integer, binary_data BINARY)");
		stmt.executeUpdate("CREATE TABLE clob_test (nr integer, text_data CLOB)");
		stmt.executeUpdate("CREATE TABLE bool_int_test (nr integer, int_flag INTEGER)");
		stmt.executeUpdate("CREATE TABLE bool_test (nr integer, flag BOOLEAN)");

		stmt.executeUpdate("CREATE TABLE zzbase (id integer primary key, info varchar(50))");
		stmt.executeUpdate("CREATE TABLE child1 (id integer primary key, base_id integer not null, info varchar(50))");
		stmt.executeUpdate("CREATE TABLE a_child1_child (id integer primary key, child_id integer not null, info varchar(50))");
		stmt.executeUpdate("alter table child1 add foreign key (base_id) references zzbase(id)");
		stmt.executeUpdate("alter table a_child1_child add foreign key (child_id) references child1(id)");

		wb.commit();
		stmt.close();

		return wb;
	}

}
