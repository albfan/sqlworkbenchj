/*
 * Db2ProcedureReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.util.List;
import workbench.db.ProcedureDefinition;
import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.sql.DelimiterDefinition;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2ProcedureReaderTest
	extends WbTestCase
{
	public Db2ProcedureReaderTest()
	{
		super("Db2ProcedureReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String sql = "create or replace procedure wb_test () \n" +
             "language SQL \n" +
             "begin \n" +
             "end \n" +
             "/\n" +
						 "commit\n" +
						 "/\n";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String sql =
			"drop procedure " + Db2TestUtil.getSchemaName() + ".wb_test; \n" +
      "commit;\n";
		TestUtil.executeScript(con, sql);
		Db2TestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetProcedures()
		throws SQLException
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) 
		{
			System.out.println("DB2 Not available, skipping test");
			return;
		}

		ProcedureReader reader = con.getMetadata().getProcedureReader();
		assertTrue(reader instanceof Db2ProcedureReader);

		List<ProcedureDefinition> procs = reader.getProcedureList(null, Db2TestUtil.getSchemaName(), null);

		assertNotNull(procs);
		assertEquals(1, procs.size());
	}
}
