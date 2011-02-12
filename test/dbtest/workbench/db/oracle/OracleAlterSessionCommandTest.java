/*
 * OracleSetCommandTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.StatementRunner;
import static org.junit.Assert.*;
/**
 *
 * @author Thomas Kellerer
 */
public class OracleAlterSessionCommandTest
	extends WbTestCase
{

	public OracleAlterSessionCommandTest()
	{
		super("OracleAlterSessionTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
	}

	@Test
	public void testExecute()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		//String oldZone = TestUtil.getSingleQueryValue(con, "select SESSIONTIMEZONE from dual").toString();
		StatementRunner runner = new StatementRunner();
		runner.setConnection(con);

		runner.runStatement("alter session set time_zone = 'Canada/Saskatchewan';");
		String newZone = TestUtil.getSingleQueryValue(con, "select SESSIONTIMEZONE from dual").toString();
		assertEquals("Canada/Saskatchewan", newZone);
	}

}