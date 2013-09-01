/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.mssql;

import java.io.BufferedReader;
import java.io.File;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.BatchRunner;

import workbench.util.EncodingUtil;
import workbench.util.FileUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbExportSqlServerTest
	extends WbTestCase
{

	public WbExportSqlServerTest()
	{
		super("WbExportSqlServerTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("export_test");
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"create table data (some_data binary(4), data_id integer not null);\n" +
			"insert into data (data_id, some_data) values (1, 0x01020304);\n" +
			"insert into data (data_id, some_data) values (2, 0x02030405);\n" +
			"commit;\n"
			);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@Test
	public void testBlobExport()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;

		TestUtil util = getTestUtil();

		String dir = util.getBaseDir();
		File output = new File(dir, "data.txt");
		String sql =
			"WbExport -type=text\n" +
			"         -file='" + output.getAbsolutePath() + "'\n" +
			"         -delimiter=';'\n" +
			"         -encoding='ISO-8859-1'\n" +
			"         -header=true\n" +
			"         -blobType=ansi\n" +
			"         -dateFormat='yyyy-MM-dd';" +
			" select some_data, data_id from data order by data_id";


		BatchRunner runner = new BatchRunner();
		runner.setConnection(conn);
		runner.executeScript(sql);

		String msg = runner.getMessages();
		assertTrue(msg, runner.isSuccess());
		assertTrue(output.exists());
		BufferedReader in = null;
		try
		{
			in = EncodingUtil.createBufferedReader(output, "ISO-8859-1");
			List<String> lines = FileUtil.getLines(in);
			assertNotNull(lines);
			assertEquals(3, lines.size());
			assertEquals("0x01020304;1", lines.get(1));
			assertEquals("0x02030405;2", lines.get(2));
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
		assertTrue(output.delete());
	}
}
