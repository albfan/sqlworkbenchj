/*
 * OracleErrorInformationReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import workbench.db.WbConnection;

import workbench.sql.DelimiterDefinition;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import workbench.TestUtil;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleErrorInformationReaderTest
	extends WbTestCase
{
	public OracleErrorInformationReaderTest()
	{
		super("OracleErrorInformationReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetErrorInfo()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql = "create procedure nocando as begin null; ende;\n/\n";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		try
		{
			con.setBusy(true);
			OracleErrorInformationReader reader = new OracleErrorInformationReader(con);
			String errorInfo = reader.getErrorInfo(null, "nocando", "procedure", true);
			con.setBusy(false);
			assertTrue(errorInfo.startsWith("Errors for PROCEDURE NOCANDO"));
			assertTrue(errorInfo.contains("PLS-00103"));
		}
		finally
		{
			con.setBusy(false);
		}
	}

}
