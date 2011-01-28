/*
 * MySqlProcedureReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mysql;

import workbench.resource.Settings;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptParser;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MySqlProcedureReaderTest
	extends WbTestCase
{

	public MySqlProcedureReaderTest()
	{
		super("MySqlProcedureReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		MySQLTestUtil.initTestcase("MySqlProcedureReaderTest");
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;
		String sql =
			"CREATE PROCEDURE simpleproc (OUT param1 INT) \n"+
			"BEGIN \n" +
			"   SELECT COUNT(*) INTO param1 FROM t;\n" +
			"END\n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;
		String sql =
			"DROP PROCEDURE simpleproc;";
		TestUtil.executeScript(con, sql);
	}

	@Test
	public void testGetProcedureHeader()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null)
		{
			System.out.println("No connection!");
			return;
		}

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(MySQLTestUtil.DB_NAME, null, null);
		assertNotNull(procs);
		assertEquals(1, procs.size());
		ProcedureDefinition proc = procs.get(0);
		assertEquals("simpleproc", proc.getProcedureName());
		String source = proc.getSource(con).toString();
		ScriptParser p = new ScriptParser(source);
		p.setAlternateDelimiter(Settings.getInstance().getAlternateDelimiter(con));
		assertEquals(2, p.getSize());
		String create = p.getCommand(1);
		String expected =
			"CREATE PROCEDURE simpleproc (OUT param1 INT)\n"+
			"BEGIN \n" +
			"   SELECT COUNT(*) INTO param1 FROM t;\n" +
			"END";
		assertEquals(expected, create.trim());
	}
}
