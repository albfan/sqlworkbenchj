/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.mssql;

import java.io.BufferedReader;
import java.io.File;
import java.util.List;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbExport;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;

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
			"create table data (data_id integer not null primary key, some_data varbinary(64));\n" +
			"insert int data (data_id, some_data) values (1, 0x01020304);\n" +
			"insert int data (data_id, some_data) values (2, 0x02030405);\n" +
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
			"         -dateFormat='yyyy-MM-dd';";
		WbExport command = new WbExport();

		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;

		command.setConnection(conn);
		StatementRunnerResult result = command.execute(sql);
		String msg = result.getMessageBuffer().toString();
		System.out.println(msg);
		assertTrue(result.isSuccess());
		assertTrue(output.exists());
		BufferedReader in = null;
		try
		{
			in = EncodingUtil.createBufferedReader(output, "ISO-8859-1");
			List<String> lines = FileUtil.getLines(in);
			assertNotNull(lines);
			assertEquals(3, lines.size());
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
	}
}
