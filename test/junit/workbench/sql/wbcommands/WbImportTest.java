/*
 * WbImportTest.java
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
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
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.db.exporter.RowDataConverter;
import workbench.db.importer.TableDependencySorterTest;

import workbench.sql.StatementRunnerResult;

import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.ZipOutputFactory;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
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
	public void testImportIntoView()
		throws Exception
	{
		WbConnection con = getTestUtil().getHSQLConnection("view_import");
		try
		{
			TestUtil.executeScript(con,
				"create table foo (id integer primary key, data varchar(100));\n" +
				"create view v_foo as select * from foo;");
			File data = new File(basedir, "data.txt");
			FileUtil.writeString(data,
				"id,data\n" +
				"1,foo\n" +
				"2,bar");
			WbImport cmd = new WbImport();
			cmd.setConnection(con);
			StatementRunnerResult result = cmd.execute("WbImport -file='" + data.getAbsolutePath() + "' -type=text -delimiter=',' -header=true -table=v_foo");
			String msg = result.getMessageBuffer().toString();
			assertTrue(msg, result.isSuccess());

			int rows = TestUtil.getNumberValue(con, "select count(*) from v_foo");
			assertEquals(2, rows);

			FileUtil.writeString(data,
				"id,data\n" +
				"1,foobar\n" +
				"2,barfoo");

			result = cmd.execute("WbImport -file='" + data.getAbsolutePath() + "' -type=text -mode=update -keyColumns=id -delimiter=',' -header=true -table=v_foo");
			assertTrue(result.isSuccess());

			int id = TestUtil.getNumberValue(con, "select id from v_foo where data = 'foobar'");
			assertEquals(1, id);
			id = TestUtil.getNumberValue(con, "select id from v_foo where data = 'barfoo'");
			assertEquals(2, id);
		}
		finally
		{
			con.disconnect();
		}
	}

	@Test
	public void testMultiSheetExcelImport()
		throws Exception
	{
		util.dropAll(connection);
		InputStream in = TableDependencySorterTest.class.getResourceAsStream("hr_schema.sql");
		TestUtil.executeScript(connection, in);
		File input = util.copyResourceFile(this, "hr.xlsx");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' -type=xlsx -sheetNumber=* -header=true -continueonerror=false -checkDependencies=true");

		assertTrue(input.delete());
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());

		int rows = ((Number)TestUtil.getSingleQueryValue(connection, "select count(*) from countries")).intValue();
		assertEquals(25, rows);

		rows = ((Number)TestUtil.getSingleQueryValue(connection, "select count(*) from employees")).intValue();
		assertEquals(107, rows);

		rows = ((Number)TestUtil.getSingleQueryValue(connection, "select count(*) from job_history")).intValue();
		assertEquals(10, rows);
	}

	@Test
	public void testMultiSheetImport()
		throws Exception
	{
		File input = util.copyResourceFile(this, "person_orders.ods");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' -type=ods -sheetNumber=* -header=true -continueonerror=false ");

		assertTrue(input.delete());
		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());

		int rows = ((Number)TestUtil.getSingleQueryValue(connection, "select count(*) from person")).intValue();
		assertEquals(2, rows);

		rows = ((Number)TestUtil.getSingleQueryValue(connection, "select count(*) from orders")).intValue();
		assertEquals(4, rows);
	}

	@Test
	public void testIgnoreMissing()
		throws Exception
	{
		File input = new File(util.getBaseDir(), "id_data.txt");

		TestUtil.writeFile(input,
			"nr\tfirstname\tfoobar\tlastname\n" +
			"1\tArthur\txxxx\tDent\n" +
			"2\tFord\tyyyy\tPrefect\n" +
			"3\tZaphod\tzzz\tBeeblebrox\n",
			"ISO-8859-1");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' " +
			"-type=text " +
			"-header=true " +
			"-ignoreMissingColumns=true " +
			"-continueonerror=false " +
			"-table=junit_test");

		assertTrue(input.delete());

		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());

		String name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from junit_test where nr=1");
		assertEquals("Dent", name);

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from junit_test where nr=2");
		assertEquals("Prefect", name);

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from junit_test where nr=3");
		assertEquals("Beeblebrox", name);
	}

	@Test
	public void testMissingColumns()
		throws Exception
	{
		File input = new File(util.getBaseDir(), "id_data.txt");

		TestUtil.writeFile(input,
			"nr\tfirstname\tlastname\n" +
			"1\tArthur\tDent\n" +
			"2\tFord\n" +
			"3\tZaphod\tBeeblebrox\n",
			"ISO-8859-1");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' " +
			"-type=text " +
			"-header=true " +
			"-continueonerror=false " +
			"-table=junit_test");

		assertTrue(input.delete());

		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());

		String name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from junit_test where nr=1");
		assertEquals("Dent", name);

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from junit_test where nr=2");
		assertNull(name);

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from junit_test where nr=3");
		assertEquals("Beeblebrox", name);
	}

	@Test
	public void testH2Upsert()
		throws Exception
	{
		File input = new File(util.getBaseDir(), "id_data.txt");

		TestUtil.writeFile(input,
			"id\tfirstname\tlastname\n" +
			"1\tArthur\tDent\n" +
			"2\tFord\tPrefect\n" +
			"3\tZaphod\tBeeblebrox\n",
			"ISO-8859-1");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' " +
			"-type=text " +
			"-header=true " +
			"-continueonerror=false " +
			"-table=person");

		assertTrue(input.delete());

		String msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());

		String name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from person where id=1");
		assertEquals("Dent", name);

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from person where id=2");
		assertEquals("Prefect", name);

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from person where id=3");
		assertEquals("Beeblebrox", name);

		TestUtil.writeFile(input,
			"id\tfirstname\tlastname\n" +
			"1\tArthur\tDENT\n" +
			"2\tFord\tPrefect\n", 
			"ISO-8859-1");

		result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' " +
			"-type=text " +
			"-mode=insert,update " +
			"-header=true " +
			"-continueonerror=false " +
			"-table=person");

		assertTrue(input.delete());

		msg = result.getMessageBuffer().toString();
		assertTrue(msg, result.isSuccess());

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from person where id=1");
		assertEquals("DENT", name);

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from person where id=2");
		assertEquals("Prefect", name);

		name = (String)TestUtil.getSingleQueryValue(connection, "select lastname from person where id=3");
		assertEquals("Beeblebrox", name);
	}

	@Test
	public void testIdentityInsert()
		throws Exception
	{
		File input = new File(util.getBaseDir(), "id_data.txt");

		TestUtil.writeFile(input,
			"id\tfirstname\tlastname\n" +
			"100\tArthur\tDent\n" +
			"101\tFord\tPrefect\n", "ISO-8859-1");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' " +
			"-type=text " +
			"-header=true " +
			"-ignoreIdentityColumns=true " +
			"-continueonerror=false " +
			"-table=id_test");

		assertTrue(input.delete());

		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		Number id = (Number)TestUtil.getSingleQueryValue(connection, "select id from id_test where lastname = 'Dent'");
		assertNotNull(id);
		assertEquals(1, id.intValue());

		id = (Number)TestUtil.getSingleQueryValue(connection, "select id from id_test where lastname = 'Prefect'");
		assertNotNull(id);
		assertEquals(2, id.intValue());
	}

	@Test
	public void testSpreadSheetIgnoreTable()
		throws Exception
	{
		TestUtil.executeScript(connection,
			"drop table person;\n" +
			"commit;");

		File input = util.copyResourceFile(this, "person_orders.ods");
		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' " +
			"-type=ods " +
			"-sheetName=* " +
			"-header=true " +
			"-continueonerror=false ");

		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertTrue(input.delete());

		Number salary = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from orders");
		assertNotNull(salary);
		int count = salary.intValue();
		assertEquals(4, count);
	}

	@Test
	public void testSpreadSheetIgnoreColumn()
		throws Exception
	{
		File input = util.copyResourceFile(this, "data-2.ods");
		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' " +
			"-type=ods " +
			"-sheetName=person " +
			"-header=true " +
			"-ignoreMissingColumns=true " +
			"-continueonerror=false " +
			"-table=person");

		assertTrue(input.delete());

		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		Number id = (Number)TestUtil.getSingleQueryValue(connection, "select id from person where lastname = 'Dent'");
		assertNotNull(id);
		assertEquals(1, id.intValue());

		Number salary = (Number)TestUtil.getSingleQueryValue(connection, "select salary from person where id = 2");
		assertNotNull(salary);
		double sal = salary.doubleValue();
		assertEquals(1234.56, sal, 0.1);
	}

	@Test
	public void testOdsImport()
		throws Exception
	{
		File input = util.copyResourceFile(this, "data.ods");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -file='" + input.getAbsolutePath() + "' " +
			"-type=ods " +
			"-header=true " +
			"-continueonerror=false " +
			"-table=junit_test");

		assertTrue(input.delete());

		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		Number count = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from junit_test");
		assertEquals(2, count.intValue());

		input = util.copyResourceFile(this, "data.ods");

		result = importCmd.execute(
			"WbImport -file='" + input.getAbsolutePath() + "' " +
			" -type=ods " +
			" -sheetNumber=2 " +
			" -header=true " +
			" -continueonerror=false " +
			" -table=datatype_test");

//		assertTrue(input.delete());

		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		count = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from junit_test");
		assertEquals(2, count.intValue());

		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select int_col,double_col,char_col,date_col,time_col,ts_col,nchar_col from datatype_test");
			rs.next();
			int i = rs.getInt("int_col");
			assertEquals(42, i);

			double d = rs.getDouble("double_col");
			assertEquals(1234.56, d, 0.01);
			String s = rs.getString("char_col");
			assertEquals("char_value", s.trim());
			java.sql.Date dt = rs.getDate("date_col");
			SimpleDateFormat dtfm = new SimpleDateFormat(StringUtil.ISO_DATE_FORMAT);
			java.util.Date dtv = dtfm.parse("1980-11-01");
			assertEquals(dtv, dt);

			java.sql.Time t = rs.getTime("time_col");
			SimpleDateFormat tfm = new SimpleDateFormat("HH:mm:ss");
			assertEquals("23:54:14", tfm.format(t));

			java.sql.Timestamp ts = rs.getTimestamp("ts_col");
			SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			assertEquals("1990-10-01 17:04:06", tsFmt.format(ts));

			s = rs.getString("nchar_col");
			assertEquals("nchar_val", s);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Test
	public void testExcel()
		throws Exception
	{

		ResultSet rs = null;
		Statement stmt = null;
		File importFile = createExcelFile("person.xls");

		try
		{

			StatementRunnerResult result = importCmd.execute("wbimport " +
				"-file='" + importFile.getAbsolutePath() + "' " +
				"-type=xls " +
				"-header=true " +
				"-continueonerror=false " +
				"-table=junit_test");

			assertTrue(importFile.delete());

			assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
			Number count = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from junit_test");
			assertEquals(2, count.intValue());

			TestUtil.executeScript(connection,
				"delete from junit_test;\n" +
				"commit;\n"
			);

			importFile = createExcelFile("person.xls");
			result = importCmd.execute("wbimport " +
				"-file='" + importFile.getAbsolutePath() + "' " +
				"-type=xls " +
				"-header=true " +
				"-sheetNumber=2 " +
				"-continueonerror=false " +
				"-table=junit_test");

			assertTrue(importFile.delete());

			assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
			count = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from junit_test");
			assertEquals(5, count.intValue());

			TestUtil.executeScript(connection,
				"delete from junit_test;\n" +
				"delete from junit_test_pk;\n" +
				"commit;\n"
			);

			util.emptyBaseDirectory();

			createExcelFile("junit_test.xls");
			createExcelFile("junit_test_pk.xls");

			result = importCmd.execute("wbimport " +
				"-sourceDir='" + util.getBaseDir() + "' " +
				"-type=xls " +
				"-extension=xls " +
				"-header=true " +
				"-continueonerror=false");
			String msg = result.getMessageBuffer().toString();
			assertTrue(msg, result.isSuccess());
			count = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from junit_test");
			assertEquals(2, count.intValue());
			count = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from junit_test_pk");
			assertEquals(2, count.intValue());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private WbFile createExcelFile(String filename)
		throws Exception
	{
		WbFile data = new WbFile(util.getBaseDir(), filename);

		Workbook workbook = null;
		if (filename.endsWith("xlsx"))
		{
			workbook = new XSSFWorkbook();
		}
		else
		{
			workbook = new HSSFWorkbook();
		}

		Sheet sheet = workbook.createSheet("Sheet One");
		Row header = sheet.createRow(0);
		List<String> columns = CollectionUtil.arrayList("nr", "firstname", "lastname");
		for (int i=0; i < columns.size(); i++)
		{
			Cell cell = header.createCell(i, Cell.CELL_TYPE_STRING);
			cell.setCellValue(columns.get(i));
		}

		for (int i=0; i < 2; i++)
		{
			Row dataRow = sheet.createRow(i + 1);
			Cell cell = dataRow.createCell(0, Cell.CELL_TYPE_NUMERIC);
			cell.setCellValue(i);
			cell = dataRow.createCell(1, Cell.CELL_TYPE_STRING);
			cell.setCellValue("Firstname " + (i+1));
			cell = dataRow.createCell(2, Cell.CELL_TYPE_STRING);
			cell.setCellValue("Lastname " + (i+1));
		}

		Sheet sheet2 = workbook.createSheet("Sheet Two");
		header = sheet2.createRow(0);

		for (int i=0; i < columns.size(); i++)
		{
			Cell cell = header.createCell(i, Cell.CELL_TYPE_STRING);
			cell.setCellValue(columns.get(i));
		}

		for (int i=0; i < 5; i++)
		{
			Row dataRow = sheet2.createRow(i + 1);
			Cell cell = dataRow.createCell(0, Cell.CELL_TYPE_NUMERIC);
			cell.setCellValue(i);
			cell = dataRow.createCell(1, Cell.CELL_TYPE_STRING);
			cell.setCellValue("Firstname " + (i+1));
			cell = dataRow.createCell(2, Cell.CELL_TYPE_STRING);
			cell.setCellValue("Lastname " + (i+1));
		}

		OutputStream out = null;
		try
		{
			out = new FileOutputStream(data);
			workbook.write(out);
		}
		finally
		{
			FileUtil.closeQuietely(out);
		}
		return data;
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
			TestUtil.writeFile(importFile,
				"nr\tfirstname\tlastname\n"+
				"1\tArthur\tDent\n"+
				"nan\tFord\tPrefect\n"+
				"3\tZaphod\tBeeblebrox", "UTF-8");

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
		throws Exception
	{
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			File importFile  = new File(this.basedir, "table_statements.txt");
			TestUtil.writeFile(importFile,
				"nr\tfirstname\tlastname\n" +
				"1\tArthur\tDent\n" +
				"2\tFord\tPrefect\n" +
				"3\tZaphod\tBeeblebrox\n", "UTF-8");

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
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Test
	public void testFunctionConstant()
		throws Exception
	{
		File importFile  = new File(this.basedir, "constant_func_import.txt");
		TestUtil.writeFile(importFile,
			"firstname\tlastname\n" +
			"Arthur\tDent\n" +
			"Ford\tPrefect\n" +
			"Zaphod\tBeeblebrox", "UTF-8");

		TestUtil.executeScript(connection,
			"create sequence seq_junit start with 1;\n" +
			"commit;");

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -constantValues=\"nr=${next value for seq_junit}\" -type=text -header=true -continueonerror=false -table=junit_test_pk");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		Number count = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from junit_test_pk");
		assertEquals("Not enough values imported", 3, count.intValue());

		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("select nr, lastname, firstname from junit_test_pk order by nr");
			)
		{

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
		}

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testConstantWithSelect()
		throws Exception
	{
		File importFile  = new File(this.basedir, "constant_func_import.txt");

		TestUtil.writeFile(importFile,
			"id\tfirstname\ttyp_cod\tlastname\ttyp_nam\n" +
			"1\tArthur\teno\tDent\tone\n" +
			"2\tFord\towt\tPrefect\ttwo\n" +
			"3\tZaphod\teerth\tBeeblebrox\tthree", "UTF-8");

		String script =
			"CREATE TABLE person2 (id integer, firstname varchar(20), lastname varchar(20), type_id integer);\n" +
			"commit;";

		TestUtil.executeScript(connection, script);

		script =
			"CREATE TABLE type_lookup (id integer, type_name varchar(10), type_code varchar(10));\n" +
			"insert into type_lookup values (1, 'one', 'eno');\n" +
			"insert into type_lookup values (2, 'two', 'owt');\n" +
			"insert into type_lookup values (3, 'three', 'eerth');\n" +
			"commit;";
		TestUtil.executeScript(connection, script);

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' " +
			" -importColumns=id,firstname,lastname " +
			" -constantValues=\"type_id=$@{select id from type_lookup where type_name = $5 and type_code = $3}\" " +
			" -type=text " +
			" -header=true " +
			" -continueonerror=false " +
			" -table=person2"
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
		TestUtil.writeFile(importFile,
		"nr\t\tlastname\t\tfirstname\n" +
		"1\t\tDent\t\tArthur\n" +
		"2\t\tPrefect\t\tFord\n" +
		"3\t\tBeeblebrox\t\tZaphod", "UTF-8");

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -delimiter='\\t\\t' -type=text -header=true -continueonerror=false -table=junit_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		int count = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
		assertEquals("Not enough values imported", 3, count);

		try (
			Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("select lastname, firstname from junit_test where nr = 1");
			)
		{
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
		}

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
		TestUtil.writeFile(importFile,
		"nr\tlastname\n" +
		"1\tDent\n" +
		"2\tPrefect\n" +
		"3\tBeeblebrox", "UTF-8");

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' " +
			"-constantValues=\"firstname=Unknown\" " +
			"-type=text " +
			"-header=true " +
			"-continueonerror=false -table=junit_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		int count = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
		assertEquals("Not enough values imported", 3, count);

		try (
				Statement stmt = this.connection.createStatementForQuery();
				ResultSet rs = stmt.executeQuery("select lastname, firstname from junit_test where nr = 1")
			)
		{
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
		}

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testMultipleConstants()
		throws Exception
	{
		File importFile  = new File(this.basedir, "mult_constant_import.txt");
		TestUtil.writeFile(importFile,
			"id\n"+
			"1\n" +
			"2\n" +
			"3", "UTF-8");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "'" +
			" -constantValues=flag1=xx,flag2=yy " +
			" -type=text " +
			" -header=true " +
			" -continueonerror=false " +
			" -table=const_test"
		);
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			int count = 0;
			stmt = this.connection.createStatementForQuery();
			rs = stmt.executeQuery("select id, flag1, flag2 from const_test order by id");
			while (rs.next())
			{
				count ++;
				int id = rs.getInt(1);
				assertEquals(count, id);
				String f1 = rs.getString(2);
				String f2 = rs.getString(3);
				assertEquals("xx", f1);
				assertEquals("yy", f2);
			}
			assertEquals("Not enough values imported", 3, count);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
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

		File xmlFile = new File(this.basedir, "partial_xml_import.xml");
		TestUtil.writeFile(xmlFile, xml, "UTF-8");

		String cmd = "wbimport -importColumns=nr,lastname -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
		StatementRunnerResult result = importCmd.execute(cmd);
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		try (Statement stmt = this.connection.createStatementForQuery();
				 ResultSet rs = stmt.executeQuery("select nr, lastname, firstname from junit_test"))
		{
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
		}
		if (!xmlFile.delete())
		{
			fail("Could not delete input file: " + xmlFile.getCanonicalPath());
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
			assertEquals("Import did not succeed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

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
			TestUtil.writeFile(xmlFile, xml, "UTF-8");

			String cmd = "wbimport -continueOnError=false -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
			StatementRunnerResult result = importCmd.execute(cmd);
			assertEquals("Import succeeded", false, result.isSuccess());

			cmd = "wbimport -encoding='UTF-8' -continueOnError=true -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
			result = importCmd.execute(cmd);
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), true, result.isSuccess());

			Number rows = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from junit_test");
			assertEquals("Wrong number of rows imported", 3, rows.intValue());

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
		throws Exception
	{
		File importfile = new File(this.basedir, "bool_literal.txt");
		TestUtil.writeFile(importfile,
		"nr,flag\n"+
		"1,yes\n"+
		"2,5\n"+
		"3,99\n"+
		"4,no\n"+
		"5,no\n");

		// Test importing correct true/false values
		String cmd = "wbimport -literalsFalse='no,99' -literalsTrue='yes,5' -type=text -header=true  -table=bool_test -continueOnError=false -delimiter=',' -booleanToNumber=true -encoding='UTF-8' -file='" + importfile.getAbsolutePath() + "'";
		StatementRunnerResult result = importCmd.execute(cmd);
		String msg = result.getMessageBuffer().toString();
		assertEquals(msg, true, result.isSuccess());

		Number rows = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from bool_test where flag = false");
		assertEquals("Wrong number of rows imported", 3, rows.intValue());

		rows = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from bool_test where flag = true");
		assertEquals("Wrong number of rows imported", 2, rows.intValue());

		TestUtil.executeScript(connection,
			"delete from bool_test;\n" +
			"commit;");

		// Test importing incorrect values
		// as -continueOnError=false is supplied no rows should make into the table
		cmd = "wbimport -literalsFalse='no,false' -literalsTrue='yes,true' -type=text -header=true  -table=bool_test -continueOnError=false -delimiter=',' -booleanToNumber=true -encoding='UTF-8' -file='" + importfile.getAbsolutePath() + "'";
		result = importCmd.execute(cmd);
		msg = result.getMessageBuffer().toString();
		assertEquals(msg, false, result.isSuccess());

		rows = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from bool_test");
		assertEquals("Rows were imported", 0, rows.intValue());

		TestUtil.executeScript(connection,
			"delete from bool_test;\n" +
			"commit;");

		// Test importing incorrect values
		// as -continueOnError=true is supplied only 3 rows should make into the table
		cmd = "wbimport -literalsFalse='no,false' -literalsTrue='yes,true' -type=text -header=true  -table=bool_test -continueOnError=true -delimiter=',' -booleanToNumber=true -encoding='UTF-8' -file='" + importfile.getAbsolutePath() + "'";
		result = importCmd.execute(cmd);
		msg = result.getMessageBuffer().toString();
		assertEquals(msg, true, result.isSuccess());
		assertEquals(msg, true, result.hasWarning());

		rows = (Number)TestUtil.getSingleQueryValue(connection, "select count(*) from bool_test");
		assertEquals("Wrong number of rows imported", 3, rows.intValue());

		if (!importfile.delete())
		{
			fail("Could not delete input file: " + importfile.getCanonicalPath());
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
			TestUtil.writeFile(xmlFile, xml, "UTF-8");

			// Test importing only correct true/false values
			String cmd = "wbimport -continueOnError=false -startRow=1 -endRow=2 -booleanToNumber=true -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=bool_int_test";
			StatementRunnerResult result = importCmd.execute(cmd);
			String msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());
//			System.out.println("messages: " + msg);
			Statement stmt = this.connection.createStatement();

			int rows = TestUtil.getNumberValue(connection, "select count(*) from bool_int_test");
			assertEquals("Wrong number of rows imported", 2, rows);

			TestUtil.executeScript(connection,
				"delete from bool_int_test;\n" +
				"commit;");

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
    TestUtil.writeFile(importfile,
		"nr,int_flag\n" +
		"1,true\n" +
		"2,ja\n" +
		"3,true\n" +
		"4,no\n" +
		"5,nein\n" +
		"6,yes\n" +
		"7,false\n", "UTF-8");

		// Test importing correct true/false values
		String cmd = "wbimport -literalsFalse='false,no,nein' -literalsTrue='true,ja,yes' -type=text -header=true  -table=bool_int_test -continueOnError=false -delimiter=',' -numericFalse='-24' -numericTrue='42' -encoding='UTF-8' -file='" + importfile.getAbsolutePath() + "'";
		StatementRunnerResult result = importCmd.execute(cmd);
		String msg = result.getMessageBuffer().toString();
		assertEquals(msg, true, result.isSuccess());

    int rows = TestUtil.getNumberValue(connection, "select count(*) from bool_int_test where int_flag = -24");
    assertEquals("Wrong number of rows imported", 3, rows);
		assertTrue("Could not delete input file: " + importfile.getCanonicalPath(), importfile.delete());
	}

	@Test
	public void testTextClobImport()
	{
		try
		{
			File importFile  = new File(this.basedir, "import_text_clob.txt");

      TestUtil.writeFile(importFile,
      "nr\ttext_data\n" +
      "1\ttext_data_r1_c2.data\n" +
      "2\ttext_data_r2_c2.data\n", "UTF-8");

			String data1 = "This is a CLOB string to be put into row 1";
			String data2 = "This is a CLOB string to be put into row 2";

			File datafile = new File(this.basedir, "text_data_r1_c2.data");
      TestUtil.writeFile(datafile, data1);

			datafile = new File(this.basedir, "text_data_r2_c2.data");
      TestUtil.writeFile(datafile, data2);

			StatementRunnerResult result = importCmd.execute(
        "-- this is the import test\n" +
          "wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -clobIsFilename=true " +
          "         -type=text -header=true -continueonerror=false -table=clob_test"
      );
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
    String name = "\u0627\u0644\u0633\u0639\u0631 \u0627\u0644\u0645\u0642\u062A\u0631\u062D \u0644\u0644\u0645\u0633\u0647\u0644\u0643";
    File importFile  = new File(this.basedir, "regular_import.txt");
    try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8")))
    {
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
    }

    StatementRunnerResult result = importCmd.execute("-- this is the import test\nwbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -multiline=false -type=text -header=true -continueonerror=false -table=junit_test");
    assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    int count = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
    assertEquals("Not enough values imported", rowCount, count);

    String sname = (String)TestUtil.getSingleQueryValue(connection, "select lastname from junit_test where nr = 999");
    assertEquals("Unicode incorrectly imported", name, sname);

    sname = (String)TestUtil.getSingleQueryValue(connection, "select firstname from junit_test where nr = 42");
    assertEquals("Embedded quote not imported", "arthur\"dent", sname);
    if (!importFile.delete())
    {
      fail("Could not delete input file: " + importFile.getCanonicalPath());
    }
	}

	@Test
	public void testColumnLimit()
		throws Exception
	{
		File importFile  = new File(this.basedir, "col_limit.txt");
    TestUtil.writeFile(importFile,
      "nr\tfirstname\tlastname\n" +
      "x1\tArthur\tDent\n" +
      "x2\tZaphod\tBeeblebrox\n", "UTF-8");

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -colSubstring=nr=1:5 -maxLength='firstname=50,lastname=4' -type=text -header=true -continueonerror=false -table=junit_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test"))
    {
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

    }
		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testSkipImport()
		throws Exception
	{
		File importFile  = new File(this.basedir, "partial_skip.txt");
    TestUtil.writeFile(importFile,
      "nr\tfirstname\tlastname\n" +
      "1\tArthur\tDent\n" +
      "2\tZaphod\tBeeblebrox\n", "UTF-8");

		StatementRunnerResult result = importCmd.execute(
			"-- this is the import test\nwbimport " +
			"-encoding=utf8 -file='" + importFile.getAbsolutePath() + "' " +
			"-filecolumns=nr,$wb_skip$,lastname -type=text " +
			"-header=true -continueonerror=false -table=junit_test");
		String msg = result.getMessageBuffer().toString();
		assertEquals("Import failed: " + msg, result.isSuccess(), true);
		String[] lines = msg.split(StringUtil.REGEX_CRLF);
		assertEquals(3, lines.length);
		assertTrue(lines[0].endsWith("into table JUNIT_TEST"));
		assertEquals("2 row(s) inserted", lines[1]);
		assertEquals("0 row(s) updated", lines[2]);

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test"))
    {
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
    }
		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testPartialTextImport()
		throws Exception
	{
		int rowCount = 100;
    File importFile  = new File(this.basedir, "partial1.txt");
    try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8")))
    {
      out.println("nr\tfirstname\tlastname");
      for (int i = 0; i < rowCount; i++)
      {
        int id = i+1;
        out.println(id + "\tFirstname" + id + "\tLastname" + id);
      }
    }

    StatementRunnerResult result = importCmd.execute("-- this is the import test\nwbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -filecolumns=nr,firstname,lastname -type=text -header=true -continueonerror=false -startrow=10 -endrow=20 -table=junit_test");
    assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select min(nr), max(nr), count(*) from junit_test"))
    {
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
    }
    if (!importFile.delete())
    {
      fail("Could not delete input file: " + importFile.getCanonicalPath());
    }
	}

	@Test
	public void testUnmatchedFileColumn()
		throws Exception
	{
    File importFile  = new File(this.basedir, "partial2.txt");
    TestUtil.writeFile(importFile, "1\tArthur\n2\tZaphod\n", "UTF-8");

    StatementRunnerResult result = importCmd.execute("" +
      "wbimport -encoding=utf8 " +
      "-file='" + importFile.getAbsolutePath() + "' " +
      "-type=text " +
      "-header=false " +
      "-continueonerror=false " +
      "-table=junit_test");
    assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test"))
    {
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

    }
    if (!importFile.delete())
    {
      fail("Could not delete input file: " + importFile.getCanonicalPath());
    }
	}

	@Test
	public void testPartialColumnTextImport()
		throws Exception
	{
    File importFile  = new File(this.basedir, "partial2.txt");
    TestUtil.writeFile(importFile,
        "nr\tfirstname\tlastname\n" +
        "1\tArthur\tDent\n"  +
        "2\tZaphod\tBeeblebrox\n");

    StatementRunnerResult result = importCmd.execute("-- this is the import test\nwbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -filecolumns=nr,firstname,lastname -importcolumns=nr,lastname -type=text -header=true -continueonerror=false -table=junit_test");
    assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test"))
    {
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

    }
    if (!importFile.delete())
    {
      fail("Could not delete input file: " + importFile.getCanonicalPath());
    }
	}

	@Test
	public void testMultiLineImport()
    throws Exception
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

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test"))
    {
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
    }
    if (!importFile.delete())
    {
      fail("Could not delete input file: " + importFile.getCanonicalPath());
    }
	}

	@Test
	public void testZipMultiLineImport()
    throws Exception
	{
    File importFile  = new File(this.basedir, "zipmulti.txt");

    File archive = new File(this.basedir, "zipmulti.zip");
    ZipOutputFactory zout = new ZipOutputFactory(archive);
    try (PrintWriter out = new PrintWriter(zout.createWriter(importFile, "UTF-8")))
    {
      out.print("nr\tfirstname\tlastname\n");
      out.print("1\tFirst\t\"Last\n");
      out.print("name\"\n");
    }
    zout.done();

    StatementRunnerResult result = importCmd.execute("wbimport -file='" + archive.getAbsolutePath() + "' -multiline=true -quotechar='\"' -type=text -header=true -continueonerror=false -table=junit_test");
    assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test"))
    {
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
    }
    if (!archive.delete())
    {
      fail("Could not delete archive! " + archive.getAbsolutePath());
    }
	}

	@Test
	public void testEmptyStringIsNull()
		throws Exception
	{
    File importFile  = new File(this.basedir, "import_empty.txt");
    TestUtil.writeFile(importFile, "1\tFirstname\t");

    StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -emptyStringIsNull=true -type=text -filecolumns=nr,firstname,lastname -header=false -table=junit_test");
    assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select nr,firstname,lastname from junit_test order by nr"))
    {
      if (rs.next())
      {
        int nr = rs.getInt(1);
        assertEquals("Wrong values imported", nr, 1);
        String first = rs.getString(2);
        assertEquals("Wrong firstname", "Firstname", first);

        String last = rs.getString(3);
        assertNull("Lastname not null", last);
      }
    }
    if (!importFile.delete())
    {
      fail("Could not delete input file: " + importFile.getCanonicalPath());
    }
	}

	@Test
	public void testNullNumeric()
		throws Exception
	{
    File importFile  = new File(this.basedir, "import_null_numeric.txt");
    TestUtil.writeFile(importFile,
      "nr\tamount\tprod_name\n" +
      "1\t1.1\tfirst\n" +
      "2\t\tsecond\n" +
      "3\t3.3\tthird\n");

    StatementRunnerResult result = importCmd.execute("wbimport -decimal=. -file='" + importFile.getAbsolutePath() + "' -type=text -header=true -table=numeric_test");
    assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = this.connection.createStatementForQuery();
      rs = stmt.executeQuery("select nr, amount, prod_name from numeric_test order by nr");
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
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
	}

	@Test
	public void testMissingTarget()
		throws Exception
	{
    File importFile  = new File(this.basedir, "dummy.txt");
    try (PrintWriter out = new PrintWriter(new FileWriter(importFile)))
    {
      for (int i = 0; i < 10; i++)
      {
        out.print(Integer.toString(i));
        out.print('\t');
        out.println("First" + i + "\tLastname" + i);
      }
    }

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

	@Test
	public void testNoHeader()
		throws Exception
	{
		int rowCount = 10;
    File importFile  = new File(this.basedir, "import_no_header.txt");
    try (PrintWriter out = new PrintWriter(new FileWriter(importFile)))
    {
      for (int i = 0; i < rowCount; i++)
      {
        out.print(Integer.toString(i));
        out.print('\t');
        out.println("First" + i + "\tLastname" + i);
      }
    }

    StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -multiline=true  -type=text -filecolumns=nr,firstname,lastname -header=false -table=junit_test");
    assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    int count = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
    assertEquals("Not enough values imported", rowCount, count);

    if (!importFile.delete())
    {
      fail("Could not delete input file: " + importFile.getCanonicalPath());
    }
	}

	@Test
	public void testColumnsFromTable()
		throws Exception
	{
		int rowCount = 10;
    File importFile  = new File(this.basedir, "import_tbl_cols.txt");
    try (PrintWriter out = new PrintWriter(new FileWriter(importFile)))
    {
      for (int i = 0; i < rowCount; i++)
      {
        out.print(Integer.toString(i));
        out.print('\t');
        out.println("First" + i + "\tLastname" + i);
      }
    }

    StatementRunnerResult result = importCmd.execute(
      "wbimport -file='" + importFile.getAbsolutePath() + "' -multiline=true -type=text -header=false -table=junit_test");
    assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    int rows = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
    assertEquals("Not enough values imported", rowCount, rows);

    if (!importFile.delete())
    {
      fail("Could not delete input file: " + importFile.getCanonicalPath());
    }
	}

	@Test
	public void testDirImport()
		throws Exception
	{
		int rowCount = 10;
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

	@Test
	public void testMultiFileSingleTableImport()
		throws Exception
	{
		int rowCount = 10;
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

	@Test
	public void testMultiFileSingleTableImportWithHeader()
		throws Exception
	{
		int rowCount = 10;
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

		TestUtil.executeScript(connection,
			"DELETE FROM junit_test;\n" +
			"commit;");

		StatementRunnerResult result = importCmd.execute("wbimport -header=true -fileColumns=$wb_skip$,nr,firstname,lastname -continueonerror=false -sourcedir='" + importFile.getParent() + "' -type=text -extension=mtxt -table=junit_test");
		assertTrue("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess());

		int count = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
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

	@Test
	public void testMappedImport()
		throws Exception
	{
		int rowCount = 10;

		File importFile  = new File(this.basedir, "import.txt");
		try (PrintWriter out = new PrintWriter(new FileWriter(importFile)))
		{
			out.println("nr\tpid\tfirstname\tlastname");
			for (int i = 0; i < rowCount; i++)
			{
				out.print(Integer.toString(i));
				out.println("\t42\tFirst" + i + "\tLastname" + i);
			}
		}

		StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -type=text -continueonerror=true -header=true -table=junit_test");
		assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		int count = TestUtil.getNumberValue(connection, "select count(*) from junit_test");

		assertEquals("Not enough values imported", rowCount, count);
		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testDataTypes()
		throws Exception
	{
		File importFile  = new File(this.basedir, "import_types.txt");
		try (PrintWriter out = new PrintWriter(new FileWriter(importFile)))
		{
			out.println("int_col\tdouble_col\tchar_col\tdate_col\ttime_col\tts_col\tnchar_col");
			out.println("42\t42.1234\tfortytwo\t2006-02-01\t22:30\t2006-04-01 22:34\tnvarchar");
		}

		StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -decimal='.' -type=text -header=true -table=datatype_test -dateformat='yyyy-MM-dd' -timestampformat='yyyy-MM-dd HH:mm'");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		try (Statement stmt = this.connection.createStatementForQuery();
				ResultSet rs = stmt.executeQuery("select int_col, double_col, char_col, date_col, time_col, ts_col from datatype_test"))
		{
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
			SqlUtil.closeAll(rs, stmt);
		}

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
	}

	@Test
	public void testZippedTextBlobImport()
		throws Exception
	{
		File importFile  = new File(this.basedir, "blob_test.txt");

		File archive = new File(this.basedir, "blob_test.zip");
		ZipOutputFactory zout = new ZipOutputFactory(archive);
		try (Writer w = zout.createWriter(importFile, "UTF-8");
				PrintWriter out = new PrintWriter(w))
		{
			out.println("nr\tbinary_data");
			out.println("1\tblob_data_r1_c1.data");
			out.close();
			zout.done();
		}

		File blobarchive = new File(this.basedir, "blob_test" + RowDataConverter.BLOB_ARCHIVE_SUFFIX + ".zip");
		zout = new ZipOutputFactory(blobarchive);
		try (OutputStream binaryOut = zout.createOutputStream(new File("blob_data_r1_c1.data")))
		{
			byte[] testData = new byte[1024];
			for (int i = 0; i < testData.length; i++)
			{
				testData[i] = (byte)(i % 255);
			}
			binaryOut.write(testData);
		}

		zout.done();

		StatementRunnerResult result = importCmd.execute("wbimport -file='" + archive.getAbsolutePath() + "' -decimal='.' -multiline=true -encoding='UTF-8' -type=text -header=true -table=blob_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		try (Statement stmt = this.connection.createStatementForQuery();
				ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test"))
		{

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
		}
		if (!archive.delete())
		{
			fail("Could not delete input file: " + archive.getCanonicalPath());
		}
	}

	@Test
	public void testVerboseXmlImport()
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
		File xmlFile = new File(this.basedir, "xml_verbose_import.xml");
		TestUtil.writeFile(xmlFile, xml, "UTF-8");

		String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
//			System.out.println("cmd=" + cmd);
		StatementRunnerResult result = importCmd.execute(cmd);
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		try (Statement stmt = this.connection.createStatementForQuery();
				ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test"))
		{
			int rowCount = 0;
			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", rowCount, nr);
			}
			assertEquals("Wrong number of rows", rowCount, 2);
		}
		if (!xmlFile.delete())
		{
			fail("Could not delete input file: " + xmlFile.getCanonicalPath());
		}
	}

	@Test
	public void testMultipleXmlImport()
		throws Exception
	{
		String xml1 =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
			"<wb-export> \n" +
			"  <meta-data> \n" +
			" \n" +
			"    <generating-sql> \n" +
			"    <![CDATA[ \n" +
			"    SELECT id_a FROM a \n" +
			"    ]]> \n" +
			"    </generating-sql> \n" +
			" \n" +
			"    <created>2012-07-31 17:52:46.265 MESZ</created> \n" +
			"    <jdbc-driver>PostgreSQL Native Driver</jdbc-driver> \n" +
			"    <jdbc-driver-version>PostgreSQL 9.1 JDBC4 (build 902)</jdbc-driver-version> \n" +
			"    <connection>User=thomas, URL=jdbc:postgresql://localhost/wbtest</connection> \n" +
			"    <schema>public</schema> \n" +
			"    <catalog></catalog> \n" +
			"    <database-product-name>PostgreSQL</database-product-name> \n" +
			"    <database-product-version>9.1.3</database-product-version> \n" +
			"    <wb-tag-format>short</wb-tag-format> \n" +
			"  </meta-data> \n" +
			" \n" +
			"  <table-def> \n" +
			" \n" +
			"    <table-name>a</table-name> \n" +
			"    <column-count>1</column-count> \n" +
			" \n" +
			"    <column-def index=\"0\"> \n" +
			"      <column-name>id_a</column-name> \n" +
			"      <java-class>java.lang.Integer</java-class> \n" +
			"      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
			"      <java-sql-type>4</java-sql-type> \n" +
			"      <dbms-data-type>integer</dbms-data-type> \n" +
			"      <primary-key>false</primary-key> \n" +
			"    </column-def> \n" +
			"  </table-def> \n" +
			" \n" +
			"<data> \n" +
			"<rd><cd>1</cd></rd> \n" +
			"<rd><cd>2</cd></rd> \n" +
			"<rd><cd>3</cd></rd> \n" +
			"<rd><cd>4</cd></rd> \n" +
			"<rd><cd>5</cd></rd> \n" +
			"<rd><cd>6</cd></rd> \n" +
			"<rd><cd>7</cd></rd> \n" +
			"<rd><cd>8</cd></rd> \n" +
			"<rd><cd>9</cd></rd> \n" +
			"<rd><cd>10</cd></rd> \n" +
			"<rd><cd>11</cd></rd> \n" +
			"<rd><cd>12</cd></rd> \n" +
			"<rd><cd>13</cd></rd> \n" +
			"<rd><cd>14</cd></rd> \n" +
			"<rd><cd>15</cd></rd> \n" +
			"</data> \n" +
			"</wb-export>";

		String xml2 =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
			"<wb-export> \n" +
			"  <meta-data> \n" +
			" \n" +
			"    <generating-sql> \n" +
			"    <![CDATA[ \n" +
			"    SELECT id_b FROM b \n" +
			"    ]]> \n" +
			"    </generating-sql> \n" +
			" \n" +
			"    <created>2012-07-31 17:52:46.265 MESZ</created> \n" +
			"    <jdbc-driver>PostgreSQL Native Driver</jdbc-driver> \n" +
			"    <jdbc-driver-version>PostgreSQL 9.1 JDBC4 (build 902)</jdbc-driver-version> \n" +
			"    <connection>User=thomas, URL=jdbc:postgresql://localhost/wbtest</connection> \n" +
			"    <schema>public</schema> \n" +
			"    <catalog></catalog> \n" +
			"    <database-product-name>PostgreSQL</database-product-name> \n" +
			"    <database-product-version>9.1.3</database-product-version> \n" +
			"    <wb-tag-format>short</wb-tag-format> \n" +
			"  </meta-data> \n" +
			" \n" +
			"  <table-def> \n" +
			"    <table-name>b</table-name> \n" +
			"    <column-count>1</column-count> \n" +
			" \n" +
			"    <column-def index=\"0\"> \n" +
			"      <column-name>id_b</column-name> \n" +
			"      <java-class>java.lang.Integer</java-class> \n" +
			"      <java-sql-type-name>INTEGER</java-sql-type-name> \n" +
			"      <java-sql-type>4</java-sql-type> \n" +
			"      <dbms-data-type>integer</dbms-data-type> \n" +
			"      <primary-key>false</primary-key> \n" +
			"    </column-def> \n" +
			"  </table-def> \n" +
			" \n" +
			"<data> \n" +
			"<rd><cd>10</cd></rd> \n" +
			"<rd><cd>20</cd></rd> \n" +
			"<rd><cd>30</cd></rd> \n" +
			"<rd><cd>40</cd></rd> \n" +
			"<rd><cd>50</cd></rd> \n" +
			"<rd><cd>60</cd></rd> \n" +
			"<rd><cd>70</cd></rd> \n" +
			"<rd><cd>80</cd></rd> \n" +
			"<rd><cd>90</cd></rd> \n" +
			"<rd><cd>100</cd></rd> \n" +
			"<rd><cd>110</cd></rd> \n" +
			"<rd><cd>120</cd></rd> \n" +
			"<rd><cd>130</cd></rd> \n" +
			"<rd><cd>140</cd></rd> \n" +
			"<rd><cd>150</cd></rd> \n" +
			"</data> \n" +
			"</wb-export>";

		try
		{
			WbFile ta = new WbFile(util.getBaseDir(), "a.xml");
			TestUtil.writeFile(ta, xml1, "UTF-8");

			WbFile tb = new WbFile(util.getBaseDir(), "b.xml");
			TestUtil.writeFile(tb, xml2, "UTF-8");

			TestUtil.executeScript(connection,
				"create table a (id_a integer); \n" +
				"create table b (id_b integer); \n" +
				"commit;\n");

			String cmd = "wbimport -encoding='UTF-8' -sourceDir='" + ta.getParent() + "' -type=xml -batchSize=10";

			StatementRunnerResult result = importCmd.execute(cmd);
			String msg = result.getMessageBuffer().toString();
			assertEquals("Import failed: " + msg, result.isSuccess(), true);
			String[] lines = msg.split(StringUtil.REGEX_CRLF);
			assertTrue(lines.length == 6);

			assertTrue(lines[0].endsWith("into table A"));
			assertEquals("15 row(s) inserted", lines[1]);
			assertEquals("0 row(s) updated", lines[2]);

			assertTrue(lines[3].endsWith("into table B"));
			assertEquals("15 row(s) inserted", lines[4]);
			assertEquals("0 row(s) updated", lines[5]);

      int rowCount = TestUtil.getNumberValue(connection, "select count(*) from a");
			assertEquals("Wrong number of rows for table a", 15, rowCount);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testXmlImport()
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
		File xmlFile = new File(this.basedir, "xml_import.xml");
		TestUtil.writeFile(xmlFile, xml, "UTF-8");

		String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=junit_test";
		//System.out.println("cmd=" + cmd);
		StatementRunnerResult result = importCmd.execute(cmd);
    String msg = result.getMessageBuffer().toString();
    System.out.println(msg);
		assertEquals(true, result.isSuccess());

		try (Statement stmt = this.connection.createStatementForQuery();
				ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from junit_test"))
		{
			int rowCount = 0;
			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", rowCount, nr);
			}
			assertEquals("Wrong number of rows", rowCount, 2);
			rs.close();
		}
		if (!xmlFile.delete())
		{
			fail("Could not delete input file: " + xmlFile.getCanonicalPath());
		}
	}

	@Test
	public void testXmlImportChangeStructure()
		throws Exception
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
		File xmlFile = new File(this.basedir, "xml_import.xml");
		TestUtil.writeFile(xmlFile, xml, "UTF-8");

		TestUtil.executeScript(connection, sql);

		String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=info";
		StatementRunnerResult result = importCmd.execute(cmd);
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		int rowCount = TestUtil.getNumberValue(connection, "select count(*) from info");
		assertEquals("Wrong number of rows", rowCount, 4);

		if (!xmlFile.delete())
		{
			fail("Could not delete input file: " + xmlFile.getCanonicalPath());
		}
	}

	@Test
	public void testXmlImportCreateTable()
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
		File xmlFile = new File(this.basedir, "xml_import.xml");
		TestUtil.writeFile(xmlFile, xml, "UTF-8");

		String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -createTarget=true";
		//System.out.println("cmd=" + cmd);
		StatementRunnerResult result = importCmd.execute(cmd);
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		try (Statement stmt = this.connection.createStatementForQuery();
				ResultSet rs = stmt.executeQuery("select nr, firstname, lastname from not_there_table"))
		{
			int rowCount = 0;
			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);
				assertEquals("Wrong data imported", rowCount, nr);
			}
			assertEquals("Wrong number of rows", rowCount, 2);
			rs.close();
		}
		if (!xmlFile.delete())
		{
			fail("Could not delete input file: " + xmlFile.getCanonicalPath());
		}
	}

	@Test
	public void testPartialXmlImport()
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
             "      <java-sql-type-name>VARCHAR</java-sql-type-name> \n" +
             "      <java-sql-type>12</java-sql-type> \n" +
             "      <dbms-data-type>VARCHAR(100)</dbms-data-type> \n" +
             "    </column-def> \n" +
             "  </table-def> \n" +
             " \n" +
             "<data> \n";
		String xmlEnd = "</data> \n" +
             "</wb-export>";

		File xmlFile = new File(this.basedir, "xml_import2.xml");
		try (BufferedWriter out = new BufferedWriter(EncodingUtil.createWriter(xmlFile, "UTF-8", false)))
		{
			out.write(xml);
			for (int i=0; i < 100; i++)
			{
				int id = i + 1;
				out.write("<rd><cd>" + id + "</cd><cd>Lastname" + id + "</cd><cd>Firstname" + id + "</cd></rd>\n");
			}
			out.write(xmlEnd);
		}

		String cmd = "wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -startRow = 15 -endrow = 24 -table=junit_test";
		//System.out.println("cmd=" + cmd);
		StatementRunnerResult result = importCmd.execute(cmd);
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		try (Statement stmt = this.connection.createStatementForQuery();
				ResultSet rs = stmt.executeQuery("select min(nr), max(nr), count(*) from junit_test"))
		{
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
		}
		if (!xmlFile.delete())
		{
			fail("Could not delete input file: " + xmlFile.getCanonicalPath());
		}
	}

	@Test
	public void testEncodedBlobImport()
		throws Exception
	{
		File xmlFile = util.copyResourceFile(this, "encoded_blob_input.xml");

		StatementRunnerResult result = importCmd.execute("wbimport -encoding='ISO-8859-1' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=blob_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		int rowCount;
		try (Statement stmt = this.connection.createStatementForQuery();
				ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test"))
		{
			String xmlContent = FileUtil.readFile(xmlFile, "ISO-8859-1");
			int id1 = Integer.parseInt(TestUtil.getXPathValue(xmlContent, "/wb-export/data/row-data[1]/column-data[1]"));
			String blob1 = TestUtil.getXPathValue(xmlContent, "/wb-export/data/row-data[1]/column-data[2]");
			int id2 = Integer.parseInt(TestUtil.getXPathValue(xmlContent, "/wb-export/data/row-data[2]/column-data[1]"));
			String blob2 = TestUtil.getXPathValue(xmlContent, "/wb-export/data/row-data[2]/column-data[2]");

			rowCount = 0;
			while (rs.next())
			{
				rowCount ++;
				int nr = rs.getInt(1);

				Object blob = rs.getObject(2);
				assertNotNull("No blob data imported", blob);

				String blobString = DatatypeConverter.printBase64Binary((byte[])blob);
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
		}
		assertEquals(2, rowCount);
		if (!xmlFile.delete())
		{
			fail("Could not delete input file: " + xmlFile.getCanonicalPath());
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
      TestUtil.writeFile(xmlFile, xml, "UTF-8");

			File dataFile = new File(this.basedir, "test_r1_c2.data");
      try (FileOutputStream binaryOut = new FileOutputStream(dataFile))
      {
        byte[] testData = new byte[1024];
        for (int i = 0; i < testData.length; i++)
        {
          testData[i] = (byte)(i % 255);
        }
        binaryOut.write(testData);
      }

			StatementRunnerResult result = importCmd.execute("wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=blob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

      try (Statement stmt = this.connection.createStatementForQuery();
           ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test"))
      {

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
          fail("Not enough data imported");
        }
      }
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
      TestUtil.writeFile(xmlFile, xml, "UTF-8");

			File datafile = new File(this.basedir, "test_r1_c2.data");

      TestUtil.writeFile(datafile, "This is a CLOB string to be put into row 1", "UTF-8");

			StatementRunnerResult result = importCmd.execute("wbimport -encoding='UTF-8' -file='" + xmlFile.getAbsolutePath() + "' -type=xml -table=clob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

      try (Statement stmt = this.connection.createStatementForQuery();
           ResultSet rs = stmt.executeQuery("select nr, text_data from clob_test"))
      {

        if (rs.next())
        {
          int nr = rs.getInt(1);
          assertEquals("Wrong data imported", 1, nr);

          String data = rs.getString(2);
          assertEquals(data, data);
        }
        else
        {
          fail("Not enough data imported");
        }
      }
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
    throws Exception
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

    File xmlFile = new File(this.basedir, "xml_import.xml");

    File archive = new File(this.basedir, "blob_test.zip");
    ZipOutputFactory zout = new ZipOutputFactory(archive);
    try (Writer w = zout.createWriter(xmlFile, "UTF-8"))
    {
      w.write(xml);
    }
    zout.done();

    File blobarchive = new File(this.basedir, "blob_test" + RowDataConverter.BLOB_ARCHIVE_SUFFIX + ".zip");
    zout = new ZipOutputFactory(blobarchive);
    try (OutputStream binaryOut = zout.createOutputStream(new File("test_r1_c2.data")))
    {
      byte[] testData = new byte[1024];
      for (int i = 0; i < testData.length; i++)
      {
        testData[i] = (byte)(i % 255);
      }
      binaryOut.write(testData);
    }
    zout.done();

    StatementRunnerResult result = importCmd.execute("wbimport -encoding='UTF-8' -file='" + archive.getAbsolutePath() + "' -type=xml -table=blob_test");
    assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

    try (Statement stmt = this.connection.createStatementForQuery();
         ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test"))
    {
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
        fail("Not enough data imported");
      }
    }
	}

	@Test
	public void testTextBlobImport()
	{
		try
		{
			File importFile  = new File(this.basedir, "blob_test.txt");
      TestUtil.writeFile(importFile,
        "nr\tbinary_data\n" +
        "1\tblob_data_r1_c1.data\n");

      try (FileOutputStream binaryOut = new FileOutputStream(new File(this.basedir, "blob_data_r1_c1.data")))
      {
        byte[] testData = new byte[1024];
        for (int i = 0; i < testData.length; i++)
        {
          testData[i] = (byte)(i % 255);
        }
        binaryOut.write(testData);
      }

			StatementRunnerResult result = importCmd.execute("wbimport -file='" + importFile.getAbsolutePath() + "' -decimal='.' -type=text -header=true -table=blob_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

      try (Statement stmt = this.connection.createStatementForQuery();
           ResultSet rs = stmt.executeQuery("select nr, binary_data from blob_test"))
      {
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
      }
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testEncodedBlob()
		throws Exception
	{
		File importFile  = new File(this.basedir, "blob2_test.txt");

		try (PrintWriter out = new PrintWriter(new FileWriter(importFile)))
		{
			byte[] testData = new byte[1024];
			for (int i = 0; i < testData.length; i++)
			{
				testData[i] = (byte)(i % 255);
			}

			out.println("nr\tbinary_data");
			out.print("1\t");
			out.println(DatatypeConverter.printBase64Binary(testData));
		}

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
	}

	@Test
	public void testBadFile()
		throws Exception
	{
		File importFile  = new File(this.basedir, "bad_import.txt");
		File badFile = new File(this.basedir, "import.bad");
		TestUtil.writeFile(importFile,
			"nr\tfirstname\tlastname\n" +
			"1\tMary\tMoviestar\n" +
			"2\tHarry\tHandsome\n" +
			"1\tZaphod\tBeeblebrox\n");

		StatementRunnerResult result = importCmd.execute(
			"wbimport -encoding=utf8 " +
				"-file='" + importFile.getAbsolutePath() + "' " +
				"-multiline=false -type=text -header=true " +
				"-continueonerror=true " +
				"-table=junit_test_pk -badFile='" + badFile.getCanonicalPath() + "'");

		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		assertEquals("Bad file not created", true, badFile.exists());

		String line;
		try (BufferedReader r = new BufferedReader(new FileReader(badFile)))
		{
			line = r.readLine();
		}
		assertEquals("Wrong record rejected", "1\tZaphod\tBeeblebrox", line);

		try (
			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr from junit_test_pk order by nr");
			)
		{

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
		}

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
		}
		if (!badFile.delete())
		{
			fail("Could not delete bad file: " + badFile.getCanonicalPath());
		}
	}

	@Test
	public void testPartialFixedWidthImport()
		throws Exception
	{
		File importFile = new File(this.basedir, "fixed_import.txt");
		TestUtil.writeFile(importFile,
		"  1      MaryMoviestar      \n" +
		"  2     HarryHandsome       \n"+
		"  3Zaphod    Beeblebrox     \n", "UTF-8");


		StatementRunnerResult result = importCmd.execute(
			"wbimport -encoding=utf8 -trimValues=true -file='" + importFile.getAbsolutePath() + "' " +
			"-multiline=false -type=text -header=false " +
			"-filecolumns=nr,firstname,lastname -importcolumns=nr,lastname -columnWidths='nr=3,firstname=10,lastname=15' " +
			"-continueonerror=true -table=junit_test");
		assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

		try (Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr,firstname,lastname from junit_test_pk order by nr");
			)
		{

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
		}

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
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

			StatementRunnerResult result = importCmd.execute(
				"wbimport -encoding=utf8 -trimValues=true " +
				"-file='" + importFile.getAbsolutePath() + "' " +
				"-multiline=false -type=text -header=false " +
				"-filecolumns=nr,firstname,lastname -columnWidths='nr=3,firstname=10,lastname=15' " +
				"-continueonerror=true -table=junit_test");

			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			Statement stmt = this.connection.createStatementForQuery();
			ResultSet rs = stmt.executeQuery("select nr,firstname,lastname from junit_test order by nr");
			while (rs.next())
			{
				int id = rs.getInt(1);
				String firstname = rs.getString(2);
				String lastname = rs.getString(3);
				if (id == 1)
				{
					assertEquals("Wrong Firstname imported", "Mary", firstname);
					assertEquals("Wrong Lastname imported", "Moviestar", lastname);
				}
				else if (id == 2)
				{
					assertEquals("Wrong Firstname imported", "Harry", firstname);
					assertEquals("Wrong Lastname imported", "Handsome", lastname);
				}
				else if (id == 3)
				{
					assertEquals("Wrong Firstname imported", "Zaphod", firstname);
					assertEquals("Wrong Lastname imported", "Beeblebrox", lastname);
				}
				else
				{
					fail("Wrong id "+ id + " retrieved");
				}
			}

			rs.close();
			stmt.executeUpdate("delete from junit_test");
			connection.commit();
			stmt.close();

			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("10Mary  Moviestar ");
			out.println("20Harry Handsome  ");
			out.println("30ZaphodBeeblebrox");
			out.close();

			result = importCmd.execute(
				"wbimport -encoding=utf8 -trimValues=true " +
				"-file='" + importFile.getAbsolutePath() + "' " +
				"-multiline=false -type=text -header=false " +
				"-filecolumns=nr,firstname,lastname -importColumns=nr,lastname -columnWidths='nr=2,firstname=6,lastname=10' " +
				"-continueonerror=true -table=junit_test");
			assertEquals("Import failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			stmt = this.connection.createStatementForQuery();
			rs = stmt.executeQuery("select nr, firstname, lastname from junit_test where nr > 5 order by nr");
			while (rs.next())
			{
				int id = rs.getInt(1);
				String firstname = rs.getString(2);
				String lastname = rs.getString(3);
				assertNull(firstname);
				if (id == 10)
				{
					assertEquals("Wrong Lastname imported", "Moviestar", lastname);
				}
				else if (id == 20)
				{
					assertEquals("Wrong Lastname imported", "Handsome", lastname);
				}
				else if (id == 30)
				{
					assertEquals("Wrong Lastname imported", "Beeblebrox", lastname);
				}
				else
				{
					fail("Wrong id "+ id + " retrieved");
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
  public void testColumnFilter()
    throws Exception
  {
    File importFile = new File(basedir, "person.txt");

    TestUtil.writeFile(importFile,
      "nr|firstname|lastname\n" +
      "1|Arthur|Dent\n" +
      "2|Ford|Prefect;\n" +
      "3|Zaphod|Beeblebrox\n" +
      "4|Tricia|McMillan", "UTF-8");

		StatementRunnerResult result = importCmd.execute(
      "wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -type=text " +
      "         -header=true " +
      "         -delimiter='|' " +
      "         -columnFilter='nr=[^3]'" +
      "         -continueonerror=false -table=junit_test " +
      "         -deleteTarget=true");

//		String msg = result.getMessageBuffer().toString();
//		System.out.println(" ***** message=" + msg);
		assertTrue(result.isSuccess());
    int rows = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
    assertEquals(3, rows);

		File ods = util.copyResourceFile(this, "col_filter_test.ods");

		result = importCmd.execute(
      "wbimport -file='" + ods.getAbsolutePath() + "' " +
      "         -header=true " +
      "         -deleteTarget=true " +
      "         -continueonerror=false " +
      "         -columnFilter='nr=^[^3]*'" +
      "         -table=junit_test ");

//		String msg = result.getMessageBuffer().toString();
//		System.out.println(" ***** message=" + msg);

		assertTrue(result.isSuccess());
    rows = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
    assertEquals(3, rows);

    rows = TestUtil.getNumberValue(connection, "select count(*) from junit_test where nr = 3");
    assertEquals(0, rows);

		File xls = util.copyResourceFile(this, "col_filter_test.xlsx");

		result = importCmd.execute(
      "wbimport -file='" + xls.getAbsolutePath() + "' " +
      "         -header=true " +
      "         -deleteTarget=true " +
      "         -continueonerror=false " +
      "         -columnFilter='nr=^[^1]*'" +
      "         -table=junit_test ");

//		String msg = result.getMessageBuffer().toString();
//		System.out.println(" ***** message=" + msg);

		assertTrue(result.isSuccess());
    rows = TestUtil.getNumberValue(connection, "select count(*) from junit_test");
    assertEquals(3, rows);

    rows = TestUtil.getNumberValue(connection, "select count(*) from junit_test where nr = 1");
    assertEquals(0, rows);
  }

	@Test
	public void testDeleteTargetFails()
		throws Exception
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
		TestUtil.writeFile(importFile,
		"id\tsome_val\n" +
		"1\t42", "UTF-8");

		StatementRunnerResult result = importCmd.execute("wbimport -encoding=utf8 -file='" + importFile.getAbsolutePath() + "' -type=text -header=true -continueonerror=false -table=parent_table -deleteTarget=true");
		assertEquals("Import did not fail", false, result.isSuccess());
		String msg = result.getMessageBuffer().toString();
//			System.out.println(" ***** message=" + msg);
		assertEquals("No error reported", true, msg.toLowerCase().indexOf("integrity constraint violation") > 0);

		int count = TestUtil.getNumberValue(connection, "select count(*) from parent_table");
		assertEquals("Wrong number of rows", 3, count);

		if (!importFile.delete())
		{
			fail("Could not delete input file: " + importFile.getCanonicalPath());
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
		throws Exception
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

		TestUtil.writeFile(f1, f1_content, "UTF-8");
		TestUtil.writeFile(f2, f2_content, "UTF-8");
		TestUtil.writeFile(f3, f3_content, "UTF-8");

		WbFile f = new WbFile(basedir);
		StatementRunnerResult result = importCmd.execute("wbimport -sourcedir='" + f.getFullPath() + "' -type=xml -checkDependencies=true");
		String msg = result.getMessageBuffer().toString();
		if (!result.isSuccess())
		{
			System.out.println(msg);
		}
		assertEquals("Import failed", result.isSuccess(), true);

		int count = TestUtil.getNumberValue(connection, "select count(*) from zzbase");
		assertEquals("Wrong row count for zzbase", 2, count);

		count = TestUtil.getNumberValue(connection, "select count(*) from child1");
		assertEquals("Wrong row count for child1", 4, count);

		count = TestUtil.getNumberValue(connection, "select count(*) from a_child1_child");
		assertEquals("Wrong row count for a_child1_child", 4, count);

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
			String msg = result.getMessageBuffer().toString();
			assertEquals("Import failed: " + msg, result.isSuccess(), true);

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
      TestUtil.writeFile(importFile,
        "nr\tfirstname\tlastname\tnot_there\n" +
        "1\tArthur\tDent\t1\n" +
        "2\tFord\tPrefect\t1\n" +
        "3\tZaphod\tBeeblebrox\t1\n", "UTF-8");

			importFile  = new File(this.basedir, "zzbase.txt");
      TestUtil.writeFile(importFile,
        "id\tinfo\n" +
        "1\tArthur\n" +
        "2\tFord\n" +
        "3\tZaphod\n", "UTF-8");

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

		try (Statement stmt = wb.createStatement())
		{
			stmt.executeUpdate("CREATE TABLE junit_test (nr integer, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("CREATE TABLE junit_test_pk (nr integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("CREATE TABLE numeric_test (nr integer primary key, amount double, prod_name varchar(50))");
			stmt.executeUpdate("CREATE TABLE datatype_test (int_col integer, double_col double, char_col varchar(50), date_col date, time_col time, ts_col timestamp, nchar_col nvarchar(10))");
			stmt.executeUpdate("CREATE TABLE blob_test (nr integer, binary_data BINARY)");
			stmt.executeUpdate("CREATE TABLE clob_test (nr integer, text_data CLOB)");
			stmt.executeUpdate("CREATE TABLE bool_int_test (nr integer, int_flag INTEGER)");
			stmt.executeUpdate("CREATE TABLE bool_test (nr integer, flag BOOLEAN)");
			stmt.executeUpdate("CREATE TABLE const_test (id integer, flag1 varchar(2), flag2 varchar(2))");
			stmt.executeUpdate("create table id_test (id integer generated always as identity primary key, firstname varchar(100), lastname varchar(100))");

			stmt.executeUpdate("CREATE TABLE zzbase (id integer primary key, info varchar(50))");
			stmt.executeUpdate("CREATE TABLE child1 (id integer primary key, base_id integer not null, info varchar(50))");
			stmt.executeUpdate("CREATE TABLE a_child1_child (id integer primary key, child_id integer not null, info varchar(50))");
			stmt.executeUpdate("alter table child1 add foreign key (base_id) references zzbase(id)");
			stmt.executeUpdate("alter table a_child1_child add foreign key (child_id) references child1(id)");

			stmt.executeUpdate("CREATE TABLE person (id integer primary key, firstname varchar(50), lastname varchar(50), hiredate date, salary numeric(10,2), last_login timestamp)");
			stmt.executeUpdate("CREATE TABLE orders (customer_id integer not null, order_id integer not null, product_id integer not null, amount integer not null)");

			wb.commit();
		}

		return wb;
	}

}
